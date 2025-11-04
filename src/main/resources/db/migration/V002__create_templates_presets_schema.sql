-- Templates and Presets Schema Migration
-- This migration creates the templates and presets tables for Business Enhancement Phase 1
-- Requirements: 2.1, 2.2, 2.3

-- Create content_templates table with full-text search support
CREATE TABLE content_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    
    -- Template configuration
    prompt_template TEXT NOT NULL,
    default_params JSONB NOT NULL DEFAULT '{}',
    
    -- Metadata for categorization and filtering
    industry VARCHAR(100),
    content_type VARCHAR(50) NOT NULL,
    target_audience VARCHAR(200),
    tone VARCHAR(50),
    language VARCHAR(10) DEFAULT 'vi',
    
    -- Usage and quality metrics
    usage_count INTEGER DEFAULT 0 NOT NULL,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    success_rate DECIMAL(5,2) DEFAULT 0.00,
    total_ratings INTEGER DEFAULT 0 NOT NULL,
    
    -- Template status and visibility
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    visibility VARCHAR(20) DEFAULT 'PUBLIC' NOT NULL,
    is_system_template BOOLEAN DEFAULT FALSE NOT NULL,
    is_featured BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Tags for enhanced categorization (stored as array)
    tags TEXT[] DEFAULT '{}',
    
    -- Template validation and requirements
    required_fields TEXT[] DEFAULT '{}',
    optional_fields TEXT[] DEFAULT '{}',
    validation_rules JSONB DEFAULT '{}',
    
    -- Performance and optimization data
    average_generation_time_ms BIGINT,
    average_tokens_used INTEGER,
    average_cost DECIMAL(10,6),
    
    -- Ownership and audit fields
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_content_templates_created_by 
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_content_templates_updated_by 
        FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_content_templates_name_not_empty 
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_content_templates_prompt_not_empty 
        CHECK (LENGTH(TRIM(prompt_template)) > 0),
    CONSTRAINT chk_content_templates_category_not_empty 
        CHECK (LENGTH(TRIM(category)) > 0),
    CONSTRAINT chk_content_templates_content_type_not_empty 
        CHECK (LENGTH(TRIM(content_type)) > 0),
    CONSTRAINT chk_content_templates_usage_count_non_negative 
        CHECK (usage_count >= 0),
    CONSTRAINT chk_content_templates_total_ratings_non_negative 
        CHECK (total_ratings >= 0),
    CONSTRAINT chk_content_templates_average_rating_range 
        CHECK (average_rating >= 0.00 AND average_rating <= 5.00),
    CONSTRAINT chk_content_templates_success_rate_range 
        CHECK (success_rate >= 0.00 AND success_rate <= 100.00),
    CONSTRAINT chk_content_templates_status_valid 
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DRAFT', 'ARCHIVED')),
    CONSTRAINT chk_content_templates_visibility_valid 
        CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'WORKSPACE', 'SYSTEM'))
);

-- Create user_presets table with JSON configuration storage
CREATE TABLE user_presets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- Preset configuration stored as JSONB for flexibility
    configuration JSONB NOT NULL DEFAULT '{}',
    
    -- Preset categorization
    category VARCHAR(100),
    content_type VARCHAR(50),
    
    -- Preset metadata
    is_default BOOLEAN DEFAULT FALSE NOT NULL,
    is_favorite BOOLEAN DEFAULT FALSE NOT NULL,
    usage_count INTEGER DEFAULT 0 NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    
    -- Sharing and collaboration
    is_shared BOOLEAN DEFAULT FALSE NOT NULL,
    shared_with_workspace BOOLEAN DEFAULT FALSE NOT NULL,
    workspace_id BIGINT,
    
    -- Tags for organization
    tags TEXT[] DEFAULT '{}',
    
    -- Performance tracking
    average_generation_time_ms BIGINT,
    average_quality_score DECIMAL(5,2),
    success_rate DECIMAL(5,2) DEFAULT 0.00,
    total_uses INTEGER DEFAULT 0 NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_user_presets_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate preset names per user
    CONSTRAINT uk_user_presets_user_name 
        UNIQUE(user_id, name),
    
    -- Check constraints for data integrity
    CONSTRAINT chk_user_presets_name_not_empty 
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_user_presets_usage_count_non_negative 
        CHECK (usage_count >= 0),
    CONSTRAINT chk_user_presets_total_uses_non_negative 
        CHECK (total_uses >= 0),
    CONSTRAINT chk_user_presets_success_rate_range 
        CHECK (success_rate >= 0.00 AND success_rate <= 100.00),
    CONSTRAINT chk_user_presets_average_quality_score_range 
        CHECK (average_quality_score IS NULL OR (average_quality_score >= 0 AND average_quality_score <= 100)),
    
    -- Only one default preset per user per content type
    CONSTRAINT chk_user_presets_single_default_per_type 
        EXCLUDE (user_id WITH =, content_type WITH =) 
        WHERE (is_default = TRUE AND content_type IS NOT NULL)
);

