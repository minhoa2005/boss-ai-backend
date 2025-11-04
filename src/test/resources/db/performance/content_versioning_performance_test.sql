-- Content Versioning Performance Test Script
-- This script tests database performance with 1M+ version records
-- Requirements: Database performance testing with 1M+ version records

-- Enable timing for performance measurement
\timing on

-- Create test data setup
DO $$
DECLARE
    content_count INTEGER := 10000;  -- 10K content items
    versions_per_content INTEGER := 100;  -- 100 versions each = 1M total versions
    batch_size INTEGER := 1000;
    current_batch INTEGER := 0;
    total_batches INTEGER;
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    i INTEGER;
    j INTEGER;
    content_id BIGINT;
    user_id BIGINT := 1; -- Assuming user ID 1 exists
BEGIN
    start_time := clock_timestamp();
    total_batches := (content_count * versions_per_content) / batch_size;
    
    RAISE NOTICE 'Starting performance test data generation...';
    RAISE NOTICE 'Target: % content items with % versions each = % total versions', 
                 content_count, versions_per_content, (content_count * versions_per_content);
    
    -- Create test content generations first
    RAISE NOTICE 'Creating % test content generations...', content_count;
    
    FOR i IN 1..content_count LOOP
        INSERT INTO content_generations (
            user_id, content_type, status, ai_provider, ai_model,
            prompt, generated_content, title, word_count, character_count,
            tokens_used, generation_cost, processing_time_ms,
            quality_score, readability_score, sentiment_score,
            industry, target_audience, tone, language,
            retry_count, max_retries, is_billable,
            created_at, updated_at, version, current_version
        ) VALUES (
            user_id, 'article', 'completed', 'openai', 'gpt-4',
            'Test prompt for content ' || i,
            'Generated content for testing performance with content ID ' || i || '. This is a longer text to simulate real content generation.',
            'Test Content Title ' || i,
            150 + (i % 500), -- Varying word counts
            800 + (i % 2500), -- Varying character counts
            200 + (i % 300), -- Varying token usage
            0.05 + (i % 100) * 0.001, -- Varying costs
            1000 + (i % 5000), -- Varying processing times
            70.0 + (i % 30), -- Quality scores 70-100
            75.0 + (i % 25), -- Readability scores 75-100
            -0.5 + (i % 100) * 0.01, -- Sentiment scores -0.5 to 0.5
            CASE (i % 5) 
                WHEN 0 THEN 'technology'
                WHEN 1 THEN 'healthcare'
                WHEN 2 THEN 'finance'
                WHEN 3 THEN 'education'
                ELSE 'marketing'
            END,
            CASE (i % 3)
                WHEN 0 THEN 'general audience'
                WHEN 1 THEN 'professionals'
                ELSE 'students'
            END,
            CASE (i % 4)
                WHEN 0 THEN 'formal'
                WHEN 1 THEN 'casual'
                WHEN 2 THEN 'friendly'
                ELSE 'professional'
            END,
            'vi',
            0, 3, true,
            NOW() - INTERVAL '1 day' * (i % 365), -- Spread over last year
            NOW() - INTERVAL '1 day' * (i % 365),
            0, 1
        );
        
        -- Progress indicator for content creation
        IF i % 1000 = 0 THEN
            RAISE NOTICE 'Created % content generations...', i;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Content generations created. Now creating versions...';
    
    -- Create versions in batches for better performance
    FOR i IN 1..content_count LOOP
        -- Get the content ID
        SELECT id INTO content_id FROM content_generations 
        WHERE user_id = user_id 
        ORDER BY id 
        OFFSET (i-1) LIMIT 1;
        
        -- Create versions for this content
        FOR j IN 1..versions_per_content LOOP
            INSERT INTO content_versions (
                content_id, version_number, content, title,
                generation_params, ai_provider, ai_model,
                tokens_used, generation_cost, processing_time_ms,
                readability_score, seo_score, quality_score, sentiment_score,
                word_count, character_count,
                industry, target_audience, tone, language,
                created_by, created_at, updated_at
            ) VALUES (
                content_id, j,
                'Version ' || j || ' content for testing performance. This is version ' || j || ' of content ID ' || content_id || '. ' ||
                'This content is generated for performance testing purposes and contains enough text to simulate real content versions.',
                'Version ' || j || ' - Test Content Title',
                ('{"temperature": ' || (0.1 + (j % 10) * 0.1) || ', "max_tokens": ' || (100 + j * 10) || ', "model": "gpt-4"}')::jsonb,
                CASE (j % 3)
                    WHEN 0 THEN 'openai'
                    WHEN 1 THEN 'claude'
                    ELSE 'gemini'
                END,
                CASE (j % 3)
                    WHEN 0 THEN 'gpt-4'
                    WHEN 1 THEN 'claude-3'
                    ELSE 'gemini-pro'
                END,
                180 + (j % 200), -- Varying token usage
                0.04 + (j % 50) * 0.001, -- Varying costs
                900 + (j % 3000), -- Varying processing times
                70.0 + (j % 30), -- Readability scores
                65.0 + (j % 35), -- SEO scores
                75.0 + (j % 25), -- Quality scores
                -0.3 + (j % 60) * 0.01, -- Sentiment scores
                140 + (j % 400), -- Word counts
                750 + (j % 2000), -- Character counts
                CASE (j % 5) 
                    WHEN 0 THEN 'technology'
                    WHEN 1 THEN 'healthcare'
                    WHEN 2 THEN 'finance'
                    WHEN 3 THEN 'education'
                    ELSE 'marketing'
                END,
                CASE (j % 3)
                    WHEN 0 THEN 'general audience'
                    WHEN 1 THEN 'professionals'
                    ELSE 'students'
                END,
                CASE (j % 4)
                    WHEN 0 THEN 'formal'
                    WHEN 1 THEN 'casual'
                    WHEN 2 THEN 'friendly'
                    ELSE 'professional'
                END,
                'vi',
                user_id,
                NOW() - INTERVAL '1 day' * (j % 100), -- Spread versions over time
                NOW() - INTERVAL '1 day' * (j % 100)
            );
            
            current_batch := current_batch + 1;
            
            -- Commit in batches and show progress
            IF current_batch % batch_size = 0 THEN
                COMMIT;
                RAISE NOTICE 'Created % versions (%.1f%% complete)', 
                           current_batch, 
                           (current_batch::FLOAT / (content_count * versions_per_content) * 100);
            END IF;
        END LOOP;
    END LOOP;
    
    end_time := clock_timestamp();
    RAISE NOTICE 'Test data generation completed in %', (end_time - start_time);
    RAISE NOTICE 'Total versions created: %', (SELECT COUNT(*) FROM content_versions);
