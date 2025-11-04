# Content Versioning Database Schema

## Overview

This directory contains database migration scripts for the Content Versioning System, implementing Requirements 1.1, 1.2, and 1.3 from the Business Enhancement Phase 1 specification.

## Migration Files

### V001__create_content_versioning_schema.sql ✅ **COMPLETED**
- **Purpose**: Creates the complete content versioning database schema
- **Tables Created**:
  - `content_versions`: Stores all versions of generated content with full history and metrics
  - `content_version_comparisons`: Stores A/B test comparisons between different content versions
- **JPA Entity**: `ContentVersion.java` - Full JPA entity with validation and business logic
- **Features**:
  - Comprehensive indexing for optimal query performance
  - JSONB fields for flexible parameter and metrics storage
  - Database triggers for automatic timestamp updates
  - Helper functions for version management
  - Database views for easy data access
  - Data integrity constraints and validation
  - JPA entity with performance calculation methods

### V001__create_content_versioning_schema_rollback.sql
- **Purpose**: Rollback script to safely remove all content versioning objects
- **Usage**: For development rollbacks or emergency schema removal
- **Safety**: Includes verification queries to ensure clean rollback

## JPA Entity Implementation ✅ **NEW**

### ContentVersion Entity
The `ContentVersion` JPA entity (`ai.content.auto.entity.ContentVersion`) provides:

#### Key Features
- **Comprehensive Validation**: Bean validation annotations for all fields
- **JSONB Support**: Native PostgreSQL JSONB mapping for generation parameters
- **Automatic Timestamps**: `@PrePersist` and `@PreUpdate` lifecycle callbacks
- **Performance Calculations**: Built-in methods for score calculation and comparison
- **Lazy Loading**: Optimized foreign key relationships with lazy fetching

#### Business Logic Methods
```java
// Calculate weighted overall performance score
public BigDecimal calculateOverallScore()

// Compare performance between versions
public boolean performsBetterThan(ContentVersion other)
```

#### Validation Rules
- Version numbers must be positive (`@Min(1)`)
- Scores must be within valid ranges (0-100 for quality metrics, -1 to 1 for sentiment)
- Content cannot be empty (database constraint + validation)
- AI provider is required and limited to 50 characters
- Generation parameters stored as JSONB with automatic serialization

#### Entity Relationships
- **ContentGeneration**: Many-to-one relationship with cascade delete
- **User**: Many-to-one relationship for audit trail (restrict delete)
- **Lazy Loading**: All relationships use `FetchType.LAZY` for performance

#### Usage Example
```java
ContentVersion version = ContentVersion.builder()
    .contentId(123L)
    .versionNumber(1)
    .content("Generated content text")
    .title("Content Title")
    .aiProvider("openai")
    .aiModel("gpt-4")
    .generationParams(Map.of("temperature", 0.7))
    .qualityScore(new BigDecimal("85.5"))
    .readabilityScore(new BigDecimal("78.2"))
    .seoScore(new BigDecimal("92.1"))
    .createdBy(userId)
    .build();

// Calculate overall performance
BigDecimal overallScore = version.calculateOverallScore();

// Compare with another version
boolean isBetter = version.performsBetterThan(otherVersion);
```

## Database Schema Details

