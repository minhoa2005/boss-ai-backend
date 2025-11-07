-- Add scheduling columns to video_generation_jobs table
-- This migration adds support for scheduled video generation

ALTER TABLE video_generation_jobs
    ADD COLUMN scheduled_at TIMESTAMP,
    ADD COLUMN scheduled_by BIGINT;

-- Add index for scheduled jobs query
CREATE INDEX idx_video_job_scheduled ON video_generation_jobs(scheduled_at, status, priority);

-- Add comment for documentation
COMMENT ON COLUMN video_generation_jobs.scheduled_at IS 'When the job should start processing (null for immediate)';
COMMENT ON COLUMN video_generation_jobs.scheduled_by IS 'User ID who scheduled the job';
