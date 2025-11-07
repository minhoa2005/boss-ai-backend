package ai.content.auto.service;

import ai.content.auto.dtos.CreateTemplateCategoryRequest;
import ai.content.auto.dtos.CreateTemplateTagRequest;
import ai.content.auto.dtos.TemplateCategoryDto;
import ai.content.auto.dtos.TemplateTagDto;
import ai.content.auto.entity.TemplateCategory;
import ai.content.auto.entity.TemplateTag;
import ai.content.auto.exception.BusinessException;
import ai.content.auto.mapper.TemplateCategoryMapper;
import ai.content.auto.mapper.TemplateTagMapper;
import ai.content.auto.repository.TemplateCategoryRepository;
import ai.content.auto.repository.TemplateTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateCategorizationServiceTest {

    @Mock
    private TemplateCategoryRepository categoryRepository;

    @Mock
    private TemplateTagRepository tagRepository;

    @Mock
    private TemplateCategoryMapper categoryMapper;

    @Mock
    private TemplateTagMapper tagMapper;

    @InjectMocks
    private TemplateCategorizationService categorizationService;

    private TemplateCategory testCategory;
    private TemplateCategoryDto testCategoryDto;
    private TemplateTag testTag;
    private TemplateTagDto testTagDto;

    @BeforeEach
    void setUp() {
        testCategory = new TemplateCategory();
        testCategory.setId(1L);
        testCategory.setName("Marketing");
        testCategory.setSlug("marketing");
        testCategory.setStatus("ACTIVE");

        testCategoryDto = new TemplateCategoryDto();
        testCategoryDto.setId(1L);
        testCategoryDto.setName("Marketing");
        testCategoryDto.setSlug("marketing");
        testCategoryDto.setStatus("ACTIVE");

        testTag = new TemplateTag();
        testTag.setId(1L);
        testTag.setName("professional");
        testTag.setSlug("professional");
        testTag.setStatus("ACTIVE");
        testTag.setUsageCount(5);

        testTagDto = new TemplateTagDto();
        testTagDto.setId(1L);
        testTagDto.setName("professional");
        testTagDto.setSlug("professional");
        testTagDto.setStatus("ACTIVE");
        testTagDto.setUsageCount(5);
    }

    @Test
    void getRootCategories_ShouldReturnCategories() {
        // Given
        List<TemplateCategory> categories = Arrays.asList(testCategory);
        when(categoryRepository.findRootCategories()).thenReturn(categories);
        when(categoryMapper.toDtoList(categories)).thenReturn(Arrays.asList(testCategoryDto));

        // When
        List<TemplateCategoryDto> result = categorizationService.getRootCategories();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Marketing", result.get(0).getName());
        verify(categoryRepository).findRootCategories();
    }

    @Test
    void createCategory_ShouldCreateSuccessfully() {
        // Given
        CreateTemplateCategoryRequest request = new CreateTemplateCategoryRequest();
        request.setName("New Category");
        request.setDescription("Test category");

        when(categoryRepository.findByNameAndStatus(anyString(), anyString())).thenReturn(Optional.empty());
        when(categoryMapper.toEntity(any(), any())).thenReturn(testCategory);
        when(categoryRepository.save(any())).thenReturn(testCategory);
        when(categoryMapper.toDto(any())).thenReturn(testCategoryDto);

        // When
        TemplateCategoryDto result = categorizationService.createCategory(request);

        // Then
        assertNotNull(result);
        assertEquals("Marketing", result.getName());
        verify(categoryRepository).save(any());
    }

    @Test
    void createCategory_ShouldThrowExceptionWhenNameExists() {
        // Given
        CreateTemplateCategoryRequest request = new CreateTemplateCategoryRequest();
        request.setName("Existing Category");

        when(categoryRepository.findByNameAndStatus(anyString(), anyString())).thenReturn(Optional.of(testCategory));

        // When & Then
        assertThrows(BusinessException.class, () -> categorizationService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getAllTags_ShouldReturnTags() {
        // Given
        List<TemplateTag> tags = Arrays.asList(testTag);
        when(tagRepository.findAllActiveOrderByUsage()).thenReturn(tags);
        when(tagMapper.toDtoList(tags)).thenReturn(Arrays.asList(testTagDto));

        // When
        List<TemplateTagDto> result = categorizationService.getAllTags();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("professional", result.get(0).getName());
        verify(tagRepository).findAllActiveOrderByUsage();
    }

    @Test
    void createTag_ShouldCreateSuccessfully() {
        // Given
        CreateTemplateTagRequest request = new CreateTemplateTagRequest();
        request.setName("new-tag");
        request.setDescription("Test tag");

        when(tagRepository.findByNameAndStatus(anyString(), anyString())).thenReturn(Optional.empty());
        when(tagMapper.toEntity(any())).thenReturn(testTag);
        when(tagRepository.save(any())).thenReturn(testTag);
        when(tagMapper.toDto(any())).thenReturn(testTagDto);

        // When
        TemplateTagDto result = categorizationService.createTag(request);

        // Then
        assertNotNull(result);
        assertEquals("professional", result.getName());
        verify(tagRepository).save(any());
    }

    @Test
    void createTag_ShouldThrowExceptionWhenNameExists() {
        // Given
        CreateTemplateTagRequest request = new CreateTemplateTagRequest();
        request.setName("existing-tag");

        when(tagRepository.findByNameAndStatus(anyString(), anyString())).thenReturn(Optional.of(testTag));

        // When & Then
        assertThrows(BusinessException.class, () -> categorizationService.createTag(request));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void getOrCreateTags_ShouldCreateMissingTags() {
        // Given
        List<String> tagNames = Arrays.asList("existing-tag", "new-tag");
        List<TemplateTag> existingTags = Arrays.asList(testTag);

        when(tagRepository.findByNamesAndStatus(tagNames, "ACTIVE")).thenReturn(existingTags);
        when(tagMapper.toEntity(any())).thenReturn(testTag);
        when(tagRepository.save(any())).thenReturn(testTag);
        when(tagMapper.toDtoList(any())).thenReturn(Arrays.asList(testTagDto, testTagDto));

        // When
        List<TemplateTagDto> result = categorizationService.getOrCreateTags(tagNames);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tagRepository).save(any()); // Should save the new tag
    }
}