-- Create template_usage_logs table for analytics
CREATE TABLE template_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_generation_id BIGINT,
    
    -- Usage context
    usage_type VARCHAR(50) DEFAULT 'GENERATION' NOT NULL,
    usage_source VARCHAR(50) DEFAULT 'WEB' NOT NULL,
    
    -- Performance metrics for this usage
    generation_time_ms BIGINT,
    tokens_used INTEGER,
    generation_cost DECIMAL(10,6),
    quality_score DECIMAL(5,2),
    
    -- User feedback and rating
    user_rating INTEGER,
    user_feedback TEXT,
    was_successful BOOLEAN,
    
    -- Template customization tracking
    parameters_used JSONB DEFAULT '{}',
    customizations_made JSONB DEFAULT '{}',
    fields_modified TEXT[] DEFAULT '{}',
    
    -- Session and context information
    session_id VARCHAR(100),
    user_agent TEXT,
    ip_address INET,
    
    -- Geographic and demographic data (for analytics)
    country_code VARCHAR(2),
    timezone VARCHAR(50),
    device_type VARCHAR(20),
    
    -- Audit fields
    used_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_template_usage_logs_template_id 
        FOREIGN KEY (template_id) REFERENCES content_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_template_usage_logs_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_template_usage_logs_content_generation_id 
        FOREIGN KEY (content_generation_id) REFERENCES content_generations(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_template_usage_logs_usage_type_valid 
        CHECK (usage_type IN ('GENERATION', 'PREVIEW', 'COPY', 'SHARE', 'RATE', 'FAVORITE')),
    CONSTRAINT chk_template_usage_logs_usage_source_valid 
        CHECK (usage_source IN ('WEB', 'API', 'MOBILE', 'INTEGRATION')),
    CONSTRAINT chk_template_usage_logs_user_rating_range 
        CHECK (user_rating IS NULL OR (user_rating >= 1 AND user_rating <= 5)),
    CONSTRAINT chk_template_usage_logs_quality_score_range 
        CHECK (quality_score IS NULL OR (quality_score >= 0 AND quality_score <= 100)),
    CONSTRAINT chk_template_usage_logs_generation_time_positive 
        CHECK (generation_time_ms IS NULL OR generation_time_ms > 0),
    CONSTRAINT chk_template_usage_logs_tokens_used_positive 
        CHECK (tokens_used IS NULL OR tokens_used > 0)
);

-- Create indexes for performance optimization

-- Content Templates Indexes
CREATE INDEX idx_content_templates_category ON content_templates(category);
CREATE INDEX idx_content_templates_content_type ON content_templates(content_type);
CREATE INDEX idx_content_templates_industry ON content_templates(industry);
CREATE INDEX idx_content_templates_status ON content_templates(status);
CREATE INDEX idx_content_templates_visibility ON content_templates(visibility);
CREATE INDEX idx_content_templates_created_by ON content_templates(created_by);
CREATE INDEX idx_content_templates_created_at ON content_templates(created_at);
CREATE INDEX idx_content_templates_usage_count ON content_templates(usage_count DESC);
CREATE INDEX idx_content_templates_average_rating ON content_templates(average_rating DESC);
CREATE INDEX idx_content_templates_success_rate ON content_templates(success_rate DESC);
CREATE INDEX idx_content_templates_is_featured ON content_templates(is_featured) WHERE is_featured = TRUE;
CREATE INDEX idx_content_templates_is_system ON content_templates(is_system_template) WHERE is_system_template = TRUE;

-- Composite indexes for common queries
CREATE INDEX idx_content_templates_category_type ON content_templates(category, content_type);
CREATE INDEX idx_content_templates_industry_type ON content_templates(industry, content_type);
CREATE INDEX idx_content_templates_status_visibility ON content_templates(status, visibility);
CREATE INDEX idx_content_templates_featured_rating ON content_templates(is_featured, average_rating DESC) WHERE status = 'ACTIVE';

-- Full-text search indexes
CREATE INDEX idx_content_templates_name_search ON content_templates USING GIN(to_tsvector('english', name));
CREATE INDEX idx_content_templates_description_search ON content_templates USING GIN(to_tsvector('english', COALESCE(description, '')));
CREATE INDEX idx_content_templates_prompt_search ON content_templates USING GIN(to_tsvector('english', prompt_template));

