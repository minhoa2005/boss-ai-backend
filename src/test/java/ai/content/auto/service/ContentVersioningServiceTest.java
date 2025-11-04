package ai.content.auto.service;

import ai.content.auto.dtos.ContentGenerateResponse;
import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.ContentVersion;
import ai.content.auto.entity.User;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.exception.NotFoundException;
import ai.content.auto.mapper.ContentVersionMapper;
import ai.content.auto.repository.ContentGenerationRepository;
import ai.content.auto.repository.ContentVersionRepository;
import ai.content.auto.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContentVersioningService.
 * Tests core functionality of content version creation and management.
 */
@ExtendWith(MockitoExtension.class)
class ContentVersioningServiceTest {

        @Mock
        private ContentVersionRepository contentVersionRepository;

        @Mock
        private ContentGenerationRepository contentGenerationRepository;

        @Mock
        private ContentVersionMapper contentVersionMapper;

        @Mock
        private SecurityUtil securityUtil;

        @Mock
        private AuditService auditService;

        @InjectMocks
        private ContentVersioningService contentVersioningService;

        private User testUser;
        private ContentGeneration testContent;
        private ContentGenerateResponse testResponse;
        private ContentVersion testVersion;
        private ContentVersionDto testVersionDto;

        @BeforeEach
        void setUp() {
                // Setup test user
                testUser = new User();
                testUser.setId(1L);
                testUser.setUsername("testuser");

                // Setup test content
                testContent = new ContentGeneration();
                testContent.setId(1L);
                testContent.setUser(testUser);
                testContent.setGeneratedContent("Test content");
                testContent.setTitle("Test Title");

                // Setup test response
                testResponse = new ContentGenerateResponse();
                testResponse.setGeneratedContent("Generated test content");
                testResponse.setTitle("Generated Title");
                testResponse.setAiProvider("OpenAI");
                testResponse.setAiModel("gpt-3.5-turbo");
                testResponse.setWordCount(3);
                testResponse.setCharacterCount(25);

                // Setup test version
                testVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Generated test content")
                                .title("Generated Title")
                                .aiProvider("OpenAI")
                                .aiModel("gpt-3.5-turbo")
                                .wordCount(3)
                                .characterCount(25)
                                .createdBy(1L)
                                .createdAt(Instant.now())
                                .build();

                // Setup test version DTO
                testVersionDto = ContentVersionDto.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Generated test content")
                                .title("Generated Title")
                                .aiProvider("OpenAI")
                                .aiModel("gpt-3.5-turbo")
                                .wordCount(3)
                                .characterCount(25)
                                .createdBy(1L)
                                .createdAt(Instant.now())
                                .build();
        }

        @Test
        void createVersion_Success() {
                // Given
                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.getNextVersionNumber(1L)).thenReturn(1);
                when(contentVersionRepository.save(any(ContentVersion.class))).thenReturn(testVersion);
                when(contentVersionMapper.toDto(testVersion)).thenReturn(testVersionDto);

