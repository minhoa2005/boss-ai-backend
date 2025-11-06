package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.dtos.PaginatedResponse;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.ContentVersion;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.exception.InternalServerException;
import ai.content.auto.mapper.ContentVersionMapper;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.repository.ContentVersionRepository;
import ai.content.auto.repository.UserRepository;
import ai.content.auto.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing content versions.
 * Handles version creation, storage optimization, and cleanup policies.
 * 
 * Requirements: 1.1, 1.2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentVersioningService {

    private final ContentVersionRepository contentVersionRepository;
    private final ContentGenerationRepository contentGenerationRepository;
    private final ContentVersionMapper contentVersionMapper;
    private final SecurityUtil securityUtil;
    private final AuditService auditService;
    private final UserRepository userRepository;

    // Configuration constants
    private static final int MAX_VERSIONS_PER_CONTENT = 50;
    private static final int ARCHIVE_THRESHOLD_DAYS = 90;
    private static final int LARGE_CONTENT_THRESHOLD = 10000; // characters

    /**
     * Create a new version from content generation response.
     * 
     * @param contentId Content ID to create version for
     * @param response  Content generation response
     * @return Created ContentVersionDto
     */
    public ContentVersionDto createVersion(Long contentId, ContentGenerateResponse response) {
        try {
            // 1. Validate input
            validateCreateVersionInput(contentId, response);

            // 2. Get current user from security context
            User currentUser = securityUtil.getCurrentUser();
            log.info("Creating version for content: {} by user: {}", contentId, currentUser.getId());

            // 3. Validate content ownership
            validateContentOwnership(contentId, currentUser.getId());

            // 4. Create version in transaction
            ContentVersion version = createVersionInTransaction(contentId, response, currentUser);

            // 5. Update main content record
            updateContentToLatestVersionInTransaction(contentId, version);

            // 6. Check and apply cleanup policies
            applyCleanupPolicies(contentId);

            log.info("Version {} created successfully for content: {}", version.getVersionNumber(), contentId);
            return contentVersionMapper.toDto(version);

        } catch (BusinessException e) {
            log.error("Business error creating version for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating version for content: {}", contentId, e);
            throw new InternalServerException("Failed to create content version");
        }
    }

    /**
     * Create a new version from content generation response with explicit user ID.
     * This method is used for background jobs where security context is not
     * available.
     * 
     * @param contentId Content ID to create version for
     * @param response  Content generation response
     * @param userId    User ID who owns the content
     * @return Created ContentVersionDto
     */
    public ContentVersionDto createVersionForUser(Long contentId, ContentGenerateResponse response, Long userId) {
        try {
            // 1. Validate input
            validateCreateVersionInput(contentId, response);
            if (userId == null) {
                throw new BusinessException("User ID is required");
            }

            log.info("Creating version for content: {} by user: {} (background job)", contentId, userId);

            // 2. Get user entity
            User user = getUserById(userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Create version in transaction
            ContentVersion version = createVersionInTransaction(contentId, response, user);

            // 5. Update main content record
            updateContentToLatestVersionInTransaction(contentId, version);

            // 6. Check and apply cleanup policies
            applyCleanupPolicies(contentId);

            log.info("Version {} created successfully for content: {} (background job)", version.getVersionNumber(),
                    contentId);
            return contentVersionMapper.toDto(version);

        } catch (BusinessException e) {
            log.error("Business error creating version for content: {} by user: {}", contentId, userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating version for content: {} by user: {}", contentId, userId, e);
            throw new InternalServerException("Failed to create content version");
        }
    }

    /**
     * Get version history for a content with pagination.
     * 
     * @param contentId Content ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @return PaginatedResponse of ContentVersionDto
     */
    @Transactional(readOnly = true)
    public ai.content.auto.dtos.PaginatedResponse<ContentVersionDto> getVersionHistory(Long contentId, int page,
            int size) {
        try {
            // 1. Validate input
            if (contentId == null) {
                throw new BusinessException("Content ID is required");
            }
            if (page < 0) {
                throw new BusinessException("Page number must be non-negative");
            }
            if (size <= 0 || size > 100) {
                throw new BusinessException("Page size must be between 1 and 100");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting version history for content: {} by user: {}, page: {}, size: {}",
                    contentId, userId, page, size);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get versions with pagination
            Pageable pageable = PageRequest.of(page, size);
            Page<ContentVersion> versionsPage = contentVersionRepository
                    .findByContentIdOrderByVersionNumberDesc(contentId, pageable);

            // 5. Map to DTOs with metadata
            List<ContentVersionDto> versionDtos = versionsPage.getContent().stream()
                    .map(version -> {
                        // Check if this is the latest version (first in the list since ordered by
                        // version desc)
                        boolean isLatest = !versionsPage.getContent().isEmpty() &&
                                version.getVersionNumber().equals(versionsPage.getContent().get(0).getVersionNumber());
                        return contentVersionMapper.toDtoWithMetadata(version, isLatest,
                                (int) versionsPage.getTotalElements());
                    })
                    .toList();

            // 6. Create paginated response
            ai.content.auto.dtos.PaginatedResponse.PaginationMetadata metadata = ai.content.auto.dtos.PaginatedResponse.PaginationMetadata
                    .builder()
                    .page(versionsPage.getNumber())
                    .size(versionsPage.getSize())
                    .totalElements(versionsPage.getTotalElements())
                    .totalPages(versionsPage.getTotalPages())
                    .first(versionsPage.isFirst())
                    .last(versionsPage.isLast())
                    .hasNext(versionsPage.hasNext())
                    .hasPrevious(versionsPage.hasPrevious())
                    .numberOfElements(versionsPage.getNumberOfElements())
                    .build();

            return ai.content.auto.dtos.PaginatedResponse.<ContentVersionDto>builder()
                    .content(versionDtos)
                    .pagination(metadata)
                    .build();

        } catch (BusinessException e) {
            log.error("Business error getting version history for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting version history for content: {}", contentId, e);
            throw new InternalServerException("Failed to get version history");
        }
    }

    /**
     * Get a specific version by content ID and version number.
     * 
     * @param contentId     Content ID
     * @param versionNumber Version number
     * @return ContentVersionDto
     */
    @Transactional(readOnly = true)
    public ContentVersionDto getVersion(Long contentId, Integer versionNumber) {
        try {
            // 1. Validate input
            if (contentId == null || versionNumber == null) {
                throw new BusinessException("Content ID and version number are required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting version {} for content: {} by user: {}", versionNumber, contentId, userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get version
            ContentVersion version = contentVersionRepository
                    .findByContentIdAndVersionNumber(contentId, versionNumber)
                    .orElseThrow(() -> new NotFoundException("Version not found"));

            // 5. Get metadata
            long totalVersions = contentVersionRepository.countByContentId(contentId);
            Optional<ContentVersion> latestVersion = contentVersionRepository.findLatestVersionByContentId(contentId);
            boolean isLatest = latestVersion.isPresent() &&
                    latestVersion.get().getVersionNumber().equals(versionNumber);

            return contentVersionMapper.toDtoWithMetadata(version, isLatest, (int) totalVersions);

        } catch (BusinessException e) {
            log.error("Business error getting version {} for content: {}", versionNumber, contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting version {} for content: {}", versionNumber, contentId, e);
            throw new InternalServerException("Failed to get content version");
        }
    }

    /**
     * Revert content to a specific version.
     * 
     * @param contentId     Content ID
     * @param versionNumber Version number to revert to
     * @return New version created from the reverted content
     */
    public ContentVersionDto revertToVersion(Long contentId, Integer versionNumber) {
        try {
            // 1. Validate input
            if (contentId == null || versionNumber == null) {
                throw new BusinessException("Content ID and version number are required");
            }

            // 2. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Reverting content: {} to version: {} by user: {}",
                    contentId, versionNumber, currentUser.getId());

            // 3. Validate content ownership
            validateContentOwnership(contentId, currentUser.getId());

            // 4. Get current version for audit logging
            Optional<ContentVersion> currentVersionOpt = contentVersionRepository
                    .findLatestVersionByContentId(contentId);
            Integer currentVersionNumber = currentVersionOpt.map(ContentVersion::getVersionNumber).orElse(null);

            // 5. Get target version
            ContentVersion targetVersion = contentVersionRepository
                    .findByContentIdAndVersionNumber(contentId, versionNumber)
                    .orElseThrow(() -> new NotFoundException("Target version not found"));

            // 6. Create new version based on target version
            ContentVersion revertedVersion = createRevertedVersionInTransaction(
                    contentId, targetVersion, currentUser);

            // 7. Update main content record
            updateContentToLatestVersionInTransaction(contentId, revertedVersion);

            // 8. Create audit log for the revert operation
            auditService.logContentVersionRevert(
                    contentId,
                    currentVersionNumber,
                    versionNumber,
                    revertedVersion.getVersionNumber(),
                    currentUser);

            log.info("Content: {} reverted to version: {} as new version: {} with audit logging",
                    contentId, versionNumber, revertedVersion.getVersionNumber());

            return contentVersionMapper.toDto(revertedVersion);

        } catch (BusinessException e) {
            log.error("Business error reverting content: {} to version: {}", contentId, versionNumber, e);
            // Log failed action for audit trail
            try {
                User currentUser = securityUtil.getCurrentUser();
                auditService.logFailedAction(
                        "REVERT_VERSION",
                        "CONTENT",
                        contentId.toString(),
                        e.getMessage(),
                        e,
                        currentUser);
            } catch (Exception auditException) {
                log.error("Failed to log audit entry for failed revert operation", auditException);
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error reverting content: {} to version: {}", contentId, versionNumber, e);
            // Log failed action for audit trail
            try {
                User currentUser = securityUtil.getCurrentUser();
                auditService.logFailedAction(
                        "REVERT_VERSION",
                        "CONTENT",
                        contentId.toString(),
                        "Unexpected error during version revert",
                        e,
                        currentUser);
            } catch (Exception auditException) {
                log.error("Failed to log audit entry for failed revert operation", auditException);
            }
            throw new InternalServerException("Failed to revert content version");
        }
    }

    /**
     * Get version statistics for a content.
     * 
     * @param contentId Content ID
     * @return Version statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVersionStatistics(Long contentId) {
        try {
            // 1. Validate input
            if (contentId == null) {
                throw new BusinessException("Content ID is required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting version statistics for content: {} by user: {}", contentId, userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get statistics
            return contentVersionRepository.getVersionStatistics(contentId);

        } catch (BusinessException e) {
            log.error("Business error getting version statistics for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting version statistics for content: {}", contentId, e);
            throw new InternalServerException("Failed to get version statistics");
        }
    }

    /**
     * Create a version branch from an existing version.
     * 
     * @param contentId Content ID
     * @param request   Branch creation request
     * @return Created branch version
     */
    public ContentVersionDto createVersionBranch(Long contentId,
            ai.content.auto.dtos.CreateVersionBranchRequest request) {
        try {
            // 1. Validate input
            if (contentId == null || request == null) {
                throw new BusinessException("Content ID and branch request are required");
            }

            // 2. Get current user
            User currentUser = securityUtil.getCurrentUser();
            log.info("Creating version branch '{}' for content: {} by user: {}",
                    request.getBranchName(), contentId, currentUser.getId());

            // 3. Validate content ownership
            validateContentOwnership(contentId, currentUser.getId());

            // 4. Get parent version
            ContentVersion parentVersion = contentVersionRepository.findById(request.getParentVersionId())
                    .orElseThrow(() -> new NotFoundException("Parent version not found"));

            if (!parentVersion.getContentId().equals(contentId)) {
                throw new BusinessException("Parent version does not belong to this content");
            }

            // 5. Check if branch name already exists
            if (contentVersionRepository.findLatestVersionInBranch(contentId, request.getBranchName()).isPresent()) {
                throw new BusinessException("Branch name already exists");
            }

            // 6. Create branch version
            ContentVersion branchVersion = createBranchVersionInTransaction(
                    contentId, parentVersion, request, currentUser);

            log.info("Version branch '{}' created successfully for content: {} as version: {}",
                    request.getBranchName(), contentId, branchVersion.getVersionNumber());

            return contentVersionMapper.toDto(branchVersion);

        } catch (BusinessException e) {
            log.error("Business error creating version branch for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating version branch for content: {}", contentId, e);
            throw new InternalServerException("Failed to create version branch");
        }
    }

    /**
     * Tag and annotate a version.
     * 
     * @param contentId     Content ID
     * @param versionNumber Version number
     * @param request       Tag request
     * @return Updated version
     */
    public ContentVersionDto tagVersion(Long contentId, Integer versionNumber,
            ai.content.auto.dtos.VersionTagRequest request) {
        try {
            // 1. Validate input
            if (contentId == null || versionNumber == null || request == null) {
                throw new BusinessException("Content ID, version number, and tag request are required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Tagging version {} for content: {} with tag '{}' by user: {}",
                    versionNumber, contentId, request.getVersionTag(), userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Update version with tag and annotation
            ContentVersion version = tagVersionInTransaction(contentId, versionNumber, request);

            log.info("Version {} tagged successfully for content: {} with tag '{}'",
                    versionNumber, contentId, request.getVersionTag());

            return contentVersionMapper.toDto(version);

        } catch (BusinessException e) {
            log.error("Business error tagging version {} for content: {}", versionNumber, contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error tagging version {} for content: {}", versionNumber, contentId, e);
            throw new InternalServerException("Failed to tag version");
        }
    }

    /**
     * Get all branches for a content.
     * 
     * @param contentId Content ID
     * @return List of branch information
     */
    @Transactional(readOnly = true)
    public List<ai.content.auto.dtos.VersionBranchDto> getContentBranches(Long contentId) {
        try {
            // 1. Validate input
            if (contentId == null) {
                throw new BusinessException("Content ID is required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting branches for content: {} by user: {}", contentId, userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get all branch names
            List<String> branchNames = contentVersionRepository.findDistinctBranchesByContentId(contentId);

            // 5. Build branch DTOs
            return branchNames.stream()
                    .map(branchName -> buildBranchDto(contentId, branchName))
                    .toList();

        } catch (BusinessException e) {
            log.error("Business error getting branches for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting branches for content: {}", contentId, e);
            throw new InternalServerException("Failed to get content branches");
        }
    }

    /**
     * Get versions in a specific branch with pagination.
     * 
     * @param contentId  Content ID
     * @param branchName Branch name
     * @param page       Page number
     * @param size       Page size
     * @return PaginatedResponse of versions in the branch
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ContentVersionDto> getBranchVersions(Long contentId, String branchName, int page,
            int size) {
        try {
            // 1. Validate input
            if (contentId == null || branchName == null) {
                throw new BusinessException("Content ID and branch name are required");
            }
            if (page < 0) {
                throw new BusinessException("Page number must be non-negative");
            }
            if (size <= 0 || size > 100) {
                throw new BusinessException("Page size must be between 1 and 100");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting versions for branch '{}' in content: {} by user: {}, page: {}, size: {}",
                    branchName, contentId, userId, page, size);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get branch versions with pagination
            PageRequest pageable = PageRequest.of(page, size);
            Page<ContentVersion> versionsPage = contentVersionRepository
                    .findByContentIdAndBranchNameOrderByVersionNumberDesc(contentId, branchName, pageable);

            // 5. Map to DTOs
            List<ContentVersionDto> versionDtos = versionsPage.getContent().stream()
                    .map(contentVersionMapper::toDto)
                    .toList();

            // 6. Create paginated response
            PaginatedResponse.PaginationMetadata metadata = PaginatedResponse.PaginationMetadata.builder()
                    .page(versionsPage.getNumber())
                    .size(versionsPage.getSize())
                    .totalElements(versionsPage.getTotalElements())
                    .totalPages(versionsPage.getTotalPages())
                    .first(versionsPage.isFirst())
                    .last(versionsPage.isLast())
                    .hasNext(versionsPage.hasNext())
                    .hasPrevious(versionsPage.hasPrevious())
                    .numberOfElements(versionsPage.getNumberOfElements())
                    .build();

            return PaginatedResponse.<ContentVersionDto>builder()
                    .content(versionDtos)
                    .pagination(metadata)
                    .build();

        } catch (BusinessException e) {
            log.error("Business error getting branch versions for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting branch versions for content: {}", contentId, e);
            throw new InternalServerException("Failed to get branch versions");
        }
    }

    /**
     * Get all tags for a content.
     * 
     * @param contentId Content ID
     * @return List of version tags
     */
    @Transactional(readOnly = true)
    public List<String> getContentTags(Long contentId) {
        try {
            // 1. Validate input
            if (contentId == null) {
                throw new BusinessException("Content ID is required");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting tags for content: {} by user: {}", contentId, userId);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get all tags
            return contentVersionRepository.findDistinctTagsByContentId(contentId);

        } catch (BusinessException e) {
            log.error("Business error getting tags for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting tags for content: {}", contentId, e);
            throw new InternalServerException("Failed to get content tags");
        }
    }

    /**
     * Get version history with custom sorting options.
     * 
     * @param contentId Content ID
     * @param page      Page number (0-based)
     * @param size      Page size
     * @param sortBy    Sort field (version, created, quality)
     * @return PaginatedResponse of ContentVersionDto
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<ContentVersionDto> getVersionHistoryWithSorting(Long contentId, int page, int size,
            String sortBy) {
        try {
            // 1. Validate input
            if (contentId == null) {
                throw new BusinessException("Content ID is required");
            }
            if (page < 0) {
                throw new BusinessException("Page number must be non-negative");
            }
            if (size <= 0 || size > 100) {
                throw new BusinessException("Page size must be between 1 and 100");
            }

            // 2. Get current user
            Long userId = securityUtil.getCurrentUserId();
            log.info("Getting version history for content: {} by user: {}, page: {}, size: {}, sortBy: {}",
                    contentId, userId, page, size, sortBy);

            // 3. Validate content ownership
            validateContentOwnership(contentId, userId);

            // 4. Get versions with pagination and custom sorting
            Pageable pageable = PageRequest.of(page, size);
            Page<ContentVersion> versionsPage;

            switch (sortBy != null ? sortBy.toLowerCase() : "version") {
                case "created":
                    versionsPage = contentVersionRepository.findByContentIdOrderByCreatedAtDesc(contentId, pageable);
                    break;
                case "quality":
                    versionsPage = contentVersionRepository.findByContentIdOrderByQualityScoreDesc(contentId, pageable);
                    break;
                case "version":
                default:
                    versionsPage = contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId,
                            pageable);
                    break;
            }

            // 5. Map to DTOs with metadata
            List<ContentVersionDto> versionDtos = versionsPage.getContent().stream()
                    .map(version -> {
                        // For custom sorting, we need to check if this is the latest version by version
                        // number
                        Optional<ContentVersion> latestVersion = contentVersionRepository
                                .findLatestVersionByContentId(contentId);
                        boolean isLatest = latestVersion.isPresent() &&
                                version.getVersionNumber().equals(latestVersion.get().getVersionNumber());
                        return contentVersionMapper.toDtoWithMetadata(version, isLatest,
                                (int) versionsPage.getTotalElements());
                    })
                    .toList();

            // 6. Create paginated response
            PaginatedResponse.PaginationMetadata metadata = PaginatedResponse.PaginationMetadata.builder()
                    .page(versionsPage.getNumber())
                    .size(versionsPage.getSize())
                    .totalElements(versionsPage.getTotalElements())
                    .totalPages(versionsPage.getTotalPages())
                    .first(versionsPage.isFirst())
                    .last(versionsPage.isLast())
                    .hasNext(versionsPage.hasNext())
                    .hasPrevious(versionsPage.hasPrevious())
                    .numberOfElements(versionsPage.getNumberOfElements())
                    .build();

            return PaginatedResponse.<ContentVersionDto>builder()
                    .content(versionDtos)
                    .pagination(metadata)
                    .build();

        } catch (BusinessException e) {
            log.error("Business error getting version history with sorting for content: {}", contentId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting version history with sorting for content: {}", contentId, e);
            throw new InternalServerException("Failed to get version history");
        }
    }

    /**
     * Apply cleanup and archival policies for a content.
     * 
     * @param contentId Content ID
     */
    public void applyCleanupPolicies(Long contentId) {
        try {
            log.info("Applying cleanup policies for content: {}", contentId);

            // 1. Check version count limit
            long versionCount = contentVersionRepository.countByContentId(contentId);
            if (versionCount > MAX_VERSIONS_PER_CONTENT) {
                archiveOldVersions(contentId);
            }

            // 2. Archive old versions based on date
            archiveVersionsByDate(contentId);

            log.info("Cleanup policies applied for content: {}", contentId);

        } catch (Exception e) {
            log.error("Error applying cleanup policies for content: {}", contentId, e);
            // Don't throw - this is a background operation
        }
    }

    // Private transactional methods

    @Transactional
    protected ContentVersion createVersionInTransaction(Long contentId, ContentGenerateResponse response, User user) {
        // Get next version number
        Integer nextVersion = contentVersionRepository.getNextVersionNumber(contentId);

        // Build version entity
        ContentVersion version = ContentVersion.builder()
                .contentId(contentId)
                .versionNumber(nextVersion)
                .content(response.getGeneratedContent())
                .title(response.getTitle())
                .generationParams(response.getGenerationParams())
                .aiProvider(response.getAiProvider())
                .aiModel(response.getAiModel())
                .tokensUsed(response.getTokensUsed())
                .generationCost(response.getGenerationCost())
                .processingTimeMs(response.getProcessingTimeMs())
                .readabilityScore(response.getReadabilityScore())
                .seoScore(response.getSeoScore())
                .qualityScore(response.getQualityScore())
                .sentimentScore(response.getSentimentScore())
                .industry(response.getIndustry())
                .targetAudience(response.getTargetAudience())
                .tone(response.getTone())
                .language(response.getLanguage())
                .createdBy(user.getId())
                .build();

        // Calculate content statistics
        if (response.getGeneratedContent() != null) {
            version.setWordCount(countWords(response.getGeneratedContent()));
            version.setCharacterCount(response.getGeneratedContent().length());
        }

        // Apply storage optimization for large content
        if (isLargeContent(response.getGeneratedContent())) {
            optimizeContentStorage(version);
        }

        return contentVersionRepository.save(version);
    }

    @Transactional
    protected ContentVersion createRevertedVersionInTransaction(Long contentId, ContentVersion targetVersion,
            User user) {
        // Get next version number
        Integer nextVersion = contentVersionRepository.getNextVersionNumber(contentId);

        // Create new version based on target version
        ContentVersion revertedVersion = ContentVersion.builder()
                .contentId(contentId)
                .versionNumber(nextVersion)
                .content(targetVersion.getContent())
                .title(targetVersion.getTitle())
                .generationParams(targetVersion.getGenerationParams())
                .aiProvider(targetVersion.getAiProvider())
                .aiModel(targetVersion.getAiModel())
                .readabilityScore(targetVersion.getReadabilityScore())
                .seoScore(targetVersion.getSeoScore())
                .qualityScore(targetVersion.getQualityScore())
                .sentimentScore(targetVersion.getSentimentScore())
                .wordCount(targetVersion.getWordCount())
                .characterCount(targetVersion.getCharacterCount())
                .industry(targetVersion.getIndustry())
                .targetAudience(targetVersion.getTargetAudience())
                .tone(targetVersion.getTone())
                .language(targetVersion.getLanguage())
                .createdBy(user.getId())
                .build();

        return contentVersionRepository.save(revertedVersion);
    }

    @Transactional
    protected void updateContentToLatestVersionInTransaction(Long contentId, ContentVersion version) {
        try {
            ContentGeneration content = contentGenerationRepository.findById(contentId).orElse(null);
            if (content != null) {
                content.setCurrentVersion(version.getVersionNumber());
                content.setGeneratedContent(version.getContent());
                content.setTitle(version.getTitle());
                content.setUpdatedAt(Instant.now());
                contentGenerationRepository.save(content);
            }
        } catch (Exception e) {
            log.error("Error updating content to latest version for content: {}", contentId, e);
            // Don't throw - avoid breaking version creation
        }
    }

    @Transactional
    protected void archiveOldVersions(Long contentId) {
        try {
            // Keep only the latest N versions, archive the rest
            List<ContentVersion> allVersions = contentVersionRepository
                    .findByContentIdOrderByVersionNumberDesc(contentId);

            if (allVersions.size() > MAX_VERSIONS_PER_CONTENT) {
                List<ContentVersion> versionsToArchive = allVersions.subList(
                        MAX_VERSIONS_PER_CONTENT, allVersions.size());

                for (ContentVersion version : versionsToArchive) {
                    // For now, we'll just log the archival
                    // In a full implementation, this would move content to archive storage
                    log.info("Archiving version {} for content: {}", version.getVersionNumber(), contentId);
                }
            }
        } catch (Exception e) {
            log.error("Error archiving old versions for content: {}", contentId, e);
        }
    }

    @Transactional
    protected void archiveVersionsByDate(Long contentId) {
        try {
            Instant archiveThreshold = Instant.now().minus(ARCHIVE_THRESHOLD_DAYS, ChronoUnit.DAYS);

            // Find versions older than threshold
            List<ContentVersion> oldVersions = contentVersionRepository
                    .findByContentIdOrderByVersionNumberDesc(contentId)
                    .stream()
                    .filter(v -> v.getCreatedAt().isBefore(archiveThreshold))
                    .toList();

            for (ContentVersion version : oldVersions) {
                // For now, we'll just log the archival
                // In a full implementation, this would move content to archive storage
                log.info("Archiving old version {} for content: {} (created: {})",
                        version.getVersionNumber(), contentId, version.getCreatedAt());
            }
        } catch (Exception e) {
            log.error("Error archiving versions by date for content: {}", contentId, e);
        }
    }

    @Transactional
    protected ContentVersion createBranchVersionInTransaction(Long contentId, ContentVersion parentVersion,
            ai.content.auto.dtos.CreateVersionBranchRequest request, User user) {

        // Get next version number
        Integer nextVersion = contentVersionRepository.getNextVersionNumber(contentId);

        // Create branch version based on parent
        ContentVersion branchVersion = ContentVersion.builder()
                .contentId(contentId)
                .versionNumber(nextVersion)
                .content(parentVersion.getContent())
                .title(parentVersion.getTitle())
                .generationParams(parentVersion.getGenerationParams())
                .aiProvider(parentVersion.getAiProvider())
                .aiModel(parentVersion.getAiModel())
                .readabilityScore(parentVersion.getReadabilityScore())
                .seoScore(parentVersion.getSeoScore())
                .qualityScore(parentVersion.getQualityScore())
                .sentimentScore(parentVersion.getSentimentScore())
                .wordCount(parentVersion.getWordCount())
                .characterCount(parentVersion.getCharacterCount())
                .industry(parentVersion.getIndustry())
                .targetAudience(parentVersion.getTargetAudience())
                .tone(parentVersion.getTone())
                .language(parentVersion.getLanguage())
                // Branch-specific fields
                .parentVersionId(parentVersion.getId())
                .branchName(request.getBranchName())
                .isExperimental(request.getIsExperimental())
                .annotation(request.getAnnotation())
                .createdBy(user.getId())
                .build();

        return contentVersionRepository.save(branchVersion);
    }

    @Transactional
    protected ContentVersion tagVersionInTransaction(Long contentId, Integer versionNumber,
            ai.content.auto.dtos.VersionTagRequest request) {

        ContentVersion version = contentVersionRepository
                .findByContentIdAndVersionNumber(contentId, versionNumber)
                .orElseThrow(() -> new NotFoundException("Version not found"));

        // Update tag and annotation
        version.setVersionTag(request.getVersionTag());
        version.setAnnotation(request.getAnnotation());
        version.setUpdatedAt(Instant.now());

        return contentVersionRepository.save(version);
    }

    // Private validation methods

    private void validateCreateVersionInput(Long contentId, ContentGenerateResponse response) {
        if (contentId == null) {
            throw new BusinessException("Content ID is required");
        }
        if (response == null) {
            throw new BusinessException("Content generation response is required");
        }
        if (response.getGeneratedContent() == null || response.getGeneratedContent().trim().isEmpty()) {
            throw new BusinessException("Generated content is required");
        }
    }

    private void validateContentOwnership(Long contentId, Long userId) {
        ContentGeneration content = contentGenerationRepository.findById(contentId)
                .orElseThrow(() -> new NotFoundException("Content not found"));

        if (!content.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied to this content");
        }
    }

    // Private helper methods

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
    }

    private boolean isLargeContent(String content) {
        return content != null && content.length() > LARGE_CONTENT_THRESHOLD;
    }

    private void optimizeContentStorage(ContentVersion version) {
        // For large content, we could implement compression or other optimizations
        // For now, we'll just log the optimization
        log.info("Optimizing storage for large content version: {} (size: {} chars)",
                version.getVersionNumber(), version.getCharacterCount());
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private ai.content.auto.dtos.VersionBranchDto buildBranchDto(Long contentId, String branchName) {
        try {
            // Get latest version in branch
            Optional<ContentVersion> latestVersion = contentVersionRepository
                    .findLatestVersionInBranch(contentId, branchName);

            if (latestVersion.isEmpty()) {
                return null;
            }

            // Get all versions in branch (limited to first page for summary)
            PageRequest pageable = PageRequest.of(0, 10);
            Page<ContentVersion> versionsPage = contentVersionRepository
                    .findByContentIdAndBranchNameOrderByVersionNumberDesc(contentId, branchName, pageable);

            // Get branch creation date (earliest version in branch)
            Instant createdAt = versionsPage.getContent().stream()
                    .map(ContentVersion::getCreatedAt)
                    .min(Instant::compareTo)
                    .orElse(null);

            // Get last modified date (latest version in branch)
            Instant lastModifiedAt = latestVersion.get().getUpdatedAt();

            return ai.content.auto.dtos.VersionBranchDto.builder()
                    .branchName(branchName)
                    .description(latestVersion.get().getAnnotation())
                    .isExperimental(latestVersion.get().getIsExperimental())
                    .versionCount((int) versionsPage.getTotalElements())
                    .latestVersion(contentVersionMapper.toDto(latestVersion.get()))
                    .versions(versionsPage.getContent().stream()
                            .map(contentVersionMapper::toLightweightDto)
                            .toList())
                    .createdAt(createdAt)
                    .lastModifiedAt(lastModifiedAt)
                    .build();

        } catch (Exception e) {
            log.error("Error building branch DTO for branch: {} in content: {}", branchName, contentId, e);
            return null;
        }
    }
}