-- JSONB indexes for configuration search
CREATE INDEX idx_content_templates_default_params ON content_templates USING GIN(default_params);
CREATE INDEX idx_content_templates_validation_rules ON content_templates USING GIN(validation_rules);

-- Array indexes for tags and fields
CREATE INDEX idx_content_templates_tags ON content_templates USING GIN(tags);
CREATE INDEX idx_content_templates_required_fields ON content_templates USING GIN(required_fields);

-- User Presets Indexes
CREATE INDEX idx_user_presets_user_id ON user_presets(user_id);
CREATE INDEX idx_user_presets_category ON user_presets(category);
CREATE INDEX idx_user_presets_content_type ON user_presets(content_type);
CREATE INDEX idx_user_presets_is_default ON user_presets(is_default) WHERE is_default = TRUE;
CREATE INDEX idx_user_presets_is_favorite ON user_presets(is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX idx_user_presets_is_shared ON user_presets(is_shared) WHERE is_shared = TRUE;
CREATE INDEX idx_user_presets_usage_count ON user_presets(usage_count DESC);
CREATE INDEX idx_user_presets_last_used_at ON user_presets(last_used_at DESC);
CREATE INDEX idx_user_presets_created_at ON user_presets(created_at);

-- Composite indexes for user presets
CREATE INDEX idx_user_presets_user_category ON user_presets(user_id, category);
CREATE INDEX idx_user_presets_user_type ON user_presets(user_id, content_type);
CREATE INDEX idx_user_presets_user_default ON user_presets(user_id, is_default) WHERE is_default = TRUE;

-- JSONB indexes for preset configuration
CREATE INDEX idx_user_presets_configuration ON user_presets USING GIN(configuration);

-- Array indexes for tags
CREATE INDEX idx_user_presets_tags ON user_presets USING GIN(tags);

-- Template Usage Logs Indexes
CREATE INDEX idx_template_usage_logs_template_id ON template_usage_logs(template_id);
CREATE INDEX idx_template_usage_logs_user_id ON template_usage_logs(user_id);
CREATE INDEX idx_template_usage_logs_content_generation_id ON template_usage_logs(content_generation_id);
CREATE INDEX idx_template_usage_logs_used_at ON template_usage_logs(used_at);
CREATE INDEX idx_template_usage_logs_usage_type ON template_usage_logs(usage_type);
CREATE INDEX idx_template_usage_logs_usage_source ON template_usage_logs(usage_source);
CREATE INDEX idx_template_usage_logs_was_successful ON template_usage_logs(was_successful) WHERE was_successful IS NOT NULL;
CREATE INDEX idx_template_usage_logs_user_rating ON template_usage_logs(user_rating) WHERE user_rating IS NOT NULL;

-- Composite indexes for analytics queries
CREATE INDEX idx_template_usage_logs_template_date ON template_usage_logs(template_id, used_at DESC);
CREATE INDEX idx_template_usage_logs_user_date ON template_usage_logs(user_id, used_at DESC);
CREATE INDEX idx_template_usage_logs_template_success ON template_usage_logs(template_id, was_successful, used_at DESC);

-- JSONB indexes for usage analytics
CREATE INDEX idx_template_usage_logs_parameters ON template_usage_logs USING GIN(parameters_used);
CREATE INDEX idx_template_usage_logs_customizations ON template_usage_logs USING GIN(customizations_made);

-- Array indexes for fields modified
CREATE INDEX idx_template_usage_logs_fields_modified ON template_usage_logs USING GIN(fields_modified);

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$ language 'plpgsql';

-- Apply triggers to tables with updated_at columns
CREATE TRIGGER update_content_templates_updated_at 
    BEFORE UPDATE ON content_templates 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_presets_updated_at 
    BEFORE UPDATE ON user_presets 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to update template usage statistics
CREATE OR REPLACE FUNCTION update_template_usage_stats()
RETURNS TRIGGER AS $
DECLARE
    template_stats RECORD;
BEGIN
    -- Calculate updated statistics for the template
    SELECT 
        COUNT(*) as total_uses,
        AVG(CASE WHEN generation_time_ms IS NOT NULL THEN generation_time_ms END) as avg_time,
        AVG(CASE WHEN tokens_used IS NOT NULL THEN tokens_used END) as avg_tokens,
        AVG(CASE WHEN generation_cost IS NOT NULL THEN generation_cost END) as avg_cost,
        AVG(CASE WHEN user_rating IS NOT NULL THEN user_rating END) as avg_rating,
        COUNT(CASE WHEN user_rating IS NOT NULL THEN 1 END) as total_ratings,
        (COUNT(CASE WHEN was_successful = TRUE THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN was_successful IS NOT NULL THEN 1 END), 0)) as success_rate
    INTO template_stats
    FROM template_usage_logs 
    WHERE template_id = NEW.template_id;
    
    -- Update the template statistics
    UPDATE content_templates 
    SET 
        usage_count = template_stats.total_uses,
        average_generation_time_ms = template_stats.avg_time,
        average_tokens_used = template_stats.avg_tokens,
        average_cost = template_stats.avg_cost,
        average_rating = COALESCE(template_stats.avg_rating, 0.00),
        total_ratings = template_stats.total_ratings,
        success_rate = COALESCE(template_stats.success_rate, 0.00),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.template_id;
    
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create trigger to automatically update template statistics
CREATE TRIGGER update_template_stats_on_usage
    AFTER INSERT OR UPDATE ON template_usage_logs
    FOR EACH ROW EXECUTE FUNCTION update_template_usage_stats();

