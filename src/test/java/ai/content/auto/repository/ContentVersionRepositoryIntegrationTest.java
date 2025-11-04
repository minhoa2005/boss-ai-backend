package ai.content.auto.repository;

import ai.content.auto.entity.ContentVersion;
import ai.content.auto.entity.ContentGeneration;
import ai.content.auto.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ContentVersionRepository.
 * Tests database operations and query performance.
 */
@DataJpaTest
@ActiveProfiles("test")
class ContentVersionRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContentVersionRepository contentVersionRepository;

    @Autowired
    private ContentGenerationRepository contentGenerationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private ContentGeneration testContent;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setCreatedAt(Instant.now());
        testUser = entityManager.persistAndFlush(testUser);

        // Create test content generation
        testContent = new ContentGeneration();
        testContent.setUser(testUser);
        testContent.setContentType("article");
        testContent.setStatus("completed");
        testContent.setAiProvider("openai");
        testContent.setAiModel("gpt-4");
        testContent.setPrompt("Test prompt");
        testContent.setGeneratedContent("Test generated content");
        testContent.setTitle("Test Content");
        testContent.setWordCount(100);
        testContent.setCharacterCount(500);
        testContent.setTokensUsed(150);
        testContent.setGenerationCost(BigDecimal.valueOf(0.05));
        testContent.setProcessingTimeMs(1500L);
        testContent.setQualityScore(BigDecimal.valueOf(85.5));
        testContent.setReadabilityScore(BigDecimal.valueOf(78.2));
        testContent.setSentimentScore(BigDecimal.valueOf(0.3));
        testContent.setIndustry("technology");
        testContent.setTargetAudience("developers");
        testContent.setTone("professional");
        testContent.setLanguage("vi");
        testContent.setRetryCount(0);
        testContent.setMaxRetries(3);
        testContent.setIsBillable(true);
        testContent.setCreatedAt(Instant.now());
        testContent.setUpdatedAt(Instant.now());
        testContent.setVersion(0L);
        testContent.setCurrentVersion(1);
        testContent = entityManager.persistAndFlush(testContent);
    }

    @Test
    void testSaveAndFindContentVersion() {
        // Create test version
        Map<String, Object> generationParams = new HashMap<>();
        generationParams.put("temperature", 0.7);
        generationParams.put("max_tokens", 1000);

        ContentVersion version = ContentVersion.builder()
                .contentId(testContent.getId())
                .versionNumber(1)
                .content("Version 1 content")
                .title("Version 1 Title")
                .generationParams(generationParams)
                .aiProvider("openai")
                .aiModel("gpt-4")
                .tokensUsed(150)
                .generationCost(BigDecimal.valueOf(0.05))
                .processingTimeMs(1500L)
                .readabilityScore(BigDecimal.valueOf(85.5))
                .seoScore(BigDecimal.valueOf(78.2))
                .qualityScore(BigDecimal.valueOf(92.1))
                .sentimentScore(BigDecimal.valueOf(0.3))
                .wordCount(25)
                .characterCount(150)
                .industry("technology")
                .targetAudience("developers")
                .tone("professional")
                .language("vi")
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();

        // Save version
        ContentVersion savedVersion = contentVersionRepository.save(version);
        entityManager.flush();

        // Verify save
        assertNotNull(savedVersion.getId());
        assertEquals(1, savedVersion.getVersionNumber());
        assertEquals("Version 1 content", savedVersion.getContent());
        assertEquals("openai", savedVersion.getAiProvider());

        // Test find by ID
        Optional<ContentVersion> foundVersion = contentVersionRepository.findById(savedVersion.getId());
        assertTrue(foundVersion.isPresent());
        assertEquals(savedVersion.getId(), foundVersion.get().getId());
    }

    @Test
    void testFindByContentIdOrderByVersionNumberDesc() {
        // Create multiple versions
        for (int i = 1; i <= 3; i++) {
            ContentVersion version = ContentVersion.builder()
                    .contentId(testContent.getId())
                    .versionNumber(i)
                    .content("Version " + i + " content")
                    .title("Version " + i + " Title")
                    .generationParams(new HashMap<>())
                    .aiProvider("openai")
                    .aiModel("gpt-4")
                    .createdBy(testUser.getId())
                    .createdAt(Instant.now())
                    .build();
            contentVersionRepository.save(version);
        }
        entityManager.flush();

        // Test query
        List<ContentVersion> versions = contentVersionRepository
                .findByContentIdOrderByVersionNumberDesc(testContent.getId());

        assertEquals(3, versions.size());
        assertEquals(3, versions.get(0).getVersionNumber()); // Descending order
        assertEquals(2, versions.get(1).getVersionNumber());
        assertEquals(1, versions.get(2).getVersionNumber());
    }

    @Test
    void testFindByContentIdAndVersionNumber() {
        // Create test version
        ContentVersion version = ContentVersion.builder()
                .contentId(testContent.getId())
                .versionNumber(2)
                .content("Version 2 content")
                .title("Version 2 Title")
                .generationParams(new HashMap<>())
                .aiProvider("openai")
                .aiModel("gpt-4")
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();
        contentVersionRepository.save(version);
        entityManager.flush();

        // Test query
        Optional<ContentVersion> foundVersion = contentVersionRepository
                .findByContentIdAndVersionNumber(testContent.getId(), 2);

        assertTrue(foundVersion.isPresent());
        assertEquals(2, foundVersion.get().getVersionNumber());
        assertEquals("Version 2 content", foundVersion.get().getContent());
    }

    @Test
    void testGetNextVersionNumber() {
        // Create existing versions
        for (int i = 1; i <= 3; i++) {
            ContentVersion version = ContentVersion.builder()
                    .contentId(testContent.getId())
                    .versionNumber(i)
                    .content("Version " + i + " content")
                    .generationParams(new HashMap<>())
                    .aiProvider("openai")
                    .aiModel("gpt-4")
                    .createdBy(testUser.getId())
                    .createdAt(Instant.now())
                    .build();
            contentVersionRepository.save(version);
        }
        entityManager.flush();

        // Test next version number
        Integer nextVersion = contentVersionRepository.getNextVersionNumber(testContent.getId());
        assertEquals(4, nextVersion);

        // Test for content with no versions
        ContentGeneration newContent = new ContentGeneration();
        newContent.setUser(testUser);
        newContent.setContentType("article");
        newContent.setStatus("completed");
        newContent.setAiProvider("openai");
        newContent.setRetryCount(0);
        newContent.setMaxRetries(3);
        newContent.setIsBillable(true);
        newContent.setCreatedAt(Instant.now());
        newContent.setUpdatedAt(Instant.now());
        newContent.setVersion(0L);
        newContent.setCurrentVersion(1);
        newContent = entityManager.persistAndFlush(newContent);

        Integer firstVersion = contentVersionRepository.getNextVersionNumber(newContent.getId());
        assertEquals(1, firstVersion);
    }

    @Test
    void testFindLatestVersionByContentId() {
        // Create multiple versions with different timestamps
        for (int i = 1; i <= 3; i++) {
            ContentVersion version = ContentVersion.builder()
                    .contentId(testContent.getId())
                    .versionNumber(i)
                    .content("Version " + i + " content")
                    .generationParams(new HashMap<>())
                    .aiProvider("openai")
                    .aiModel("gpt-4")
                    .createdBy(testUser.getId())
                    .createdAt(Instant.now().minusSeconds(10 - i)) // Latest has highest version number
                    .build();
            contentVersionRepository.save(version);
        }
        entityManager.flush();

        // Test query
        Optional<ContentVersion> latestVersion = contentVersionRepository
                .findLatestVersionByContentId(testContent.getId());

        assertTrue(latestVersion.isPresent());
        assertEquals(3, latestVersion.get().getVersionNumber());
        assertEquals("Version 3 content", latestVersion.get().getContent());
    }

    @Test
    void testCountByContentId() {
        // Create multiple versions
        for (int i = 1; i <= 5; i++) {
            ContentVersion version = ContentVersion.builder()
                    .contentId(testContent.getId())
                    .versionNumber(i)
                    .content("Version " + i + " content")
                    .generationParams(new HashMap<>())
                    .aiProvider("openai")
                    .aiModel("gpt-4")
                    .createdBy(testUser.getId())
                    .createdAt(Instant.now())
                    .build();
            contentVersionRepository.save(version);
        }
        entityManager.flush();

        // Test count
        long count = contentVersionRepository.countByContentId(testContent.getId());
        assertEquals(5, count);
    }

    @Test
    void testExistsByContentIdAndVersionNumber() {
        // Create test version
        ContentVersion version = ContentVersion.builder()
                .contentId(testContent.getId())
                .versionNumber(1)
                .content("Version 1 content")
                .generationParams(new HashMap<>())
                .aiProvider("openai")
                .aiModel("gpt-4")
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();
        contentVersionRepository.save(version);
        entityManager.flush();

        // Test existence
        assertTrue(contentVersionRepository.existsByContentIdAndVersionNumber(testContent.getId(), 1));
        assertFalse(contentVersionRepository.existsByContentIdAndVersionNumber(testContent.getId(), 2));
    }

    @Test
    void testJsonbGenerationParams() {
        // Create version with complex JSONB data
        Map<String, Object> complexParams = new HashMap<>();
        complexParams.put("temperature", 0.7);
        complexParams.put("max_tokens", 1000);
        complexParams.put("top_p", 0.9);
        complexParams.put("frequency_penalty", 0.1);
        complexParams.put("presence_penalty", 0.2);

        Map<String, Object> nestedParams = new HashMap<>();
        nestedParams.put("style", "creative");
        nestedParams.put("format", "markdown");
        complexParams.put("custom_settings", nestedParams);

        ContentVersion version = ContentVersion.builder()
                .contentId(testContent.getId())
                .versionNumber(1)
                .content("Version with complex params")
                .generationParams(complexParams)
                .aiProvider("openai")
                .aiModel("gpt-4")
                .createdBy(testUser.getId())
                .createdAt(Instant.now())
                .build();

        ContentVersion savedVersion = contentVersionRepository.save(version);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context

        // Retrieve and verify JSONB data
        Optional<ContentVersion> retrievedVersion = contentVersionRepository.findById(savedVersion.getId());
        assertTrue(retrievedVersion.isPresent());

        Map<String, Object> retrievedParams = retrievedVersion.get().getGenerationParams();
        assertNotNull(retrievedParams);
        assertEquals(0.7, retrievedParams.get("temperature"));
        assertEquals(1000, retrievedParams.get("max_tokens"));

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedNested = (Map<String, Object>) retrievedParams.get("custom_settings");
        assertNotNull(retrievedNested);
        assertEquals("creative", retrievedNested.get("style"));
        assertEquals("markdown", retrievedNested.get("format"));
    }
}