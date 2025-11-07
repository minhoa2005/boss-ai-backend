-- Video Publishing and Distribution Tables

-- Video Publications table
CREATE TABLE IF NOT EXISTS video_publications (
    id BIGSERIAL PRIMARY KEY,
    video_job_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    platform_video_id VARCHAR(255),
    publication_url VARCHAR(1000),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP,
    published_at TIMESTAMP,
    title VARCHAR(500),
    description TEXT,
    tags TEXT,
    visibility VARCHAR(50) DEFAULT 'PUBLIC',
    category VARCHAR(100),
    thumbnail_url VARCHAR(1000),
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_video_publications_video_job 
        FOREIGN KEY (video_job_id) REFERENCES video_generation_jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_publications_user 
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for video_publications
CREATE INDEX idx_video_publications_video_job ON video_publications(video_job_id);
CREATE INDEX idx_video_publications_platform ON video_publications(platform);
CREATE INDEX idx_video_publications_status ON video_publications(status);
CREATE INDEX idx_video_publications_scheduled_at ON video_publications(scheduled_at);
CREATE INDEX idx_video_publications_created_by ON video_publications(created_by);
CREATE INDEX idx_video_publications_platform_video_id ON video_publications(platform_video_id);

-- Video Analytics table
CREATE TABLE IF NOT EXISTS video_analytics (
    id BIGSERIAL PRIMARY KEY,
    publication_id BIGINT NOT NULL,
    views BIGINT DEFAULT 0,
    likes BIGINT DEFAULT 0,
    dislikes BIGINT DEFAULT 0,
    comments BIGINT DEFAULT 0,
    shares BIGINT DEFAULT 0,
    watch_time_seconds BIGINT DEFAULT 0,
    average_view_duration_seconds DOUBLE PRECISION,
    engagement_rate DOUBLE PRECISION,
    click_through_rate DOUBLE PRECISION,
    conversion_rate DOUBLE PRECISION,
    revenue DOUBLE PRECISION,
    impressions BIGINT DEFAULT 0,
    reach BIGINT DEFAULT 0,
    snapshot_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_video_analytics_publication 
        FOREIGN KEY (publication_id) REFERENCES video_publications(id) ON DELETE CASCADE
);

-- Indexes for video_analytics
CREATE INDEX idx_video_analytics_publication ON video_analytics(publication_id);
CREATE INDEX idx_video_analytics_snapshot_at ON video_analytics(snapshot_at);
CREATE INDEX idx_video_analytics_engagement_rate ON video_analytics(engagement_rate);

-- Video SEO Metadata table
CREATE TABLE IF NOT EXISTS video_seo_metadata (
    id BIGSERIAL PRIMARY KEY,
    video_job_id BIGINT NOT NULL UNIQUE,
    optimized_title VARCHAR(500),
    optimized_description TEXT,
    keywords TEXT,
    hashtags TEXT,
    target_audience VARCHAR(255),
    content_category VARCHAR(100),
    language VARCHAR(10) DEFAULT 'en',
    transcript TEXT,
    closed_captions_url VARCHAR(1000),
    thumbnail_alt_text VARCHAR(500),
    schema_markup TEXT,
    canonical_url VARCHAR(1000),
    seo_score DOUBLE PRECISION,
    readability_score DOUBLE PRECISION,
    keyword_density DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_video_seo_metadata_video_job 
        FOREIGN KEY (video_job_id) REFERENCES video_generation_jobs(id) ON DELETE CASCADE
);

-- Indexes for video_seo_metadata
CREATE INDEX idx_video_seo_metadata_video_job ON video_seo_metadata(video_job_id);
CREATE INDEX idx_video_seo_metadata_seo_score ON video_seo_metadata(seo_score);
CREATE INDEX idx_video_seo_metadata_content_category ON video_seo_metadata(content_category);

-- Comments
COMMENT ON TABLE video_publications IS 'Stores video publication records for social media platforms';
COMMENT ON TABLE video_analytics IS 'Tracks video performance metrics and analytics';
COMMENT ON TABLE video_seo_metadata IS 'Stores SEO optimization metadata for videos';

COMMENT ON COLUMN video_publications.platform IS 'Social media platform: YOUTUBE, FACEBOOK, INSTAGRAM, TIKTOK, LINKEDIN, TWITTER';
COMMENT ON COLUMN video_publications.status IS 'Publication status: PENDING, SCHEDULED, PUBLISHING, PUBLISHED, FAILED';
COMMENT ON COLUMN video_publications.visibility IS 'Video visibility: PUBLIC, PRIVATE, UNLISTED';
COMMENT ON COLUMN video_analytics.engagement_rate IS 'Calculated as (likes + comments + shares) / views * 100';
COMMENT ON COLUMN video_seo_metadata.seo_score IS 'Overall SEO score (0-100) based on optimization factors';
