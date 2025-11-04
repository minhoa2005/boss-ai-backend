-- Content Versioning Schema Migration
-- This migration creates the content versioning tables for Business Enhancement Phase 1
-- Requirements: 1.1, 1.2, 1.3

-- Create content_versions table with proper indexing
CREATE TABLE content_versions (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    title VARCHAR(500),
    
    -- Generation parameters stored as JSONB for flexibility
    generation_params JSONB NOT NULL DEFAULT '{}',
    ai_provider VARCHAR(50) NOT NULL,
    ai_model VARCHAR(100),
    
    -- Metrics and performance data
    tokens_used INTEGER,
    generation_cost DECIMAL(10,6),
    processing_time_ms BIGINT,
    
    -- Quality metrics
    readability_score DECIMAL(5,2),
    seo_score DECIMAL(5,2),
    quality_score DECIMAL(5,2),
    sentiment_score DECIMAL(5,2),
    
    -- Word and character counts
    word_count INTEGER,
    character_count INTEGER,
    
    -- Additional metadata
    industry VARCHAR(100),
    target_audience VARCHAR(200),
    tone VARCHAR(50),
    language VARCHAR(10) DEFAULT 'vi',
    
    -- Audit fields
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_content_versions_content_id 
        FOREIGN KEY (content_id) REFERENCES content_generations(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_versions_created_by 
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Unique constraint to prevent duplicate versions
    CONSTRAINT uk_content_versions_content_version 
        UNIQUE(content_id, version_number),
    
    -- Check constraints for data integrity
    CONSTRAINT chk_content_versions_version_number_positive 
        CHECK (version_number > 0),
    CONSTRAINT chk_content_versions_content_not_empty 
        CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_content_versions_scores_range 
        CHECK (
            (readability_score IS NULL OR (readability_score >= 0 AND readability_score <= 100)) AND
            (seo_score IS NULL OR (seo_score >= 0 AND seo_score <= 100)) AND
            (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 100)) AND
            (sentiment_score IS NULL OR (sentiment_score >= -1 AND sentiment_score <= 1))
        )
);

-- Create indexes for optimal performance
CREATE INDEX idx_content_versions_content_id ON content_versions(content_id);
CREATE INDEX idx_content_versions_version_number ON content_versions(version_number);
CREATE INDEX idx_content_versions_created_at ON content_versions(created_at);
CREATE INDEX idx_content_versions_created_by ON content_versions(created_by);
CREATE INDEX idx_content_versions_ai_provider ON content_versions(ai_provider);
CREATE INDEX idx_content_versions_quality_score ON content_versions(quality_score) WHERE quality_score IS NOT NULL;

-- Composite index for common queries
CREATE INDEX idx_content_versions_content_created ON content_versions(content_id, created_at DESC);
CREATE INDEX idx_content_versions_user_created ON content_versions(created_by, created_at DESC);

-- JSONB index for generation parameters
CREATE INDEX idx_content_versions_generation_params ON content_versions USING GIN(generation_params);