END $$;

-- Performance Test Queries
RAISE NOTICE 'Starting performance tests...';

-- Test 1: Index performance on content_id
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_versions WHERE content_id = 5000;

-- Test 2: Index performance on version lookup
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_versions WHERE content_id = 5000 AND version_number = 50;

-- Test 3: Index performance on created_at range
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_versions 
WHERE created_at >= NOW() - INTERVAL '30 days' 
ORDER BY created_at DESC 
LIMIT 100;

-- Test 4: Index performance on quality score
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_versions 
WHERE quality_score >= 90.0 
ORDER BY quality_score DESC 
LIMIT 100;

-- Test 5: Complex query with multiple conditions
EXPLAIN (ANALYZE, BUFFERS) 
SELECT cv.*, cg.title as content_title
FROM content_versions cv
JOIN content_generations cg ON cv.content_id = cg.id
WHERE cv.ai_provider = 'openai'
  AND cv.quality_score >= 85.0
  AND cv.created_at >= NOW() - INTERVAL '7 days'
ORDER BY cv.quality_score DESC, cv.created_at DESC
LIMIT 50;

-- Test 6: Aggregation performance
EXPLAIN (ANALYZE, BUFFERS) 
SELECT 
    content_id,
    COUNT(*) as version_count,
    AVG(quality_score) as avg_quality,
    MAX(quality_score) as max_quality,
    MIN(created_at) as first_version,
    MAX(created_at) as latest_version
