package ai.content.auto.controller;

import ai.content.auto.dtos.BaseResponse;
import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.dtos.ContentVersionComparisonDto;
import ai.content.auto.dtos.PaginatedResponse;
import ai.content.auto.service.ContentVersioningService;
import ai.content.auto.service.ContentComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for content versioning operations.
 * Provides endpoints for managing content versions and comparisons.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@RestController
@RequestMapping("/api/v1/content/{contentId}/versions")
@RequiredArgsConstructor
@Slf4j
public class ContentVersionController {

        private final ContentVersioningService contentVersioningService;
        private final ContentComparisonService contentComparisonService;

        /**
         * Get version history for a content with pagination.
         * 
         * @param contentId Content ID
         * @param page      Page number (default: 0)
         * @param size      Page size (default: 20, max: 100)
         * @return Paginated list of content versions
         */
        @GetMapping
        public ResponseEntity<BaseResponse<ai.content.auto.dtos.PaginatedResponse<ContentVersionDto>>> getVersionHistory(
                        @PathVariable Long contentId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                log.info("Getting version history for content: {}, page: {}, size: {}", contentId, page, size);

                ai.content.auto.dtos.PaginatedResponse<ContentVersionDto> paginatedVersions = contentVersioningService
                                .getVersionHistory(contentId, page, size);

                BaseResponse<ai.content.auto.dtos.PaginatedResponse<ContentVersionDto>> response = new BaseResponse<ai.content.auto.dtos.PaginatedResponse<ContentVersionDto>>()
                                .setErrorMessage("Version history retrieved successfully")
                                .setData(paginatedVersions);

                return ResponseEntity.ok(response);
        }

        /**
         * Get a specific version by version number.
         * 
         * @param contentId     Content ID
         * @param versionNumber Version number
         * @return Content version details
         */
        @GetMapping("/{versionNumber}")
        public ResponseEntity<BaseResponse<ContentVersionDto>> getVersion(
                        @PathVariable Long contentId,
                        @PathVariable Integer versionNumber) {

                log.info("Getting version {} for content: {}", versionNumber, contentId);

                ContentVersionDto version = contentVersioningService.getVersion(contentId, versionNumber);

                BaseResponse<ContentVersionDto> response = new BaseResponse<ContentVersionDto>()
                                .setErrorMessage("Version retrieved successfully")
                                .setData(version);

                return ResponseEntity.ok(response);
        }