-- Create content_version_comparisons table for A/B testing
CREATE TABLE content_version_comparisons (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    version_a INTEGER NOT NULL,
    version_b INTEGER NOT NULL,
    
    -- Comparison results
    user_preference VARCHAR(10) CHECK (user_preference IN ('A', 'B', 'NONE')),
    performance_winner VARCHAR(10) CHECK (performance_winner IN ('A', 'B', 'TIE')),
    comparison_notes TEXT,
    
    -- Metrics comparison stored as JSONB for flexibility
    metrics_comparison JSONB DEFAULT '{}',
    
    -- Performance metrics for A/B testing
    version_a_engagement_rate DECIMAL(5,4),
    version_b_engagement_rate DECIMAL(5,4),
    version_a_conversion_rate DECIMAL(5,4),
    version_b_conversion_rate DECIMAL(5,4),
    version_a_click_through_rate DECIMAL(5,4),
    version_b_click_through_rate DECIMAL(5,4),
    
    -- Statistical significance
    statistical_significance DECIMAL(5,4),
    confidence_level DECIMAL(5,4) DEFAULT 0.95,
    sample_size_a INTEGER,
    sample_size_b INTEGER,
    
    -- Test duration and status
    test_start_date TIMESTAMP WITH TIME ZONE,
    test_end_date TIMESTAMP WITH TIME ZONE,
    test_status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (test_status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELLED')),
    
    -- Audit fields
    compared_by BIGINT NOT NULL,
    compared_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_content_version_comparisons_content_id 
        FOREIGN KEY (content_id) REFERENCES content_generations(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_version_comparisons_compared_by 
        FOREIGN KEY (compared_by) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Ensure versions exist and are different
    CONSTRAINT chk_content_version_comparisons_different_versions 
        CHECK (version_a != version_b),
    CONSTRAINT chk_content_version_comparisons_positive_versions 
        CHECK (version_a > 0 AND version_b > 0),
    
    -- Ensure rates are valid percentages
    CONSTRAINT chk_content_version_comparisons_rates_valid 
        CHECK (
            (version_a_engagement_rate IS NULL OR (version_a_engagement_rate >= 0 AND version_a_engagement_rate <= 1)) AND
            (version_b_engagement_rate IS NULL OR (version_b_engagement_rate >= 0 AND version_b_engagement_rate <= 1)) AND
            (version_a_conversion_rate IS NULL OR (version_a_conversion_rate >= 0 AND version_a_conversion_rate <= 1)) AND
            (version_b_conversion_rate IS NULL OR (version_b_conversion_rate >= 0 AND version_b_conversion_rate <= 1)) AND
            (version_a_click_through_rate IS NULL OR (version_a_click_through_rate >= 0 AND version_a_click_through_rate <= 1)) AND
            (version_b_click_through_rate IS NULL OR (version_b_click_through_rate >= 0 AND version_b_click_through_rate <= 1))
        ),
    
    -- Ensure test dates are logical
    CONSTRAINT chk_content_version_comparisons_test_dates 
        CHECK (test_end_date IS NULL OR test_end_date >= test_start_date)
);

-- Create indexes for content_version_comparisons
CREATE INDEX idx_content_version_comparisons_content_id ON content_version_comparisons(content_id);
CREATE INDEX idx_content_version_comparisons_compared_by ON content_version_comparisons(compared_by);
CREATE INDEX idx_content_version_comparisons_compared_at ON content_version_comparisons(compared_at);
CREATE INDEX idx_content_version_comparisons_test_status ON content_version_comparisons(test_status);
CREATE INDEX idx_content_version_comparisons_performance_winner ON content_version_comparisons(performance_winner) WHERE performance_winner IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_content_version_comparisons_content_status ON content_version_comparisons(content_id, test_status);
CREATE INDEX idx_content_version_comparisons_user_date ON content_version_comparisons(compared_by, compared_at DESC);

-- JSONB index for metrics comparison
CREATE INDEX idx_content_version_comparisons_metrics ON content_version_comparisons USING GIN(metrics_comparison);

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to both tables
CREATE TRIGGER update_content_versions_updated_at 
    BEFORE UPDATE ON content_versions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_content_version_comparisons_updated_at 
    BEFORE UPDATE ON content_version_comparisons 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to automatically increment version numbers
CREATE OR REPLACE FUNCTION get_next_version_number(p_content_id BIGINT)
RETURNS INTEGER AS $$
DECLARE
    next_version INTEGER;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) + 1 
    INTO next_version 
    FROM content_versions 
    WHERE content_id = p_content_id;
    
    RETURN next_version;
END;
$$ LANGUAGE plpgsql;

-- Create function to validate version existence for comparisons
CREATE OR REPLACE FUNCTION validate_version_exists(p_content_id BIGINT, p_version_number INTEGER)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 
        FROM content_versions 
        WHERE content_id = p_content_id AND version_number = p_version_number
    );
END;
$$ LANGUAGE plpgsql;