### content_versions Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| content_id | BIGINT | Foreign key to content_generations |
| version_number | INTEGER | Version number (1, 2, 3, ...) |
| content | TEXT | The actual generated content |
| title | VARCHAR(500) | Content title |
| generation_params | JSONB | Generation parameters as JSON |
| ai_provider | VARCHAR(50) | AI provider used (openai, claude, gemini) |
| ai_model | VARCHAR(100) | Specific AI model used |
| tokens_used | INTEGER | Number of tokens consumed |
| generation_cost | DECIMAL(10,6) | Cost of generation |
| processing_time_ms | BIGINT | Processing time in milliseconds |
| readability_score | DECIMAL(5,2) | Readability score (0-100) |
| seo_score | DECIMAL(5,2) | SEO score (0-100) |
| quality_score | DECIMAL(5,2) | Overall quality score (0-100) |
| sentiment_score | DECIMAL(5,2) | Sentiment score (-1 to 1) |
| word_count | INTEGER | Number of words |
| character_count | INTEGER | Number of characters |
| industry | VARCHAR(100) | Target industry |
| target_audience | VARCHAR(200) | Target audience |
| tone | VARCHAR(50) | Content tone |
| language | VARCHAR(10) | Content language |
| created_by | BIGINT | User who created the version |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### content_version_comparisons Table

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| content_id | BIGINT | Foreign key to content_generations |
| version_a | INTEGER | First version number |
| version_b | INTEGER | Second version number |
| user_preference | VARCHAR(10) | User preference (A, B, NONE) |
| performance_winner | VARCHAR(10) | Performance winner (A, B, TIE) |
| comparison_notes | TEXT | Notes about the comparison |
| metrics_comparison | JSONB | Detailed comparison metrics |
| version_a_engagement_rate | DECIMAL(5,4) | Version A engagement rate |
| version_b_engagement_rate | DECIMAL(5,4) | Version B engagement rate |
| version_a_conversion_rate | DECIMAL(5,4) | Version A conversion rate |
| version_b_conversion_rate | DECIMAL(5,4) | Version B conversion rate |
| version_a_click_through_rate | DECIMAL(5,4) | Version A CTR |
| version_b_click_through_rate | DECIMAL(5,4) | Version B CTR |
| statistical_significance | DECIMAL(5,4) | Statistical significance |
| confidence_level | DECIMAL(5,4) | Confidence level (default 0.95) |
| sample_size_a | INTEGER | Sample size for version A |
| sample_size_b | INTEGER | Sample size for version B |
| test_start_date | TIMESTAMP | Test start date |
| test_end_date | TIMESTAMP | Test end date |
| test_status | VARCHAR(20) | Test status (ACTIVE, COMPLETED, PAUSED, CANCELLED) |
| compared_by | BIGINT | User who created the comparison |
| compared_at | TIMESTAMP | Comparison creation timestamp |
| created_at | TIMESTAMP | Record creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

## Indexes

### content_versions Indexes
- `idx_content_versions_content_id`: Fast lookup by content ID
- `idx_content_versions_version_number`: Fast lookup by version number
- `idx_content_versions_created_at`: Fast lookup by creation date
- `idx_content_versions_created_by`: Fast lookup by creator
- `idx_content_versions_ai_provider`: Fast lookup by AI provider
- `idx_content_versions_quality_score`: Fast lookup by quality score
- `idx_content_versions_content_created`: Composite index for content + date queries
- `idx_content_versions_user_created`: Composite index for user + date queries
- `idx_content_versions_generation_params`: GIN index for JSONB queries

### content_version_comparisons Indexes
- `idx_content_version_comparisons_content_id`: Fast lookup by content ID
- `idx_content_version_comparisons_compared_by`: Fast lookup by creator
- `idx_content_version_comparisons_compared_at`: Fast lookup by comparison date
- `idx_content_version_comparisons_test_status`: Fast lookup by test status
- `idx_content_version_comparisons_performance_winner`: Fast lookup by winner
- `idx_content_version_comparisons_content_status`: Composite index
- `idx_content_version_comparisons_user_date`: Composite index
- `idx_content_version_comparisons_metrics`: GIN index for JSONB queries

## Database Functions

### get_next_version_number(p_content_id BIGINT)
- **Purpose**: Returns the next version number for a content item
- **Usage**: `SELECT get_next_version_number(123);`
- **Returns**: INTEGER (next version number)

### validate_version_exists(p_content_id BIGINT, p_version_number INTEGER)
- **Purpose**: Validates that a version exists for comparison
- **Usage**: Internal validation function
- **Returns**: BOOLEAN