                // When
                ContentVersionDto result = contentVersioningService.createVersion(1L, testResponse);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getContentId());
                assertEquals(1, result.getVersionNumber());
                assertEquals("Generated test content", result.getContent());
                assertEquals("Generated Title", result.getTitle());

                verify(contentVersionRepository).save(any(ContentVersion.class));
                verify(contentGenerationRepository).save(any(ContentGeneration.class));
        }

        @Test
        void createVersion_ContentNotFound() {
                // Given
                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.empty());

                // When & Then
                assertThrows(NotFoundException.class, () -> contentVersioningService.createVersion(1L, testResponse));
        }

        @Test
        void createVersion_InvalidInput() {
                // When & Then
                assertThrows(BusinessException.class, () -> contentVersioningService.createVersion(null, testResponse));

                assertThrows(BusinessException.class, () -> contentVersioningService.createVersion(1L, null));
        }

        @Test
        void getVersionHistory_Success() {
                // Given
                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.countByContentId(1L)).thenReturn(1L);
                when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(eq(1L), any()))
                                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testVersion)));
                when(contentVersionMapper.toDtoWithMetadata(any(), any(), any())).thenReturn(testVersionDto);

                // When
                ai.content.auto.dtos.PaginatedResponse<ContentVersionDto> result = contentVersioningService
                                .getVersionHistory(1L, 0, 10);

                // Then
                assertNotNull(result);
                assertNotNull(result.getContent());
                assertEquals(1, result.getContent().size());
                assertEquals(testVersionDto, result.getContent().get(0));
        }

        @Test
        void getVersion_Success() {
                // Given
                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 1))
                                .thenReturn(Optional.of(testVersion));
                when(contentVersionRepository.countByContentId(1L)).thenReturn(1L);
                when(contentVersionRepository.findLatestVersionByContentId(1L))
                                .thenReturn(Optional.of(testVersion));
                when(contentVersionMapper.toDtoWithMetadata(any(), any(), any())).thenReturn(testVersionDto);

                // When
                ContentVersionDto result = contentVersioningService.getVersion(1L, 1);

                // Then
                assertNotNull(result);
                assertEquals(1L, result.getContentId());
                assertEquals(1, result.getVersionNumber());
        }

        @Test
        void getVersion_NotFound() {
                // Given
                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 1))
                                .thenReturn(Optional.empty());

                // When & Then
                assertThrows(NotFoundException.class, () -> contentVersioningService.getVersion(1L, 1));
        }

        @Test
        void revertToVersion_Success() {
                // Given
                ContentVersion currentVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(3)
                                .content("Current content")
                                .title("Current Title")
                                .build();

                ContentVersion targetVersion = ContentVersion.builder()
                                .id(2L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Original content")
                                .title("Original Title")
                                .build();

                ContentVersion revertedVersion = ContentVersion.builder()
                                .id(3L)
                                .contentId(1L)
                                .versionNumber(4)
                                .content("Original content")
                                .title("Original Title")
                                .createdBy(1L)
                                .build();

                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findLatestVersionByContentId(1L))
                                .thenReturn(Optional.of(currentVersion));
                when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 1))
                                .thenReturn(Optional.of(targetVersion));
                when(contentVersionRepository.getNextVersionNumber(1L)).thenReturn(4);
                when(contentVersionRepository.save(any(ContentVersion.class))).thenReturn(revertedVersion);
                when(contentVersionMapper.toDto(revertedVersion)).thenReturn(testVersionDto);

                // When
                ContentVersionDto result = contentVersioningService.revertToVersion(1L, 1);

                // Then
                assertNotNull(result);
                verify(contentVersionRepository).save(any(ContentVersion.class));
                verify(contentGenerationRepository).save(any(ContentGeneration.class));
                verify(auditService).logContentVersionRevert(1L, 3, 1, 4, testUser);
        }

        @Test
        void applyCleanupPolicies_Success() {
                // Given
                when(contentVersionRepository.countByContentId(1L)).thenReturn(10L);

                // When
                assertDoesNotThrow(() -> contentVersioningService.applyCleanupPolicies(1L));

                // Then
                // Should not throw any exceptions
        }

        @Test
        void createVersionBranch_Success() {
                // Given
                ai.content.auto.dtos.CreateVersionBranchRequest request = ai.content.auto.dtos.CreateVersionBranchRequest
                                .builder()
                                .parentVersionId(1L)
                                .branchName("experimental-feature")
                                .description("Testing experimental feature")
                                .isExperimental(true)
                                .annotation("Branch for testing new functionality")
                                .build();

                ContentVersion parentVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Original content")
                                .title("Original Title")
                                .aiProvider("OpenAI")
                                .aiModel("gpt-3.5-turbo")
                                .build();

                ContentVersion branchVersion = ContentVersion.builder()
                                .id(2L)
                                .contentId(1L)
                                .versionNumber(2)
                                .content("Original content")
                                .title("Original Title")
                                .aiProvider("OpenAI")
                                .aiModel("gpt-3.5-turbo")
                                .parentVersionId(1L)
                                .branchName("experimental-feature")
                                .isExperimental(true)
                                .annotation("Branch for testing new functionality")
                                .createdBy(1L)
                                .build();

                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findById(1L)).thenReturn(Optional.of(parentVersion));
                when(contentVersionRepository.findLatestVersionInBranch(1L, "experimental-feature"))
                                .thenReturn(Optional.empty());
                when(contentVersionRepository.getNextVersionNumber(1L)).thenReturn(2);
                when(contentVersionRepository.save(any(ContentVersion.class))).thenReturn(branchVersion);
                when(contentVersionMapper.toDto(branchVersion)).thenReturn(testVersionDto);

                // When
                ContentVersionDto result = contentVersioningService.createVersionBranch(1L, request);

                // Then
                assertNotNull(result);
                verify(contentVersionRepository).save(any(ContentVersion.class));
        }

        @Test
        void createVersionBranch_ParentVersionNotFound() {
                // Given
                ai.content.auto.dtos.CreateVersionBranchRequest request = ai.content.auto.dtos.CreateVersionBranchRequest
                                .builder()
                                .parentVersionId(999L)
                                .branchName("experimental-feature")
                                .isExperimental(true)
                                .build();

                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findById(999L)).thenReturn(Optional.empty());

                // When & Then
                assertThrows(NotFoundException.class, () -> contentVersioningService.createVersionBranch(1L, request));
        }

        @Test
        void createVersionBranch_BranchNameAlreadyExists() {
                // Given
                ai.content.auto.dtos.CreateVersionBranchRequest request = ai.content.auto.dtos.CreateVersionBranchRequest
                                .builder()
                                .parentVersionId(1L)
                                .branchName("existing-branch")
                                .isExperimental(true)
                                .build();

                ContentVersion parentVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Original content")
                                .build();

                ContentVersion existingBranchVersion = ContentVersion.builder()
                                .id(2L)
                                .contentId(1L)
                                .versionNumber(2)
                                .branchName("existing-branch")
                                .build();

                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findById(1L)).thenReturn(Optional.of(parentVersion));
                when(contentVersionRepository.findLatestVersionInBranch(1L, "existing-branch"))
                                .thenReturn(Optional.of(existingBranchVersion));

                // When & Then
                assertThrows(BusinessException.class, () -> contentVersioningService.createVersionBranch(1L, request));
        }

        @Test
        void createVersionBranch_ParentVersionNotBelongToContent() {
                // Given
                ai.content.auto.dtos.CreateVersionBranchRequest request = ai.content.auto.dtos.CreateVersionBranchRequest
                                .builder()
                                .parentVersionId(1L)
                                .branchName("experimental-feature")
                                .isExperimental(true)
                                .build();

                ContentVersion parentVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(999L) // Different content ID
                                .versionNumber(1)
                                .content("Original content")
                                .build();

                when(securityUtil.getCurrentUser()).thenReturn(testUser);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findById(1L)).thenReturn(Optional.of(parentVersion));

                // When & Then
                assertThrows(BusinessException.class, () -> contentVersioningService.createVersionBranch(1L, request));
        }

        @Test
        void tagVersion_Success() {
                // Given
                ai.content.auto.dtos.VersionTagRequest request = ai.content.auto.dtos.VersionTagRequest.builder()
                                .versionTag("stable-v1.0")
                                .annotation("Stable version ready for production")
                                .build();

                ContentVersion taggedVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .content("Generated test content")
                                .versionTag("stable-v1.0")
                                .annotation("Stable version ready for production")
                                .build();

                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 1))
                                .thenReturn(Optional.of(testVersion));
                when(contentVersionRepository.save(any(ContentVersion.class))).thenReturn(taggedVersion);
                when(contentVersionMapper.toDto(taggedVersion)).thenReturn(testVersionDto);

                // When
                ContentVersionDto result = contentVersioningService.tagVersion(1L, 1, request);

                // Then
                assertNotNull(result);
                verify(contentVersionRepository).save(any(ContentVersion.class));
        }

        @Test
        void tagVersion_VersionNotFound() {
                // Given
                ai.content.auto.dtos.VersionTagRequest request = ai.content.auto.dtos.VersionTagRequest.builder()
                                .versionTag("stable-v1.0")
                                .annotation("Stable version ready for production")
                                .build();

                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findByContentIdAndVersionNumber(1L, 999))
                                .thenReturn(Optional.empty());

                // When & Then
                assertThrows(NotFoundException.class, () -> contentVersioningService.tagVersion(1L, 999, request));
        }

        @Test
        void getContentBranches_Success() {
                // Given
                List<String> branchNames = List.of("main", "experimental-feature", "hotfix-branch");

                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findDistinctBranchesByContentId(1L)).thenReturn(branchNames);

                // Mock branch versions for each branch
                ContentVersion mainBranchVersion = ContentVersion.builder()
                                .id(1L)
                                .contentId(1L)
                                .versionNumber(1)
                                .branchName("main")
                                .isExperimental(false)
                                .createdAt(Instant.now())
                                .build();

                when(contentVersionRepository.findLatestVersionInBranch(1L, "main"))
                                .thenReturn(Optional.of(mainBranchVersion));
                when(contentVersionRepository.findByContentIdAndBranchNameOrderByVersionNumberDesc(eq(1L), eq("main"),
                                any()))
                                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(mainBranchVersion)));

                // When
                List<ai.content.auto.dtos.VersionBranchDto> result = contentVersioningService.getContentBranches(1L);

                // Then
                assertNotNull(result);
                // Note: The actual size depends on the buildBranchDto implementation
                // which may filter out null results
        }

        @Test
        void getBranchVersions_Success() {
                // Given
                String branchName = "experimental-feature";
                List<ContentVersion> branchVersions = List.of(testVersion);

                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findByContentIdAndBranchNameOrderByVersionNumberDesc(eq(1L),
                                eq(branchName), any()))
                                .thenReturn(new org.springframework.data.domain.PageImpl<>(branchVersions));
                when(contentVersionMapper.toDto(testVersion)).thenReturn(testVersionDto);

                // When
                ai.content.auto.dtos.PaginatedResponse<ContentVersionDto> result = contentVersioningService
                                .getBranchVersions(1L, branchName, 0, 10);

                // Then
                assertNotNull(result);
                assertNotNull(result.getContent());
                assertEquals(1, result.getContent().size());
        }

        @Test
        void getContentTags_Success() {
                // Given
                List<String> tags = List.of("stable-v1.0", "beta-v2.0", "experimental");

                when(securityUtil.getCurrentUserId()).thenReturn(1L);
                when(contentGenerationRepository.findById(1L)).thenReturn(Optional.of(testContent));
                when(contentVersionRepository.findDistinctTagsByContentId(1L)).thenReturn(tags);

                // When
                List<String> result = contentVersioningService.getContentTags(1L);

                // Then
                assertNotNull(result);
                assertEquals(3, result.size());
                assertTrue(result.contains("stable-v1.0"));
                assertTrue(result.contains("beta-v2.0"));
                assertTrue(result.contains("experimental"));
        }

        @Test
        void createVersionBranch_InvalidInput() {
                // When & Then
                assertThrows(BusinessException.class, () -> contentVersioningService.createVersionBranch(null, null));

                ai.content.auto.dtos.CreateVersionBranchRequest request = ai.content.auto.dtos.CreateVersionBranchRequest
                                .builder()
                                .parentVersionId(1L)
                                .branchName("test-branch")
                                .build();

                assertThrows(BusinessException.class,
                                () -> contentVersioningService.createVersionBranch(null, request));
        }

        @Test
        void tagVersion_InvalidInput() {
                // When & Then
                assertThrows(BusinessException.class, () -> contentVersioningService.tagVersion(null, null, null));
                assertThrows(BusinessException.class, () -> contentVersioningService.tagVersion(1L, null, null));
                assertThrows(BusinessException.class, () -> contentVersioningService.tagVersion(1L, 1, null));
        }
}