-- Create function to update user preset usage statistics
CREATE OR REPLACE FUNCTION update_preset_usage_stats()
RETURNS TRIGGER AS $
BEGIN
    -- Update last_used_at and increment usage_count
    UPDATE user_presets 
    SET 
        usage_count = usage_count + 1,
        last_used_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;
    
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create function to validate template configuration
CREATE OR REPLACE FUNCTION validate_template_configuration()
RETURNS TRIGGER AS $
BEGIN
    -- Validate that prompt_template contains required placeholders
    IF NEW.prompt_template IS NULL OR LENGTH(TRIM(NEW.prompt_template)) = 0 THEN
        RAISE EXCEPTION 'Template prompt cannot be empty';
    END IF;
    
    -- Validate that required_fields are present in prompt_template if specified
    IF NEW.required_fields IS NOT NULL AND array_length(NEW.required_fields, 1) > 0 THEN
        DECLARE
            field TEXT;
        BEGIN
            FOREACH field IN ARRAY NEW.required_fields
            LOOP
                IF NEW.prompt_template NOT LIKE '%{' || field || '}%' THEN
                    RAISE EXCEPTION 'Required field "%" not found in prompt template', field;
                END IF;
            END LOOP;
        END;
    END IF;
    
    -- Validate JSONB structure for default_params
    IF NEW.default_params IS NOT NULL THEN
        BEGIN
            -- Try to access the JSONB to validate structure
            PERFORM NEW.default_params::text;
        EXCEPTION WHEN OTHERS THEN
            RAISE EXCEPTION 'Invalid JSON structure in default_params';
        END;
    END IF;
    
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create trigger to validate template configuration
CREATE TRIGGER validate_template_config_on_insert_update
    BEFORE INSERT OR UPDATE ON content_templates
    FOR EACH ROW EXECUTE FUNCTION validate_template_configuration();

-- Create function to ensure only one default preset per user per content type
CREATE OR REPLACE FUNCTION ensure_single_default_preset()
RETURNS TRIGGER AS $
BEGIN
    -- If setting this preset as default, unset others for the same user and content type
    IF NEW.is_default = TRUE THEN
        UPDATE user_presets 
        SET is_default = FALSE, updated_at = CURRENT_TIMESTAMP
        WHERE user_id = NEW.user_id 
          AND content_type = NEW.content_type 
          AND id != NEW.id
          AND is_default = TRUE;
    END IF;
    
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create trigger to ensure single default preset
CREATE TRIGGER ensure_single_default_preset_trigger
    BEFORE INSERT OR UPDATE ON user_presets
    FOR EACH ROW EXECUTE FUNCTION ensure_single_default_preset();

-- Create views for easy access to template and preset data

-- View for popular templates with usage statistics
CREATE VIEW v_popular_templates AS
SELECT 
    ct.id,
    ct.name,
    ct.description,
    ct.category,
    ct.content_type,
    ct.industry,
    ct.target_audience,
    ct.tone,
    ct.language,
    ct.usage_count,
    ct.average_rating,
    ct.total_ratings,
    ct.success_rate,
    ct.is_featured,
    ct.is_system_template,
    ct.tags,
    ct.created_at,
    u.username as created_by_username,
    -- Calculate popularity score based on usage, rating, and recency
    (
        (ct.usage_count * 0.4) + 
        (ct.average_rating * 20 * 0.3) + 
        (ct.success_rate * 0.2) + 
        (CASE WHEN ct.is_featured THEN 50 ELSE 0 END * 0.1)
    ) as popularity_score,
    -- Calculate days since creation
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - ct.created_at)) / 86400.0 as days_since_creation
FROM content_templates ct
JOIN users u ON ct.created_by = u.id
WHERE ct.status = 'ACTIVE' AND ct.visibility IN ('PUBLIC', 'SYSTEM')
ORDER BY popularity_score DESC, ct.average_rating DESC, ct.usage_count DESC;