FROM content_versions 
GROUP BY content_id 
HAVING COUNT(*) > 50
ORDER BY avg_quality DESC
LIMIT 100;

-- Test 7: JSONB query performance
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_versions 
WHERE generation_params->>'temperature' = '0.7'
LIMIT 100;

-- Test 8: View performance
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM v_content_version_history 
WHERE quality_rank <= 5 
LIMIT 100;

-- Performance Statistics
SELECT 
    'content_versions' as table_name,
    COUNT(*) as total_records,
    pg_size_pretty(pg_total_relation_size('content_versions')) as table_size,
    pg_size_pretty(pg_relation_size('content_versions')) as data_size,
    pg_size_pretty(pg_total_relation_size('content_versions') - pg_relation_size('content_versions')) as index_size
FROM content_versions

UNION ALL

SELECT 
    'content_generations' as table_name,
    COUNT(*) as total_records,
    pg_size_pretty(pg_total_relation_size('content_generations')) as table_size,
    pg_size_pretty(pg_relation_size('content_generations')) as data_size,
    pg_size_pretty(pg_total_relation_size('content_generations') - pg_relation_size('content_generations')) as index_size
FROM content_generations;

-- Index usage statistics
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes 
WHERE tablename IN ('content_versions', 'content_version_comparisons')
ORDER BY idx_scan DESC;

-- Create some sample comparisons for testing
INSERT INTO content_version_comparisons (
    content_id, version_a, version_b,
    user_preference, performance_winner,
    version_a_engagement_rate, version_b_engagement_rate,
    version_a_conversion_rate, version_b_conversion_rate,
    statistical_significance, confidence_level,
    sample_size_a, sample_size_b,
    test_status, test_start_date,
    compared_by, compared_at, created_at
)
SELECT 
    cg.id as content_id,
    1 as version_a,
    2 as version_b,
    CASE (cg.id % 3) 
        WHEN 0 THEN 'A'::text
        WHEN 1 THEN 'B'::text
        ELSE 'NONE'::text
    END::ai.content.auto.entity.ContentVersionComparison.UserPreference,
    CASE (cg.id % 3)
        WHEN 0 THEN 'A'::text
        WHEN 1 THEN 'B'::text
        ELSE 'TIE'::text
    END::ai.content.auto.entity.ContentVersionComparison.PerformanceWinner,
    0.15 + (cg.id % 100) * 0.001, -- Version A engagement
    0.18 + (cg.id % 100) * 0.001, -- Version B engagement
    0.05 + (cg.id % 50) * 0.001,  -- Version A conversion
    0.07 + (cg.id % 50) * 0.001,  -- Version B conversion
    0.85 + (cg.id % 15) * 0.01,   -- Statistical significance
    0.95, -- Confidence level
    1000 + (cg.id % 500), -- Sample size A
    1000 + (cg.id % 500), -- Sample size B
    'COMPLETED'::ai.content.auto.entity.ContentVersionComparison.TestStatus,
    NOW() - INTERVAL '7 days',
    1, -- compared_by user_id
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
FROM content_generations cg
WHERE cg.id <= 1000; -- Create 1000 sample comparisons

-- Test comparison queries
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM content_version_comparisons 
WHERE content_id = 500;

EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM v_content_comparison_analytics 
WHERE statistical_significance >= 0.95 
LIMIT 100;

RAISE NOTICE 'Performance testing completed!';
RAISE NOTICE 'Check the EXPLAIN ANALYZE results above for performance metrics.';
RAISE NOTICE 'All queries should complete in under 100ms for optimal performance.';