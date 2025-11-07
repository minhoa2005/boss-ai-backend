-- Create video_templates table
CREATE TABLE IF NOT EXISTS video_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    
    -- Style Configuration
    style_name VARCHAR(100),
    animation_style VARCHAR(100),
    transition_style VARCHAR(100),
    
    -- Branding Options
    logo_url VARCHAR(500),
    logo_position VARCHAR(50),
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    accent_color VARCHAR(7),
    font_family VARCHAR(100),
    font_size VARCHAR(20),
    
    -- Duration and Format Options
    default_duration INTEGER,
    min_duration INTEGER,
    max_duration INTEGER,
    aspect_ratio VARCHAR(20),
    resolution VARCHAR(20),
    frame_rate INTEGER,
    video_format VARCHAR(20),
    
    -- Voice and Audio Options
    voice_over_enabled BOOLEAN DEFAULT TRUE,
    voice_type VARCHAR(100),
    voice_speed VARCHAR(20),
    background_music_enabled BOOLEAN DEFAULT FALSE,
    music_genre VARCHAR(100),
    music_volume INTEGER,
    
    -- Advanced Configuration
    advanced_config JSONB,
    
    -- Template Metadata
    is_public BOOLEAN DEFAULT FALSE,
    is_system_template BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0,
    
    -- Ownership
    created_by BIGINT REFERENCES users(id),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for video_templates
CREATE INDEX idx_video_templates_category ON video_templates(category);
CREATE INDEX idx_video_templates_style_name ON video_templates(style_name);
CREATE INDEX idx_video_templates_is_public ON video_templates(is_public);
CREATE INDEX idx_video_templates_created_by ON video_templates(created_by);
CREATE INDEX idx_video_templates_usage_count ON video_templates(usage_count DESC);
CREATE INDEX idx_video_templates_average_rating ON video_templates(average_rating DESC);

-- Create video_template_usage_logs table
CREATE TABLE IF NOT EXISTS video_template_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES video_templates(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    content_id BIGINT REFERENCES content_generations(id),
    
    video_duration INTEGER,
    generation_status VARCHAR(50),
    processing_time_ms BIGINT,
    file_size_bytes BIGINT,
    error_message TEXT,
    
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for video_template_usage_logs
CREATE INDEX idx_video_template_usage_logs_template_id ON video_template_usage_logs(template_id);
CREATE INDEX idx_video_template_usage_logs_user_id ON video_template_usage_logs(user_id);
CREATE INDEX idx_video_template_usage_logs_used_at ON video_template_usage_logs(used_at DESC);
CREATE INDEX idx_video_template_usage_logs_status ON video_template_usage_logs(generation_status);

-- Insert system video templates
INSERT INTO video_templates (
    name, description, category, style_name, animation_style, transition_style,
    primary_color, secondary_color, accent_color, font_family, font_size,
    default_duration, min_duration, max_duration, aspect_ratio, resolution, frame_rate, video_format,
    voice_over_enabled, voice_type, voice_speed, background_music_enabled, music_genre, music_volume,
    is_public, is_system_template, created_by
) VALUES
-- Professional Business Template
('Professional Business', 'Clean and professional template for business content', 'business', 
 'professional', 'smooth', 'fade',
 '#1E3A8A', '#3B82F6', '#60A5FA', 'Inter', 'medium',
 60, 30, 180, '16:9', '1080p', 30, 'mp4',
 TRUE, 'neutral', 'normal', TRUE, 'corporate', 30,
 TRUE, TRUE, 1),

-- Creative Marketing Template
('Creative Marketing', 'Dynamic and engaging template for marketing videos', 'marketing',
 'creative', 'dynamic', 'slide',
 '#DC2626', '#EF4444', '#F87171', 'Poppins', 'large',
 45, 15, 120, '16:9', '1080p', 30, 'mp4',
 TRUE, 'female', 'fast', TRUE, 'upbeat', 40,
 TRUE, TRUE, 1),

-- Social Media Short
('Social Media Short', 'Optimized for social media platforms', 'social',
 'casual', 'energetic', 'zoom',
 '#7C3AED', '#8B5CF6', '#A78BFA', 'Montserrat', 'large',
 30, 15, 60, '9:16', '1080p', 30, 'mp4',
 TRUE, 'female', 'fast', TRUE, 'pop', 50,
 TRUE, TRUE, 1),

-- Educational Content
('Educational Content', 'Clear and informative template for educational videos', 'education',
 'professional', 'minimal', 'fade',
 '#059669', '#10B981', '#34D399', 'Roboto', 'medium',
 120, 60, 300, '16:9', '1080p', 30, 'mp4',
 TRUE, 'neutral', 'normal', FALSE, NULL, 0,
 TRUE, TRUE, 1),

-- Product Showcase
('Product Showcase', 'Highlight products with style and elegance', 'product',
 'corporate', 'smooth', 'slide',
 '#0891B2', '#06B6D4', '#22D3EE', 'Lato', 'medium',
 90, 45, 180, '16:9', '1080p', 30, 'mp4',
 TRUE, 'male', 'normal', TRUE, 'ambient', 25,
 TRUE, TRUE, 1);

-- Add comments
COMMENT ON TABLE video_templates IS 'Stores video template configurations with customizable styles and branding';
COMMENT ON TABLE video_template_usage_logs IS 'Tracks video template usage for analytics and recommendations';
