-- Create video_generation_jobs table for batch video processing queue
CREATE TABLE IF NOT EXISTS video_generation_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id BIGINT REFERENCES video_templates(id) ON DELETE SET NULL,
    content_id BIGINT REFERENCES content_generations(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    priority VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    
    -- Video Information
    video_title VARCHAR(500),
    video_description TEXT,
    video_script TEXT,
    duration INTEGER,
    
    -- Configuration
    branding_config JSONB,
    generation_params JSONB,
    
    -- Processing Information
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    processing_time_ms BIGINT,
    
    -- Result Information
    video_url VARCHAR(1000),
    thumbnail_url VARCHAR(1000),
    video_size_bytes BIGINT,
    video_format VARCHAR(20),
    
    -- Error Handling
    error_message TEXT,
    error_code VARCHAR(100),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP,
    
    -- Batch Information
    batch_id VARCHAR(100),
    batch_position INTEGER,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_video_job_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_video_job_priority CHECK (priority IN ('LOW', 'STANDARD', 'HIGH', 'URGENT')),
    CONSTRAINT chk_video_job_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_video_job_max_retries CHECK (max_retries >= 0)
);

-- Create indexes for efficient querying
CREATE INDEX idx_video_job_status ON video_generation_jobs(status);
CREATE INDEX idx_video_job_user ON video_generation_jobs(user_id);
CREATE INDEX idx_video_job_created ON video_generation_jobs(created_at);
CREATE INDEX idx_video_job_priority ON video_generation_jobs(priority, created_at);
CREATE INDEX idx_video_job_batch ON video_generation_jobs(batch_id);
CREATE INDEX idx_video_job_template ON video_generation_jobs(template_id);
CREATE INDEX idx_video_job_retry ON video_generation_jobs(status, next_retry_at) WHERE status = 'FAILED';

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_video_job_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_video_job_updated_at
    BEFORE UPDATE ON video_generation_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_video_job_updated_at();

-- Add comments for documentation
COMMENT ON TABLE video_generation_jobs IS 'Queue system for batch video generation processing';
COMMENT ON COLUMN video_generation_jobs.job_id IS 'Unique identifier for the video generation job';
COMMENT ON COLUMN video_generation_jobs.status IS 'Current status of the job: QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN video_generation_jobs.priority IS 'Job priority: LOW, STANDARD, HIGH, URGENT';
COMMENT ON COLUMN video_generation_jobs.batch_id IS 'Identifier for grouping multiple video jobs together';
COMMENT ON COLUMN video_generation_jobs.batch_position IS 'Position of this job within its batch';
COMMENT ON COLUMN video_generation_jobs.retry_count IS 'Number of times this job has been retried';
COMMENT ON COLUMN video_generation_jobs.next_retry_at IS 'Timestamp when the job should be retried next';
