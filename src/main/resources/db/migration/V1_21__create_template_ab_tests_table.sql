-- Create template A/B tests table for template optimization
CREATE TABLE IF NOT EXISTS template_ab_tests (
    id BIGSERIAL PRIMARY KEY,
    test_name VARCHAR(200) NOT NULL,
    description TEXT,
    variant_a_template_id BIGINT NOT NULL REFERENCES content_templates(id),
    variant_b_template_id BIGINT NOT NULL REFERENCES content_templates(id),
    traffic_split INTEGER NOT NULL DEFAULT 50,
    metric_to_optimize VARCHAR(50) NOT NULL,
    min_sample_size INTEGER NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_traffic_split CHECK (traffic_split >= 0 AND traffic_split <= 100),
    CONSTRAINT chk_different_variants CHECK (variant_a_template_id != variant_b_template_id),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Create indexes for performance
CREATE INDEX idx_template_ab_tests_status ON template_ab_tests(status);
CREATE INDEX idx_template_ab_tests_variant_a ON template_ab_tests(variant_a_template_id);
CREATE INDEX idx_template_ab_tests_variant_b ON template_ab_tests(variant_b_template_id);
CREATE INDEX idx_template_ab_tests_started_at ON template_ab_tests(started_at);
CREATE INDEX idx_template_ab_tests_created_by ON template_ab_tests(created_by);

-- Add comments for documentation
COMMENT ON TABLE template_ab_tests IS 'A/B tests for template optimization and performance comparison';
COMMENT ON COLUMN template_ab_tests.test_name IS 'Name of the A/B test';
COMMENT ON COLUMN template_ab_tests.variant_a_template_id IS 'First template variant to test';
COMMENT ON COLUMN template_ab_tests.variant_b_template_id IS 'Second template variant to test';
COMMENT ON COLUMN template_ab_tests.traffic_split IS 'Percentage of traffic to variant A (0-100)';
COMMENT ON COLUMN template_ab_tests.metric_to_optimize IS 'Metric being optimized (SUCCESS_RATE, AVERAGE_RATING, etc.)';
COMMENT ON COLUMN template_ab_tests.min_sample_size IS 'Minimum samples per variant before declaring winner';
COMMENT ON COLUMN template_ab_tests.status IS 'Test status: ACTIVE, COMPLETED, CANCELLED';