-- View for user preset analytics
CREATE VIEW v_user_preset_analytics AS
SELECT 
    up.id,
    up.user_id,
    up.name,
    up.description,
    up.category,
    up.content_type,
    up.usage_count,
    up.last_used_at,
    up.is_default,
    up.is_favorite,
    up.is_shared,
    up.tags,
    up.created_at,
    u.username,
    -- Calculate days since last use
    CASE 
        WHEN up.last_used_at IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - up.last_used_at)) / 86400.0
        ELSE NULL
    END as days_since_last_use,
    -- Calculate usage frequency (uses per day since creation)
    CASE 
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - up.created_at)) > 0 THEN
            up.usage_count / (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - up.created_at)) / 86400.0)
        ELSE 0
    END as usage_frequency_per_day
FROM user_presets up
JOIN users u ON up.user_id = u.id
ORDER BY up.usage_count DESC, up.last_used_at DESC;

-- View for template usage analytics
CREATE VIEW v_template_usage_analytics AS
SELECT 
    tul.template_id,
    ct.name as template_name,
    ct.category,
    ct.content_type,
    COUNT(*) as total_uses,
    COUNT(DISTINCT tul.user_id) as unique_users,
    AVG(tul.generation_time_ms) as avg_generation_time_ms,
    AVG(tul.tokens_used) as avg_tokens_used,
    AVG(tul.generation_cost) as avg_generation_cost,
    AVG(tul.quality_score) as avg_quality_score,
    AVG(CASE WHEN tul.user_rating IS NOT NULL THEN tul.user_rating END) as avg_user_rating,
    COUNT(CASE WHEN tul.user_rating IS NOT NULL THEN 1 END) as total_ratings,
    COUNT(CASE WHEN tul.was_successful = TRUE THEN 1 END) as successful_uses,
    COUNT(CASE WHEN tul.was_successful = FALSE THEN 1 END) as failed_uses,
    (COUNT(CASE WHEN tul.was_successful = TRUE THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN tul.was_successful IS NOT NULL THEN 1 END), 0)) as success_rate_percent,
    MIN(tul.used_at) as first_used_at,
    MAX(tul.used_at) as last_used_at,
    -- Usage trend (uses in last 7 days vs previous 7 days)
    COUNT(CASE WHEN tul.used_at >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as uses_last_7_days,
    COUNT(CASE WHEN tul.used_at >= CURRENT_TIMESTAMP - INTERVAL '14 days' AND tul.used_at < CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as uses_previous_7_days
FROM template_usage_logs tul
JOIN content_templates ct ON tul.template_id = ct.id
GROUP BY tul.template_id, ct.name, ct.category, ct.content_type
ORDER BY total_uses DESC, avg_user_rating DESC;

-- Add comments for documentation
COMMENT ON TABLE content_templates IS 'Stores content generation templates with full-text search support and usage analytics';
COMMENT ON TABLE user_presets IS 'Stores user-specific presets with JSON configuration and sharing capabilities';
COMMENT ON TABLE template_usage_logs IS 'Comprehensive logging of template usage for analytics and optimization';

COMMENT ON COLUMN content_templates.prompt_template IS 'Template string with placeholders for dynamic content generation';
COMMENT ON COLUMN content_templates.default_params IS 'JSONB field storing default parameters for template instantiation';
COMMENT ON COLUMN content_templates.tags IS 'Array of tags for enhanced categorization and search';
COMMENT ON COLUMN content_templates.validation_rules IS 'JSONB field storing validation rules for template parameters';

COMMENT ON COLUMN user_presets.configuration IS 'JSONB field storing complete user preset configuration';
COMMENT ON COLUMN user_presets.tags IS 'Array of user-defined tags for preset organization';

COMMENT ON COLUMN template_usage_logs.parameters_used IS 'JSONB field storing actual parameters used during template instantiation';
COMMENT ON COLUMN template_usage_logs.customizations_made IS 'JSONB field storing user customizations to the template';

COMMENT ON VIEW v_popular_templates IS 'Analytics view showing popular templates with calculated popularity scores';
COMMENT ON VIEW v_user_preset_analytics IS 'Analytics view for user preset usage patterns and statistics';
COMMENT ON VIEW v_template_usage_analytics IS 'Comprehensive analytics view for template usage patterns and performance metrics';