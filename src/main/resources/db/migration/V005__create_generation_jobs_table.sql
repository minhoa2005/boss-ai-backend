-- Create generation_jobs table for content generation queue infrastructure
-- This table supports asynchronous processing with status tracking and priority queuing

CREATE TABLE generation_jobs (
    id BIGSERIAL PRIMARY KEY,
    
    -- Job identification
    job_id VARCHAR(36) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- Job configuration
    request_params JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    priority VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    content_type VARCHAR(50),
    
    -- AI provider information
    ai_provider VARCHAR(50),
    ai_model VARCHAR(100),
    
    -- Job results
    result_content TEXT,
    error_message TEXT,
    error_details JSONB,
    
    -- Retry configuration
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Timing information
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Performance metrics
    processing_time_ms BIGINT,
    tokens_used INTEGER,
    generation_cost DECIMAL(10,4),
    
    -- Additional metadata
    metadata JSONB,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_generation_jobs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for optimal queue performance
CREATE INDEX idx_generation_jobs_status ON generation_jobs(status);
CREATE INDEX idx_generation_jobs_priority ON generation_jobs(priority);
CREATE INDEX idx_generation_jobs_user_id ON generation_jobs(user_id);
CREATE INDEX idx_generation_jobs_created_at ON generation_jobs(created_at);
CREATE INDEX idx_generation_jobs_expires_at ON generation_jobs(expires_at);
CREATE INDEX idx_generation_jobs_next_retry_at ON generation_jobs(next_retry_at);

-- Composite index for queue processing optimization
CREATE INDEX idx_generation_jobs_status_priority ON generation_jobs(status, priority, created_at);

-- Index for cleanup operations
CREATE INDEX idx_generation_jobs_completed_at ON generation_jobs(completed_at) WHERE completed_at IS NOT NULL;

-- Index for retry job queries
CREATE INDEX idx_generation_jobs_retry ON generation_jobs(status, next_retry_at, retry_count) 
    WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;

-- Add check constraints for data integrity
ALTER TABLE generation_jobs ADD CONSTRAINT chk_generation_jobs_status 
    CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED'));

ALTER TABLE generation_jobs ADD CONSTRAINT chk_generation_jobs_priority 
    CHECK (priority IN ('PREMIUM', 'STANDARD', 'BATCH'));

ALTER TABLE generation_jobs ADD CONSTRAINT chk_generation_jobs_retry_count 
    CHECK (retry_count >= 0 AND retry_count <= max_retries);

ALTER TABLE generation_jobs ADD CONSTRAINT chk_generation_jobs_max_retries 
    CHECK (max_retries >= 0 AND max_retries <= 10);

-- Add comments for documentation
COMMENT ON TABLE generation_jobs IS 'Content generation jobs queue with status tracking and priority processing';
COMMENT ON COLUMN generation_jobs.job_id IS 'Unique identifier for tracking job status';
COMMENT ON COLUMN generation_jobs.request_params IS 'JSON parameters for content generation request';
COMMENT ON COLUMN generation_jobs.status IS 'Current job status: QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED, EXPIRED';
COMMENT ON COLUMN generation_jobs.priority IS 'Job priority: PREMIUM (1), STANDARD (2), BATCH (3)';
COMMENT ON COLUMN generation_jobs.expires_at IS 'Job expiration time for automatic cleanup';
COMMENT ON COLUMN generation_jobs.next_retry_at IS 'Next retry time for failed jobs with exponential backoff';
COMMENT ON COLUMN generation_jobs.processing_time_ms IS 'Actual processing time in milliseconds';

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_generation_jobs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER trigger_generation_jobs_updated_at
    BEFORE UPDATE ON generation_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_generation_jobs_updated_at();

-- Insert sample data for testing (optional - remove in production)
-- INSERT INTO generation_jobs (job_id, user_id, request_params, content_type, expires_at) VALUES
-- ('550e8400-e29b-41d4-a716-446655440001', 1, '{"content": "Test content", "industry": "Technology"}', 'blog-post', CURRENT_TIMESTAMP + INTERVAL '24 hours'),
-- ('550e8400-e29b-41d4-a716-446655440002', 1, '{"content": "Another test", "industry": "Healthcare"}', 'social-media', CURRENT_TIMESTAMP + INTERVAL '24 hours');