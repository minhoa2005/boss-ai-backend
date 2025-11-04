package ai.content.auto.service;

import ai.content.auto.dtos.ContentVersionDto;
import ai.content.auto.dtos.PaginatedResponse;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.ContentVersion;
import ai.content.auto.entity.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ContentVersioningService pagination functionality.
 * Tests the version history retrieval with pagination implementation.
 */
@ExtendWith(MockitoExtension.class)
class ContentVersioningServicePaginationTest {

    @Mock
    private ContentVersionRepository contentVersionRepository;

    @Mock
    private ContentGenerationRepository contentGenerationRepository;

    @Mock
    private ContentVersionMapper contentVersionMapper;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private ContentVersioningService contentVersioningService;

    private User testUser;
    private ContentGeneration testContent;
    private List<ContentVersion> testVersions;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testContent = ContentGeneration.builder()
                .id(1L)
                .user(testUser)
                .generatedContent("Test content")
                .title("Test Title")
                .currentVersion(3)
                .build();

        // Create test versions
        testVersions = Arrays.asList(
                createTestVersion(1L, 1L, 3, "Version 3 content"),
                createTestVersion(2L, 1L, 2, "Version 2 content"),
                createTestVersion(3L, 1L, 1, "Version 1 content"));
    }

    @Test
    void testGetVersionHistoryWithPagination_Success() {
        // Arrange
        Long contentId = 1L;
        int page = 0;
        int size = 2;

        Pageable pageable = PageRequest.of(page, size);
        Page<ContentVersion> versionsPage = new PageImpl<>(
                testVersions.subList(0, 2), // First 2 versions
                pageable,
                testVersions.size());

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(testContent));
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId, pageable))
                .thenReturn(versionsPage);

        // Mock mapper calls
        when(contentVersionMapper.toDtoWithMetadata(any(ContentVersion.class), anyBoolean(), anyInt()))
                .thenAnswer(invocation -> {
                    ContentVersion version = invocation.getArgument(0);
                    Boolean isLatest = invocation.getArgument(1);
                    Integer totalVersions = invocation.getArgument(2);

                    return ContentVersionDto.builder()
                            .id(version.getId())
                            .contentId(version.getContentId())
                            .versionNumber(version.getVersionNumber())
                            .content(version.getContent())
                            .isLatestVersion(isLatest)
                            .totalVersions(totalVersions)
                            .build();
                });

        // Act
        PaginatedResponse<ContentVersionDto> result = contentVersioningService.getVersionHistory(contentId, page, size);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertNotNull(result.getPagination());

        // Check content
        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getContent().get(0).getVersionNumber()); // Latest version first
        assertEquals(2, result.getContent().get(1).getVersionNumber());

        // Check pagination metadata
        PaginatedResponse.PaginationMetadata pagination = result.getPagination();
        assertEquals(0, pagination.getPage());
        assertEquals(2, pagination.getSize());
        assertEquals(3, pagination.getTotalElements());
        assertEquals(2, pagination.getTotalPages());
        assertTrue(pagination.isFirst());
        assertFalse(pagination.isLast());
        assertTrue(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
        assertEquals(2, pagination.getNumberOfElements());

        // Verify interactions
        verify(securityUtil).getCurrentUserId();
        verify(contentGenerationRepository).findById(contentId);
        verify(contentVersionRepository).findByContentIdOrderByVersionNumberDesc(contentId, pageable);
        verify(contentVersionMapper, times(2)).toDtoWithMetadata(any(ContentVersion.class), anyBoolean(), eq(3));
    }

    @Test
    void testGetVersionHistoryWithPagination_SecondPage() {
        // Arrange
        Long contentId = 1L;
        int page = 1;
        int size = 2;

        Pageable pageable = PageRequest.of(page, size);
        Page<ContentVersion> versionsPage = new PageImpl<>(
                testVersions.subList(2, 3), // Last version
                pageable,
                testVersions.size());

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(testContent));
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId, pageable))
                .thenReturn(versionsPage);

        when(contentVersionMapper.toDtoWithMetadata(any(ContentVersion.class), anyBoolean(), anyInt()))
                .thenAnswer(invocation -> {
                    ContentVersion version = invocation.getArgument(0);
                    Boolean isLatest = invocation.getArgument(1);
                    Integer totalVersions = invocation.getArgument(2);

                    return ContentVersionDto.builder()
                            .id(version.getId())
                            .contentId(version.getContentId())
                            .versionNumber(version.getVersionNumber())
                            .content(version.getContent())
                            .isLatestVersion(isLatest)
                            .totalVersions(totalVersions)
                            .build();
                });

        // Act
        PaginatedResponse<ContentVersionDto> result = contentVersioningService.getVersionHistory(contentId, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getContent().get(0).getVersionNumber());

        // Check pagination metadata for second page
        PaginatedResponse.PaginationMetadata pagination = result.getPagination();
        assertEquals(1, pagination.getPage());
        assertEquals(2, pagination.getSize());
        assertEquals(3, pagination.getTotalElements());
        assertEquals(2, pagination.getTotalPages());
        assertFalse(pagination.isFirst());
        assertTrue(pagination.isLast());
        assertFalse(pagination.isHasNext());
        assertTrue(pagination.isHasPrevious());
        assertEquals(1, pagination.getNumberOfElements());
    }

    @Test
    void testGetVersionHistoryWithPagination_InvalidPageNumber() {
        // Arrange
        Long contentId = 1L;
        int page = -1;
        int size = 20;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);

        // Act & Assert
        Exception exception = assertThrows(ai.content.auto.exception.BusinessException.class, () -> {
            contentVersioningService.getVersionHistory(contentId, page, size);
        });

        assertEquals("Page number must be non-negative", exception.getMessage());
    }

    @Test
    void testGetVersionHistoryWithPagination_InvalidPageSize() {
        // Arrange
        Long contentId = 1L;
        int page = 0;
        int size = 0;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);

        // Act & Assert
        Exception exception = assertThrows(ai.content.auto.exception.BusinessException.class, () -> {
            contentVersioningService.getVersionHistory(contentId, page, size);
        });

        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @Test
    void testGetVersionHistoryWithPagination_PageSizeTooLarge() {
        // Arrange
        Long contentId = 1L;
        int page = 0;
        int size = 101;

        when(securityUtil.getCurrentUserId()).thenReturn(1L);

        // Act & Assert
        Exception exception = assertThrows(ai.content.auto.exception.BusinessException.class, () -> {
            contentVersioningService.getVersionHistory(contentId, page, size);
        });

        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @Test
    void testGetVersionHistoryWithPagination_EmptyResult() {
        // Arrange
        Long contentId = 1L;
        int page = 0;
        int size = 20;

        Pageable pageable = PageRequest.of(page, size);
        Page<ContentVersion> emptyPage = new PageImpl<>(
                Arrays.asList(),
                pageable,
                0);

        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(contentGenerationRepository.findById(contentId)).thenReturn(Optional.of(testContent));
        when(contentVersionRepository.findByContentIdOrderByVersionNumberDesc(contentId, pageable))
                .thenReturn(emptyPage);

        // Act
        PaginatedResponse<ContentVersionDto> result = contentVersioningService.getVersionHistory(contentId, page, size);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertTrue(result.getContent().isEmpty());

        // Check pagination metadata
        PaginatedResponse.PaginationMetadata pagination = result.getPagination();
        assertEquals(0, pagination.getPage());
        assertEquals(20, pagination.getSize());
        assertEquals(0, pagination.getTotalElements());
        assertEquals(0, pagination.getTotalPages());
        assertTrue(pagination.isFirst());
        assertTrue(pagination.isLast());
        assertFalse(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
        assertEquals(0, pagination.getNumberOfElements());
    }

    private ContentVersion createTestVersion(Long id, Long contentId, Integer versionNumber, String content) {
        return ContentVersion.builder()
                .id(id)
                .contentId(contentId)
                .versionNumber(versionNumber)
                .content(content)
                .title("Test Title " + versionNumber)
                .createdBy(1L)
                .createdAt(Instant.now())
                .build();
    }
}