### update_updated_at_column()
- **Purpose**: Trigger function to automatically update updated_at timestamps
- **Usage**: Automatically called on UPDATE operations

### validate_comparison_versions()
- **Purpose**: Validates that both versions exist before creating comparisons
- **Usage**: Automatically called on INSERT/UPDATE of comparisons
- **Behavior**: Raises exception if versions don't exist

## Database Views

### v_content_version_history
- **Purpose**: Comprehensive view of content version history with user information
- **Features**:
  - Joins with users and content_generations tables
  - Calculates quality ranking within each content
  - Identifies latest version for each content
  - Optimized for common queries

### v_content_comparison_analytics
- **Purpose**: Analytics view for A/B test comparisons
- **Features**:
  - Calculates test duration in days
  - Computes improvement percentages
  - Includes user information
  - Optimized for reporting queries

## Performance Considerations

### Query Performance
- All common query patterns are indexed
- JSONB fields use GIN indexes for fast JSON queries
- Composite indexes support multi-column queries
- Views pre-join frequently accessed data

### Storage Optimization
- Appropriate data types minimize storage overhead
- JSONB provides flexible storage without schema changes
- Indexes are selective to avoid unnecessary overhead

### Scalability
- Schema designed to handle 1M+ version records
- Partitioning can be added later if needed
- Archival strategies can be implemented for old data

## Testing

### Performance Testing
- `content_versioning_performance_test.sql` creates 1M+ test records
- Tests all major query patterns with EXPLAIN ANALYZE
- Validates index usage and query performance
- Includes realistic data distribution

### Integration Testing
- JPA entity tests verify ORM mapping
- Repository tests validate query methods
- JSONB functionality is thoroughly tested
- Foreign key constraints are validated

## Usage Examples

### Creating a New Version
```sql
INSERT INTO content_versions (
    content_id, version_number, content, title,
    generation_params, ai_provider, ai_model,
    created_by, created_at
) VALUES (
    123, 
    (SELECT get_next_version_number(123)),
    'New version content',
    'New Version Title',
    '{"temperature": 0.7, "max_tokens": 1000}'::jsonb,
    'openai',
    'gpt-4',
    1,
    CURRENT_TIMESTAMP
);
```

### Finding Version History
```sql
SELECT * FROM v_content_version_history 
WHERE content_id = 123 
ORDER BY version_number DESC;
```

### Creating A/B Test Comparison
```sql
INSERT INTO content_version_comparisons (
    content_id, version_a, version_b,
    test_status, compared_by, compared_at
) VALUES (
    123, 1, 2,
    'ACTIVE', 1, CURRENT_TIMESTAMP
);
```

### Analytics Query
```sql
SELECT * FROM v_content_comparison_analytics
WHERE statistical_significance >= 0.95
ORDER BY engagement_improvement_percent DESC;
```

## Migration Instructions

### Development Environment
1. Ensure PostgreSQL 12+ is running
2. Run migration: `./gradlew flywayMigrate`
3. Verify with: `./gradlew flywayInfo`

### Production Deployment
1. Backup database before migration
2. Test migration on staging environment
3. Run migration during maintenance window
4. Verify all indexes are created
5. Run performance validation queries

### Rollback Procedure
1. Stop application
2. Run rollback script: `V001__create_content_versioning_schema_rollback.sql`
3. Verify rollback with verification queries
4. Restart application with previous version

## Monitoring

### Key Metrics to Monitor
- Query response times (should be <100ms for 95% of queries)
- Index usage statistics
- Table sizes and growth rates
- JSONB query performance

### Maintenance Tasks
- Regular VACUUM and ANALYZE operations
- Monitor index bloat and rebuild if necessary
- Archive old comparison data based on retention policy
- Update table statistics for query optimization

## Security Considerations

- All foreign key constraints prevent orphaned records
- Row-level security can be added for multi-tenant scenarios
- Audit logging is built into the schema
- Sensitive data should not be stored in JSONB fields