-- Create trigger to validate versions exist before creating comparisons
CREATE OR REPLACE FUNCTION validate_comparison_versions()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate version A exists
    IF NOT validate_version_exists(NEW.content_id, NEW.version_a) THEN
        RAISE EXCEPTION 'Version % does not exist for content %', NEW.version_a, NEW.content_id;
    END IF;
    
    -- Validate version B exists
    IF NOT validate_version_exists(NEW.content_id, NEW.version_b) THEN
        RAISE EXCEPTION 'Version % does not exist for content %', NEW.version_b, NEW.content_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER validate_content_version_comparisons_versions
    BEFORE INSERT OR UPDATE ON content_version_comparisons
    FOR EACH ROW EXECUTE FUNCTION validate_comparison_versions();

-- Create view for easy version history access
CREATE VIEW v_content_version_history AS
SELECT 
    cv.id,
    cv.content_id,
    cv.version_number,
    cv.title,
    cv.ai_provider,
    cv.ai_model,
    cv.tokens_used,
    cv.generation_cost,
    cv.processing_time_ms,
    cv.readability_score,
    cv.seo_score,
    cv.quality_score,
    cv.sentiment_score,
    cv.word_count,
    cv.character_count,
    cv.created_by,
    cv.created_at,
    u.username as created_by_username,
    cg.title as content_title,
    cg.content_type,
    cg.status as content_status,
    -- Calculate version performance rank
    RANK() OVER (PARTITION BY cv.content_id ORDER BY cv.quality_score DESC NULLS LAST) as quality_rank,
    -- Check if this is the latest version
    cv.version_number = MAX(cv.version_number) OVER (PARTITION BY cv.content_id) as is_latest_version
FROM content_versions cv
JOIN users u ON cv.created_by = u.id
JOIN content_generations cg ON cv.content_id = cg.id
ORDER BY cv.content_id, cv.version_number DESC;

-- Create view for comparison analytics
CREATE VIEW v_content_comparison_analytics AS
SELECT 
    cvc.id,
    cvc.content_id,
    cvc.version_a,
    cvc.version_b,
    cvc.user_preference,
    cvc.performance_winner,
    cvc.statistical_significance,
    cvc.confidence_level,
    cvc.test_status,
    cvc.test_start_date,
    cvc.test_end_date,
    -- Calculate test duration
    CASE 
        WHEN cvc.test_end_date IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (cvc.test_end_date - cvc.test_start_date)) / 86400.0
        ELSE 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - cvc.test_start_date)) / 86400.0
    END as test_duration_days,
    -- Performance metrics
    cvc.version_a_engagement_rate,
    cvc.version_b_engagement_rate,
    cvc.version_a_conversion_rate,
    cvc.version_b_conversion_rate,
    -- Calculate improvement percentages
    CASE 
        WHEN cvc.version_a_engagement_rate > 0 THEN 
            ((cvc.version_b_engagement_rate - cvc.version_a_engagement_rate) / cvc.version_a_engagement_rate) * 100
        ELSE NULL
    END as engagement_improvement_percent,
    CASE 
        WHEN cvc.version_a_conversion_rate > 0 THEN 
            ((cvc.version_b_conversion_rate - cvc.version_a_conversion_rate) / cvc.version_a_conversion_rate) * 100
        ELSE NULL
    END as conversion_improvement_percent,
    cvc.compared_by,
    cvc.compared_at,
    u.username as compared_by_username
FROM content_version_comparisons cvc
JOIN users u ON cvc.compared_by = u.id
ORDER BY cvc.compared_at DESC;

-- Add comments for documentation
COMMENT ON TABLE content_versions IS 'Stores all versions of generated content with full history and metrics';
COMMENT ON TABLE content_version_comparisons IS 'Stores A/B test comparisons between different content versions';
COMMENT ON COLUMN content_versions.generation_params IS 'JSONB field storing all generation parameters used for this version';
COMMENT ON COLUMN content_version_comparisons.metrics_comparison IS 'JSONB field storing detailed comparison metrics and analysis';
COMMENT ON VIEW v_content_version_history IS 'Comprehensive view of content version history with user and ranking information';
COMMENT ON VIEW v_content_comparison_analytics IS 'Analytics view for A/B test comparisons with calculated metrics';