        /**
         * Revert content to a specific version.
         * 
         * @param contentId     Content ID
         * @param versionNumber Version number to revert to
         * @return New version created from the reverted content
         */
        @PostMapping("/{versionNumber}/revert")
        public ResponseEntity<BaseResponse<ContentVersionDto>> revertToVersion(
                        @PathVariable Long contentId,
                        @PathVariable Integer versionNumber) {

                log.info("Reverting content: {} to version: {}", contentId, versionNumber);

                ContentVersionDto newVersion = contentVersioningService.revertToVersion(contentId, versionNumber);

                BaseResponse<ContentVersionDto> response = new BaseResponse<ContentVersionDto>()
                                .setErrorMessage("Content reverted successfully")
                                .setData(newVersion);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Get version statistics for a content.
         * 
         * @param contentId Content ID
         * @return Version statistics
         */
        @GetMapping("/statistics")
        public ResponseEntity<BaseResponse<Map<String, Object>>> getVersionStatistics(
                        @PathVariable Long contentId) {

                log.info("Getting version statistics for content: {}", contentId);

                Map<String, Object> statistics = contentVersioningService.getVersionStatistics(contentId);

                BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                                .setErrorMessage("Version statistics retrieved successfully")
                                .setData(statistics);

                return ResponseEntity.ok(response);
        }

        /**
         * Apply cleanup policies for a content.
         * 
         * @param contentId Content ID
         * @return Success response
         */
        @PostMapping("/cleanup")
        public ResponseEntity<BaseResponse<Void>> applyCleanupPolicies(
                        @PathVariable Long contentId) {

                log.info("Applying cleanup policies for content: {}", contentId);

                contentVersioningService.applyCleanupPolicies(contentId);

                BaseResponse<Void> response = new BaseResponse<Void>()
                                .setErrorMessage("Cleanup policies applied successfully");

                return ResponseEntity.ok(response);
        }

        /**
         * Compare two content versions with detailed analysis.
         * 
         * @param contentId      Content ID
         * @param versionNumberA Version A number
         * @param versionNumberB Version B number
         * @return Detailed comparison results
         */
        @GetMapping("/compare")
        public ResponseEntity<BaseResponse<ContentVersionComparisonDto>> compareVersions(
                        @PathVariable Long contentId,
                        @RequestParam Integer versionA,
                        @RequestParam Integer versionB) {

                log.info("Comparing versions {} vs {} for content: {}", versionA, versionB, contentId);

                ContentVersionComparisonDto comparison = contentComparisonService.compareVersions(
                                contentId, versionA, versionB);

                BaseResponse<ContentVersionComparisonDto> response = new BaseResponse<ContentVersionComparisonDto>()
                                .setErrorMessage("Version comparison completed successfully")
                                .setData(comparison);

                return ResponseEntity.ok(response);
        }

        /**
         * Get side-by-side comparison for UI display.
         * 
         * @param contentId      Content ID
         * @param versionNumberA Version A number
         * @param versionNumberB Version B number
         * @return Side-by-side comparison data
         */
        @GetMapping("/compare/side-by-side")
        public ResponseEntity<BaseResponse<Map<String, Object>>> getSideBySideComparison(
                        @PathVariable Long contentId,
                        @RequestParam Integer versionA,
                        @RequestParam Integer versionB) {

                log.info("Getting side-by-side comparison for versions {} vs {} for content: {}",
                                versionA, versionB, contentId);

                Map<String, Object> sideBySide = contentComparisonService.getSideBySideComparison(
                                contentId, versionA, versionB);

                BaseResponse<Map<String, Object>> response = new BaseResponse<Map<String, Object>>()
                                .setErrorMessage("Side-by-side comparison retrieved successfully")
                                .setData(sideBySide);

                return ResponseEntity.ok(response);
        }

        /**
         * Create a version branch from an existing version.
         * 
         * @param contentId Content ID
         * @param request   Branch creation request
         * @return Created branch version
         */
        @PostMapping("/branch")
        public ResponseEntity<BaseResponse<ContentVersionDto>> createVersionBranch(
                        @PathVariable Long contentId,
                        @Valid @RequestBody ai.content.auto.dtos.CreateVersionBranchRequest request) {

                log.info("Creating version branch '{}' for content: {}", request.getBranchName(), contentId);

                ContentVersionDto branchVersion = contentVersioningService.createVersionBranch(contentId, request);

                BaseResponse<ContentVersionDto> response = new BaseResponse<ContentVersionDto>()
                                .setErrorMessage("Version branch created successfully")
                                .setData(branchVersion);

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        /**
         * Tag and annotate a version.
         * 
         * @param contentId     Content ID
         * @param versionNumber Version number
         * @param request       Tag request
         * @return Updated version
         */
        @PostMapping("/{versionNumber}/tag")
        public ResponseEntity<BaseResponse<ContentVersionDto>> tagVersion(
                        @PathVariable Long contentId,
                        @PathVariable Integer versionNumber,
                        @Valid @RequestBody ai.content.auto.dtos.VersionTagRequest request) {

                log.info("Tagging version {} for content: {} with tag '{}'",
                                versionNumber, contentId, request.getVersionTag());

                ContentVersionDto taggedVersion = contentVersioningService.tagVersion(contentId, versionNumber,
                                request);

                BaseResponse<ContentVersionDto> response = new BaseResponse<ContentVersionDto>()
                                .setErrorMessage("Version tagged successfully")
                                .setData(taggedVersion);

                return ResponseEntity.ok(response);
        }

        /**
         * Get all branches for a content.
         * 
         * @param contentId Content ID
         * @return List of branch information
         */
        @GetMapping("/branches")
        public ResponseEntity<BaseResponse<List<ai.content.auto.dtos.VersionBranchDto>>> getContentBranches(
                        @PathVariable Long contentId) {

                log.info("Getting branches for content: {}", contentId);

                List<ai.content.auto.dtos.VersionBranchDto> branches = contentVersioningService
                                .getContentBranches(contentId);

                BaseResponse<List<ai.content.auto.dtos.VersionBranchDto>> response = new BaseResponse<List<ai.content.auto.dtos.VersionBranchDto>>()
                                .setErrorMessage("Content branches retrieved successfully")
                                .setData(branches);

                return ResponseEntity.ok(response);
        }

        /**
         * Get versions in a specific branch with pagination.
         * 
         * @param contentId  Content ID
         * @param branchName Branch name
         * @param page       Page number (default: 0)
         * @param size       Page size (default: 20, max: 100)
         * @return Paginated list of versions in the branch
         */
        @GetMapping("/branches/{branchName}")
        public ResponseEntity<BaseResponse<PaginatedResponse<ContentVersionDto>>> getBranchVersions(
                        @PathVariable Long contentId,
                        @PathVariable String branchName,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {

                log.info("Getting versions for branch '{}' in content: {}, page: {}, size: {}",
                                branchName, contentId, page, size);

                PaginatedResponse<ContentVersionDto> paginatedVersions = contentVersioningService.getBranchVersions(
                                contentId, branchName, page, size);

                BaseResponse<PaginatedResponse<ContentVersionDto>> response = new BaseResponse<PaginatedResponse<ContentVersionDto>>()
                                .setErrorMessage("Branch versions retrieved successfully")
                                .setData(paginatedVersions);

                return ResponseEntity.ok(response);
        }

        /**
         * Get all tags for a content.
         * 
         * @param contentId Content ID
         * @return List of version tags
         */
        @GetMapping("/tags")
        public ResponseEntity<BaseResponse<List<String>>> getContentTags(
                        @PathVariable Long contentId) {

                log.info("Getting tags for content: {}", contentId);

                List<String> tags = contentVersioningService.getContentTags(contentId);

                BaseResponse<List<String>> response = new BaseResponse<List<String>>()
                                .setErrorMessage("Content tags retrieved successfully")
                                .setData(tags);

                return ResponseEntity.ok(response);
        }

        /**
         * Get version history with custom sorting options.
         * 
         * @param contentId Content ID
         * @param page      Page number (default: 0)
         * @param size      Page size (default: 20, max: 100)
         * @param sortBy    Sort field: version (default), created, quality
         * @return Paginated list of content versions with custom sorting
         */
        @GetMapping("/sorted")
        public ResponseEntity<BaseResponse<PaginatedResponse<ContentVersionDto>>> getVersionHistoryWithSorting(
                        @PathVariable Long contentId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(defaultValue = "version") String sortBy) {

                log.info("Getting sorted version history for content: {}, page: {}, size: {}, sortBy: {}",
                                contentId, page, size, sortBy);

                PaginatedResponse<ContentVersionDto> paginatedVersions = contentVersioningService
                                .getVersionHistoryWithSorting(contentId, page, size, sortBy);

                BaseResponse<PaginatedResponse<ContentVersionDto>> response = new BaseResponse<PaginatedResponse<ContentVersionDto>>()
                                .setErrorMessage("Sorted version history retrieved successfully")
                                .setData(paginatedVersions);

                return ResponseEntity.ok(response);
        }
}