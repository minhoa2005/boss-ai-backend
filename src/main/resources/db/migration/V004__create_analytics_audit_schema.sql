-- Analytics and Audit Schema Migration
-- This migration creates the analytics and audit tables for Business Enhancement Phase 1
-- Requirements: 4.1, 4.2, 7.1, 7.2

-- Create content_performance table for content metrics tracking
CREATE TABLE content_performance (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    version_number INTEGER,
    
    -- Performance metrics
    view_count INTEGER DEFAULT 0 NOT NULL,
    unique_view_count INTEGER DEFAULT 0 NOT NULL,
    engagement_rate DECIMAL(5,4) DEFAULT 0.0000,
    click_through_rate DECIMAL(5,4) DEFAULT 0.0000,
    conversion_rate DECIMAL(5,4) DEFAULT 0.0000,
    bounce_rate DECIMAL(5,4) DEFAULT 0.0000,
    
    -- Time-based metrics
    average_time_on_page INTEGER, -- in seconds
    total_time_spent INTEGER DEFAULT 0, -- in seconds
    session_duration_avg INTEGER, -- in seconds
    
    -- Social and sharing metrics
    share_count INTEGER DEFAULT 0 NOT NULL,
    like_count INTEGER DEFAULT 0 NOT NULL,
    comment_count INTEGER DEFAULT 0 NOT NULL,
    social_engagement_score DECIMAL(8,4) DEFAULT 0.0000,
    
    -- SEO and search metrics
    search_impressions INTEGER DEFAULT 0 NOT NULL,
    search_clicks INTEGER DEFAULT 0 NOT NULL,
    search_position_avg DECIMAL(5,2),
    organic_traffic_count INTEGER DEFAULT 0 NOT NULL,
    
    -- Content quality metrics
    readability_score DECIMAL(5,2),
    seo_score DECIMAL(5,2),
    content_quality_score DECIMAL(5,2),
    user_satisfaction_score DECIMAL(5,2),
    
    -- Performance benchmarks
    industry_benchmark_score DECIMAL(5,2),
    competitor_comparison_score DECIMAL(5,2),
    performance_trend VARCHAR(20) DEFAULT 'STABLE',
    
    -- Geographic and demographic data
    top_countries JSONB DEFAULT '[]',
    top_cities JSONB DEFAULT '[]',
    age_demographics JSONB DEFAULT '{}',
    device_breakdown JSONB DEFAULT '{}',
    
    -- Revenue and conversion metrics
    revenue_generated DECIMAL(12,2) DEFAULT 0.00,
    cost_per_acquisition DECIMAL(10,4),
    return_on_investment DECIMAL(8,4),
    conversion_value DECIMAL(12,2) DEFAULT 0.00,
    
    -- Time period for metrics
    measurement_period VARCHAR(20) DEFAULT 'DAILY' NOT NULL,
    period_start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Data collection metadata
    data_source VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL,
    collection_method VARCHAR(50) DEFAULT 'AUTOMATIC' NOT NULL,
    data_quality_score DECIMAL(3,2) DEFAULT 1.00,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_content_performance_content_id 
        FOREIGN KEY (content_id) REFERENCES content_generations(id) ON DELETE CASCADE,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_content_performance_counts_non_negative 
        CHECK (
            view_count >= 0 AND unique_view_count >= 0 AND 
            share_count >= 0 AND like_count >= 0 AND comment_count >= 0 AND
            search_impressions >= 0 AND search_clicks >= 0 AND organic_traffic_count >= 0
        ),
    CONSTRAINT chk_content_performance_rates_valid 
        CHECK (
            engagement_rate >= 0 AND engagement_rate <= 1 AND
            click_through_rate >= 0 AND click_through_rate <= 1 AND
            conversion_rate >= 0 AND conversion_rate <= 1 AND
            bounce_rate >= 0 AND bounce_rate <= 1
        ),
    CONSTRAINT chk_content_performance_scores_range 
        CHECK (
            (readability_score IS NULL OR (readability_score >= 0 AND readability_score <= 100)) AND
            (seo_score IS NULL OR (seo_score >= 0 AND seo_score <= 100)) AND
            (content_quality_score IS NULL OR (content_quality_score >= 0 AND content_quality_score <= 100)) AND
            (user_satisfaction_score IS NULL OR (user_satisfaction_score >= 0 AND user_satisfaction_score <= 100)) AND
            (data_quality_score >= 0 AND data_quality_score <= 1)
        ),
    CONSTRAINT chk_content_performance_time_periods_logical 
        CHECK (period_end_date >= period_start_date),
    CONSTRAINT chk_content_performance_measurement_period_valid 
        CHECK (measurement_period IN ('HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT chk_content_performance_performance_trend_valid 
        CHECK (performance_trend IN ('IMPROVING', 'STABLE', 'DECLINING', 'VOLATILE')),
    CONSTRAINT chk_content_performance_data_source_valid 
        CHECK (data_source IN ('SYSTEM', 'GOOGLE_ANALYTICS', 'SOCIAL_MEDIA', 'MANUAL', 'API')),
    CONSTRAINT chk_content_performance_collection_method_valid 
        CHECK (collection_method IN ('AUTOMATIC', 'MANUAL', 'SCHEDULED', 'REAL_TIME')),
    CONSTRAINT chk_content_performance_unique_view_count_logical 
        CHECK (unique_view_count <= view_count),
    CONSTRAINT chk_content_performance_search_clicks_logical 
        CHECK (search_clicks <= search_impressions),
    CONSTRAINT chk_content_performance_version_number_positive 
        CHECK (version_number IS NULL OR version_number > 0)
);

-- Create user_behavior_analytics table for user activity analysis
CREATE TABLE user_behavior_analytics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100),
    
    -- User identification and segmentation
    user_segment VARCHAR(50),
    user_cohort VARCHAR(50),
    user_type VARCHAR(30) DEFAULT 'REGULAR',
    subscription_tier VARCHAR(30),
    
    -- Activity and engagement metrics
    page_views INTEGER DEFAULT 0 NOT NULL,
    unique_page_views INTEGER DEFAULT 0 NOT NULL,
    session_duration INTEGER DEFAULT 0, -- in seconds
    bounce_rate DECIMAL(5,4) DEFAULT 0.0000,
    pages_per_session DECIMAL(5,2) DEFAULT 0.00,
    
    -- Content interaction metrics
    content_generated INTEGER DEFAULT 0 NOT NULL,
    content_shared INTEGER DEFAULT 0 NOT NULL,
    content_liked INTEGER DEFAULT 0 NOT NULL,
    content_commented INTEGER DEFAULT 0 NOT NULL,
    content_downloaded INTEGER DEFAULT 0 NOT NULL,
    
    -- Feature usage tracking
    features_used JSONB DEFAULT '[]',
    most_used_feature VARCHAR(100),
    feature_adoption_score DECIMAL(5,2) DEFAULT 0.00,
    advanced_features_used INTEGER DEFAULT 0 NOT NULL,
    
    -- User journey and funnel metrics
    funnel_stage VARCHAR(50),
    conversion_events JSONB DEFAULT '[]',
    drop_off_points JSONB DEFAULT '[]',
    user_journey_path JSONB DEFAULT '[]',
    
    -- Engagement patterns
    login_frequency DECIMAL(5,2) DEFAULT 0.00, -- logins per week
    average_session_length INTEGER DEFAULT 0, -- in seconds
    peak_activity_hour INTEGER, -- 0-23
    peak_activity_day INTEGER, -- 1-7 (Monday=1)
    activity_consistency_score DECIMAL(5,2) DEFAULT 0.00,
    
    -- User preferences and behavior
    preferred_content_types JSONB DEFAULT '[]',
    preferred_features JSONB DEFAULT '[]',
    interaction_patterns JSONB DEFAULT '{}',
    customization_level VARCHAR(20) DEFAULT 'BASIC',
    
    -- Geographic and device information
    country_code VARCHAR(2),
    city VARCHAR(100),
    timezone VARCHAR(50),
    device_type VARCHAR(20),
    browser VARCHAR(50),
    operating_system VARCHAR(50),
    screen_resolution VARCHAR(20),
    
    -- Performance and satisfaction metrics
    page_load_time_avg INTEGER, -- in milliseconds
    error_encounters INTEGER DEFAULT 0 NOT NULL,
    support_tickets_created INTEGER DEFAULT 0 NOT NULL,
    user_satisfaction_rating DECIMAL(3,2),
    net_promoter_score INTEGER,
    
    -- Behavioral insights
    user_intent VARCHAR(50),
    search_queries JSONB DEFAULT '[]',
    exit_pages JSONB DEFAULT '[]',
    referral_sources JSONB DEFAULT '[]',
    
    -- Time period for analytics
    measurement_period VARCHAR(20) DEFAULT 'DAILY' NOT NULL,
    period_start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Data collection metadata
    data_source VARCHAR(50) DEFAULT 'SYSTEM' NOT NULL,
    collection_method VARCHAR(50) DEFAULT 'AUTOMATIC' NOT NULL,
    data_quality_score DECIMAL(3,2) DEFAULT 1.00,
    privacy_compliant BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_user_behavior_analytics_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_user_behavior_analytics_counts_non_negative 
        CHECK (
            page_views >= 0 AND unique_page_views >= 0 AND
            content_generated >= 0 AND content_shared >= 0 AND content_liked >= 0 AND
            content_commented >= 0 AND content_downloaded >= 0 AND
            advanced_features_used >= 0 AND error_encounters >= 0 AND
            support_tickets_created >= 0
        ),
    CONSTRAINT chk_user_behavior_analytics_rates_valid 
        CHECK (
            bounce_rate >= 0 AND bounce_rate <= 1 AND
            pages_per_session >= 0 AND
            feature_adoption_score >= 0 AND feature_adoption_score <= 100 AND
            activity_consistency_score >= 0 AND activity_consistency_score <= 100 AND
            login_frequency >= 0 AND
            data_quality_score >= 0 AND data_quality_score <= 1
        ),
    CONSTRAINT chk_user_behavior_analytics_time_periods_logical 
        CHECK (period_end_date >= period_start_date),
    CONSTRAINT chk_user_behavior_analytics_measurement_period_valid 
        CHECK (measurement_period IN ('HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT chk_user_behavior_analytics_user_type_valid 
        CHECK (user_type IN ('NEW', 'REGULAR', 'POWER_USER', 'PREMIUM', 'ENTERPRISE', 'CHURNED')),
    CONSTRAINT chk_user_behavior_analytics_customization_level_valid 
        CHECK (customization_level IN ('BASIC', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_user_behavior_analytics_device_type_valid 
        CHECK (device_type IS NULL OR device_type IN ('DESKTOP', 'MOBILE', 'TABLET', 'OTHER')),
    CONSTRAINT chk_user_behavior_analytics_peak_activity_hour_valid 
        CHECK (peak_activity_hour IS NULL OR (peak_activity_hour >= 0 AND peak_activity_hour <= 23)),
    CONSTRAINT chk_user_behavior_analytics_peak_activity_day_valid 
        CHECK (peak_activity_day IS NULL OR (peak_activity_day >= 1 AND peak_activity_day <= 7)),
    CONSTRAINT chk_user_behavior_analytics_satisfaction_rating_valid 
        CHECK (user_satisfaction_rating IS NULL OR (user_satisfaction_rating >= 1 AND user_satisfaction_rating <= 5)),
    CONSTRAINT chk_user_behavior_analytics_nps_valid 
        CHECK (net_promoter_score IS NULL OR (net_promoter_score >= 0 AND net_promoter_score <= 10)),
    CONSTRAINT chk_user_behavior_analytics_unique_page_views_logical 
        CHECK (unique_page_views <= page_views)
);

-- Create system_metrics table for performance monitoring
CREATE TABLE system_metrics (
    id BIGSERIAL PRIMARY KEY,
    
    -- System identification
    service_name VARCHAR(100) NOT NULL,
    instance_id VARCHAR(100),
    environment VARCHAR(20) DEFAULT 'PRODUCTION' NOT NULL,
    version VARCHAR(50),
    
    -- Performance metrics
    response_time_avg DECIMAL(10,3), -- in milliseconds
    response_time_p50 DECIMAL(10,3), -- 50th percentile
    response_time_p95 DECIMAL(10,3), -- 95th percentile
    response_time_p99 DECIMAL(10,3), -- 99th percentile
    response_time_max DECIMAL(10,3), -- maximum response time
    
    -- Throughput metrics
    requests_per_second DECIMAL(10,2) DEFAULT 0.00,
    requests_total INTEGER DEFAULT 0 NOT NULL,
    successful_requests INTEGER DEFAULT 0 NOT NULL,
    failed_requests INTEGER DEFAULT 0 NOT NULL,
    
    -- Error and reliability metrics
    error_rate DECIMAL(5,4) DEFAULT 0.0000,
    success_rate DECIMAL(5,4) DEFAULT 1.0000,
    availability_percentage DECIMAL(5,2) DEFAULT 100.00,
    uptime_seconds INTEGER DEFAULT 0 NOT NULL,
    downtime_seconds INTEGER DEFAULT 0 NOT NULL,
    
    -- Resource utilization metrics
    cpu_usage_percentage DECIMAL(5,2) DEFAULT 0.00,
    memory_usage_percentage DECIMAL(5,2) DEFAULT 0.00,
    disk_usage_percentage DECIMAL(5,2) DEFAULT 0.00,
    network_io_bytes BIGINT DEFAULT 0,
    disk_io_bytes BIGINT DEFAULT 0,
    
    -- Database performance metrics
    database_connections_active INTEGER DEFAULT 0,
    database_connections_max INTEGER,
    database_query_time_avg DECIMAL(10,3), -- in milliseconds
    database_slow_queries INTEGER DEFAULT 0,
    database_deadlocks INTEGER DEFAULT 0,
    
    -- Cache performance metrics
    cache_hit_rate DECIMAL(5,4) DEFAULT 0.0000,
    cache_miss_rate DECIMAL(5,4) DEFAULT 0.0000,
    cache_evictions INTEGER DEFAULT 0,
    cache_memory_usage_mb INTEGER DEFAULT 0,
    
    -- AI/ML service metrics
    ai_requests_total INTEGER DEFAULT 0 NOT NULL,
    ai_requests_successful INTEGER DEFAULT 0 NOT NULL,
    ai_requests_failed INTEGER DEFAULT 0 NOT NULL,
    ai_response_time_avg DECIMAL(10,3), -- in milliseconds
    ai_tokens_consumed INTEGER DEFAULT 0,
    ai_cost_total DECIMAL(12,6) DEFAULT 0.000000,
    
    -- Queue and background job metrics
    queue_size INTEGER DEFAULT 0,
    queue_processing_time_avg DECIMAL(10,3), -- in milliseconds
    background_jobs_completed INTEGER DEFAULT 0,
    background_jobs_failed INTEGER DEFAULT 0,
    
    -- Security metrics
    security_events INTEGER DEFAULT 0,
    failed_login_attempts INTEGER DEFAULT 0,
    blocked_requests INTEGER DEFAULT 0,
    suspicious_activities INTEGER DEFAULT 0,
    
    -- Business metrics
    active_users INTEGER DEFAULT 0,
    new_registrations INTEGER DEFAULT 0,
    content_generations INTEGER DEFAULT 0,
    revenue_generated DECIMAL(12,2) DEFAULT 0.00,
    
    -- Alert and threshold information
    alerts_triggered INTEGER DEFAULT 0,
    critical_alerts INTEGER DEFAULT 0,
    warning_alerts INTEGER DEFAULT 0,
    threshold_breaches JSONB DEFAULT '[]',
    
    -- Time period for metrics
    measurement_period VARCHAR(20) DEFAULT 'MINUTE' NOT NULL,
    period_start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Data collection metadata
    collection_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    data_source VARCHAR(50) DEFAULT 'MONITORING_SYSTEM' NOT NULL,
    collection_method VARCHAR(50) DEFAULT 'AUTOMATIC' NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_system_metrics_service_name_not_empty 
        CHECK (LENGTH(TRIM(service_name)) > 0),
    CONSTRAINT chk_system_metrics_counts_non_negative 
        CHECK (
            requests_total >= 0 AND successful_requests >= 0 AND failed_requests >= 0 AND
            uptime_seconds >= 0 AND downtime_seconds >= 0 AND
            database_connections_active >= 0 AND database_slow_queries >= 0 AND
            database_deadlocks >= 0 AND cache_evictions >= 0 AND cache_memory_usage_mb >= 0 AND
            ai_requests_total >= 0 AND ai_requests_successful >= 0 AND ai_requests_failed >= 0 AND
            ai_tokens_consumed >= 0 AND queue_size >= 0 AND background_jobs_completed >= 0 AND
            background_jobs_failed >= 0 AND security_events >= 0 AND failed_login_attempts >= 0 AND
            blocked_requests >= 0 AND suspicious_activities >= 0 AND active_users >= 0 AND
            new_registrations >= 0 AND content_generations >= 0 AND alerts_triggered >= 0 AND
            critical_alerts >= 0 AND warning_alerts >= 0
        ),
    CONSTRAINT chk_system_metrics_percentages_valid 
        CHECK (
            cpu_usage_percentage >= 0 AND cpu_usage_percentage <= 100 AND
            memory_usage_percentage >= 0 AND memory_usage_percentage <= 100 AND
            disk_usage_percentage >= 0 AND disk_usage_percentage <= 100 AND
            availability_percentage >= 0 AND availability_percentage <= 100
        ),
    CONSTRAINT chk_system_metrics_rates_valid 
        CHECK (
            error_rate >= 0 AND error_rate <= 1 AND
            success_rate >= 0 AND success_rate <= 1 AND
            cache_hit_rate >= 0 AND cache_hit_rate <= 1 AND
            cache_miss_rate >= 0 AND cache_miss_rate <= 1 AND
            requests_per_second >= 0
        ),
    CONSTRAINT chk_system_metrics_time_periods_logical 
        CHECK (period_end_time >= period_start_time),
    CONSTRAINT chk_system_metrics_measurement_period_valid 
        CHECK (measurement_period IN ('SECOND', 'MINUTE', 'HOUR', 'DAY')),
    CONSTRAINT chk_system_metrics_environment_valid 
        CHECK (environment IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION', 'TEST')),
    CONSTRAINT chk_system_metrics_data_source_valid 
        CHECK (data_source IN ('MONITORING_SYSTEM', 'APPLICATION', 'INFRASTRUCTURE', 'MANUAL')),
    CONSTRAINT chk_system_metrics_collection_method_valid 
        CHECK (collection_method IN ('AUTOMATIC', 'MANUAL', 'SCHEDULED', 'REAL_TIME')),
    CONSTRAINT chk_system_metrics_requests_logical 
        CHECK (requests_total = successful_requests + failed_requests),
    CONSTRAINT chk_system_metrics_ai_requests_logical 
        CHECK (ai_requests_total = ai_requests_successful + ai_requests_failed),
    CONSTRAINT chk_system_metrics_database_connections_logical 
        CHECK (database_connections_max IS NULL OR database_connections_active <= database_connections_max)
);

-- Create audit_logs table for comprehensive system auditing
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    
    -- Audit event identification
    event_id VARCHAR(100) UNIQUE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    event_description TEXT NOT NULL,
    
    -- User and session information
    user_id BIGINT,
    username VARCHAR(100),
    user_role VARCHAR(50),
    session_id VARCHAR(100),
    impersonated_by BIGINT,
    
    -- Resource and action details
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    resource_name VARCHAR(500),
    action_performed VARCHAR(100) NOT NULL,
    action_result VARCHAR(20) DEFAULT 'SUCCESS' NOT NULL,
    
    -- Request and response information
    request_method VARCHAR(10),
    request_url TEXT,
    request_headers JSONB DEFAULT '{}',
    request_body JSONB DEFAULT '{}',
    response_status_code INTEGER,
    response_body JSONB DEFAULT '{}',
    
    -- Data changes tracking
    old_values JSONB DEFAULT '{}',
    new_values JSONB DEFAULT '{}',
    changed_fields TEXT[] DEFAULT '{}',
    data_sensitivity_level VARCHAR(20) DEFAULT 'PUBLIC',
    
    -- Security and compliance information
    ip_address INET,
    user_agent TEXT,
    geographic_location JSONB DEFAULT '{}',
    security_context JSONB DEFAULT '{}',
    compliance_tags TEXT[] DEFAULT '{}',
    
    -- Risk and impact assessment
    risk_level VARCHAR(20) DEFAULT 'LOW' NOT NULL,
    impact_level VARCHAR(20) DEFAULT 'LOW' NOT NULL,
    business_impact TEXT,
    security_implications TEXT,
    
    -- Error and exception details
    error_code VARCHAR(50),
    error_message TEXT,
    stack_trace TEXT,
    exception_type VARCHAR(100),
    
    -- Performance and timing information
    processing_time_ms BIGINT,
    database_queries_count INTEGER DEFAULT 0,
    external_api_calls_count INTEGER DEFAULT 0,
    
    -- Correlation and tracing
    correlation_id VARCHAR(100),
    trace_id VARCHAR(100),
    parent_event_id VARCHAR(100),
    related_events JSONB DEFAULT '[]',
    
    -- Workflow and approval information
    workflow_id VARCHAR(100),
    approval_status VARCHAR(20),
    approved_by BIGINT,
    approval_timestamp TIMESTAMP WITH TIME ZONE,
    approval_comments TEXT,
    
    -- Data retention and archival
    retention_policy VARCHAR(50) DEFAULT 'STANDARD' NOT NULL,
    archive_date TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Compliance and regulatory information
    gdpr_relevant BOOLEAN DEFAULT FALSE NOT NULL,
    pii_involved BOOLEAN DEFAULT FALSE NOT NULL,
    regulatory_requirements TEXT[] DEFAULT '{}',
    data_classification VARCHAR(30) DEFAULT 'INTERNAL',
    
    -- Alert and notification information
    alert_triggered BOOLEAN DEFAULT FALSE NOT NULL,
    notification_sent BOOLEAN DEFAULT FALSE NOT NULL,
    escalation_level INTEGER DEFAULT 0,
    
    -- Timestamp information
    event_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_audit_logs_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_impersonated_by 
        FOREIGN KEY (impersonated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_approved_by 
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_audit_logs_event_id_not_empty 
        CHECK (LENGTH(TRIM(event_id)) > 0),
    CONSTRAINT chk_audit_logs_event_type_not_empty 
        CHECK (LENGTH(TRIM(event_type)) > 0),
    CONSTRAINT chk_audit_logs_event_category_not_empty 
        CHECK (LENGTH(TRIM(event_category)) > 0),
    CONSTRAINT chk_audit_logs_event_description_not_empty 
        CHECK (LENGTH(TRIM(event_description)) > 0),
    CONSTRAINT chk_audit_logs_action_performed_not_empty 
        CHECK (LENGTH(TRIM(action_performed)) > 0),
    CONSTRAINT chk_audit_logs_action_result_valid 
        CHECK (action_result IN ('SUCCESS', 'FAILURE', 'PARTIAL', 'CANCELLED', 'PENDING')),
    CONSTRAINT chk_audit_logs_risk_level_valid 
        CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_audit_logs_impact_level_valid 
        CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_audit_logs_data_sensitivity_level_valid 
        CHECK (data_sensitivity_level IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED')),
    CONSTRAINT chk_audit_logs_retention_policy_valid 
        CHECK (retention_policy IN ('MINIMAL', 'STANDARD', 'EXTENDED', 'PERMANENT')),
    CONSTRAINT chk_audit_logs_data_classification_valid 
        CHECK (data_classification IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED', 'TOP_SECRET')),
    CONSTRAINT chk_audit_logs_approval_status_valid 
        CHECK (approval_status IS NULL OR approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_audit_logs_request_method_valid 
        CHECK (request_method IS NULL OR request_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS')),
    CONSTRAINT chk_audit_logs_response_status_code_valid 
        CHECK (response_status_code IS NULL OR (response_status_code >= 100 AND response_status_code <= 599)),
    CONSTRAINT chk_audit_logs_counts_non_negative 
        CHECK (
            database_queries_count >= 0 AND external_api_calls_count >= 0 AND
            escalation_level >= 0
        ),
    CONSTRAINT chk_audit_logs_processing_time_non_negative 
        CHECK (processing_time_ms IS NULL OR processing_time_ms >= 0),
    CONSTRAINT chk_audit_logs_archive_consistency 
        CHECK (
            (is_archived = TRUE AND archive_date IS NOT NULL) OR 
            (is_archived = FALSE AND archive_date IS NULL)
        ),
    CONSTRAINT chk_audit_logs_approval_consistency 
        CHECK (
            (approval_status = 'APPROVED' AND approved_by IS NOT NULL AND approval_timestamp IS NOT NULL) OR
            (approval_status != 'APPROVED' OR approval_status IS NULL)
        )
);

-- Create indexes for performance optimization

-- Content Performance Indexes
CREATE INDEX idx_content_performance_content_id ON content_performance(content_id);
CREATE INDEX idx_content_performance_version_number ON content_performance(version_number) WHERE version_number IS NOT NULL;
CREATE INDEX idx_content_performance_period_start_date ON content_performance(period_start_date);
CREATE INDEX idx_content_performance_period_end_date ON content_performance(period_end_date);
CREATE INDEX idx_content_performance_measurement_period ON content_performance(measurement_period);
CREATE INDEX idx_content_performance_data_source ON content_performance(data_source);
CREATE INDEX idx_content_performance_performance_trend ON content_performance(performance_trend);
CREATE INDEX idx_content_performance_engagement_rate ON content_performance(engagement_rate DESC);
CREATE INDEX idx_content_performance_conversion_rate ON content_performance(conversion_rate DESC);
CREATE INDEX idx_content_performance_view_count ON content_performance(view_count DESC);
CREATE INDEX idx_content_performance_revenue_generated ON content_performance(revenue_generated DESC);

-- Composite indexes for content performance
CREATE INDEX idx_content_performance_content_period ON content_performance(content_id, period_start_date DESC);
CREATE INDEX idx_content_performance_period_range ON content_performance(period_start_date, period_end_date);
CREATE INDEX idx_content_performance_content_version ON content_performance(content_id, version_number) WHERE version_number IS NOT NULL;

-- JSONB indexes for content performance
CREATE INDEX idx_content_performance_top_countries ON content_performance USING GIN(top_countries);
CREATE INDEX idx_content_performance_device_breakdown ON content_performance USING GIN(device_breakdown);
CREATE INDEX idx_content_performance_age_demographics ON content_performance USING GIN(age_demographics);

-- User Behavior Analytics Indexes
CREATE INDEX idx_user_behavior_analytics_user_id ON user_behavior_analytics(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_session_id ON user_behavior_analytics(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_user_segment ON user_behavior_analytics(user_segment) WHERE user_segment IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_user_type ON user_behavior_analytics(user_type);
CREATE INDEX idx_user_behavior_analytics_period_start_date ON user_behavior_analytics(period_start_date);
CREATE INDEX idx_user_behavior_analytics_measurement_period ON user_behavior_analytics(measurement_period);
CREATE INDEX idx_user_behavior_analytics_funnel_stage ON user_behavior_analytics(funnel_stage) WHERE funnel_stage IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_engagement_score ON user_behavior_analytics(feature_adoption_score DESC);
CREATE INDEX idx_user_behavior_analytics_satisfaction_rating ON user_behavior_analytics(user_satisfaction_rating DESC) WHERE user_satisfaction_rating IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_country_code ON user_behavior_analytics(country_code) WHERE country_code IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_device_type ON user_behavior_analytics(device_type) WHERE device_type IS NOT NULL;

-- Composite indexes for user behavior analytics
CREATE INDEX idx_user_behavior_analytics_user_period ON user_behavior_analytics(user_id, period_start_date DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_segment_period ON user_behavior_analytics(user_segment, period_start_date DESC) WHERE user_segment IS NOT NULL;
CREATE INDEX idx_user_behavior_analytics_type_period ON user_behavior_analytics(user_type, period_start_date DESC);

-- JSONB indexes for user behavior analytics
CREATE INDEX idx_user_behavior_analytics_features_used ON user_behavior_analytics USING GIN(features_used);
CREATE INDEX idx_user_behavior_analytics_conversion_events ON user_behavior_analytics USING GIN(conversion_events);
CREATE INDEX idx_user_behavior_analytics_preferred_content_types ON user_behavior_analytics USING GIN(preferred_content_types);
CREATE INDEX idx_user_behavior_analytics_interaction_patterns ON user_behavior_analytics USING GIN(interaction_patterns);

-- System Metrics Indexes
CREATE INDEX idx_system_metrics_service_name ON system_metrics(service_name);
CREATE INDEX idx_system_metrics_instance_id ON system_metrics(instance_id) WHERE instance_id IS NOT NULL;
CREATE INDEX idx_system_metrics_environment ON system_metrics(environment);
CREATE INDEX idx_system_metrics_period_start_time ON system_metrics(period_start_time);
CREATE INDEX idx_system_metrics_period_end_time ON system_metrics(period_end_time);
CREATE INDEX idx_system_metrics_measurement_period ON system_metrics(measurement_period);
CREATE INDEX idx_system_metrics_collection_timestamp ON system_metrics(collection_timestamp);
CREATE INDEX idx_system_metrics_response_time_avg ON system_metrics(response_time_avg) WHERE response_time_avg IS NOT NULL;
CREATE INDEX idx_system_metrics_error_rate ON system_metrics(error_rate DESC);
CREATE INDEX idx_system_metrics_cpu_usage_percentage ON system_metrics(cpu_usage_percentage DESC);
CREATE INDEX idx_system_metrics_memory_usage_percentage ON system_metrics(memory_usage_percentage DESC);
CREATE INDEX idx_system_metrics_availability_percentage ON system_metrics(availability_percentage);
CREATE INDEX idx_system_metrics_alerts_triggered ON system_metrics(alerts_triggered DESC) WHERE alerts_triggered > 0;

-- Composite indexes for system metrics
CREATE INDEX idx_system_metrics_service_time ON system_metrics(service_name, period_start_time DESC);
CREATE INDEX idx_system_metrics_service_environment ON system_metrics(service_name, environment);
CREATE INDEX idx_system_metrics_environment_time ON system_metrics(environment, period_start_time DESC);
CREATE INDEX idx_system_metrics_time_range ON system_metrics(period_start_time, period_end_time);

-- JSONB indexes for system metrics
CREATE INDEX idx_system_metrics_threshold_breaches ON system_metrics USING GIN(threshold_breaches);

-- Audit Logs Indexes
CREATE INDEX idx_audit_logs_event_id ON audit_logs(event_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_event_category ON audit_logs(event_category);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_logs_username ON audit_logs(username) WHERE username IS NOT NULL;
CREATE INDEX idx_audit_logs_session_id ON audit_logs(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type) WHERE resource_type IS NOT NULL;
CREATE INDEX idx_audit_logs_resource_id ON audit_logs(resource_id) WHERE resource_id IS NOT NULL;
CREATE INDEX idx_audit_logs_action_performed ON audit_logs(action_performed);
CREATE INDEX idx_audit_logs_action_result ON audit_logs(action_result);
CREATE INDEX idx_audit_logs_event_timestamp ON audit_logs(event_timestamp);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_risk_level ON audit_logs(risk_level);
CREATE INDEX idx_audit_logs_impact_level ON audit_logs(impact_level);
CREATE INDEX idx_audit_logs_ip_address ON audit_logs(ip_address) WHERE ip_address IS NOT NULL;
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_audit_logs_trace_id ON audit_logs(trace_id) WHERE trace_id IS NOT NULL;
CREATE INDEX idx_audit_logs_parent_event_id ON audit_logs(parent_event_id) WHERE parent_event_id IS NOT NULL;
CREATE INDEX idx_audit_logs_is_archived ON audit_logs(is_archived);
CREATE INDEX idx_audit_logs_gdpr_relevant ON audit_logs(gdpr_relevant) WHERE gdpr_relevant = TRUE;
CREATE INDEX idx_audit_logs_pii_involved ON audit_logs(pii_involved) WHERE pii_involved = TRUE;
CREATE INDEX idx_audit_logs_alert_triggered ON audit_logs(alert_triggered) WHERE alert_triggered = TRUE;
CREATE INDEX idx_audit_logs_data_classification ON audit_logs(data_classification);

-- Composite indexes for audit logs
CREATE INDEX idx_audit_logs_user_timestamp ON audit_logs(user_id, event_timestamp DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_logs_type_timestamp ON audit_logs(event_type, event_timestamp DESC);
CREATE INDEX idx_audit_logs_category_timestamp ON audit_logs(event_category, event_timestamp DESC);
CREATE INDEX idx_audit_logs_resource_timestamp ON audit_logs(resource_type, resource_id, event_timestamp DESC) WHERE resource_type IS NOT NULL AND resource_id IS NOT NULL;
CREATE INDEX idx_audit_logs_action_timestamp ON audit_logs(action_performed, event_timestamp DESC);
CREATE INDEX idx_audit_logs_result_timestamp ON audit_logs(action_result, event_timestamp DESC);
CREATE INDEX idx_audit_logs_risk_timestamp ON audit_logs(risk_level, event_timestamp DESC);
CREATE INDEX idx_audit_logs_session_timestamp ON audit_logs(session_id, event_timestamp DESC) WHERE session_id IS NOT NULL;

-- JSONB indexes for audit logs
CREATE INDEX idx_audit_logs_request_headers ON audit_logs USING GIN(request_headers);
CREATE INDEX idx_audit_logs_request_body ON audit_logs USING GIN(request_body);
CREATE INDEX idx_audit_logs_response_body ON audit_logs USING GIN(response_body);
CREATE INDEX idx_audit_logs_old_values ON audit_logs USING GIN(old_values);
CREATE INDEX idx_audit_logs_new_values ON audit_logs USING GIN(new_values);
CREATE INDEX idx_audit_logs_geographic_location ON audit_logs USING GIN(geographic_location);
CREATE INDEX idx_audit_logs_security_context ON audit_logs USING GIN(security_context);
CREATE INDEX idx_audit_logs_related_events ON audit_logs USING GIN(related_events);

-- Array indexes for audit logs
CREATE INDEX idx_audit_logs_changed_fields ON audit_logs USING GIN(changed_fields);
CREATE INDEX idx_audit_logs_compliance_tags ON audit_logs USING GIN(compliance_tags);
CREATE INDEX idx_audit_logs_regulatory_requirements ON audit_logs USING GIN(regulatory_requirements);

-- Full-text search indexes for audit logs
CREATE INDEX idx_audit_logs_event_description_search ON audit_logs USING GIN(to_tsvector('english', event_description));
CREATE INDEX idx_audit_logs_business_impact_search ON audit_logs USING GIN(to_tsvector('english', COALESCE(business_impact, '')));

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$ language 'plpgsql';

-- Apply triggers to tables with updated_at columns
CREATE TRIGGER update_content_performance_updated_at 
    BEFORE UPDATE ON content_performance 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_behavior_analytics_updated_at 
    BEFORE UPDATE ON user_behavior_analytics 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_audit_logs_updated_at 
    BEFORE UPDATE ON audit_logs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to generate unique event IDs for audit logs
CREATE OR REPLACE FUNCTION generate_audit_event_id()
RETURNS TRIGGER AS $
BEGIN
    IF NEW.event_id IS NULL OR NEW.event_id = '' THEN
        NEW.event_id := 'AUD_' || EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT || '_' || 
                       LPAD(nextval('audit_logs_id_seq')::TEXT, 8, '0');
    END IF;
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create trigger to automatically generate event IDs
CREATE TRIGGER generate_audit_event_id_trigger
    BEFORE INSERT ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION generate_audit_event_id();

-- Create function to log audit events
CREATE OR REPLACE FUNCTION log_audit_event(
    p_event_type VARCHAR(50),
    p_event_category VARCHAR(50),
    p_event_description TEXT,
    p_user_id BIGINT DEFAULT NULL,
    p_username VARCHAR(100) DEFAULT NULL,
    p_resource_type VARCHAR(50) DEFAULT NULL,
    p_resource_id VARCHAR(100) DEFAULT NULL,
    p_resource_name VARCHAR(500) DEFAULT NULL,
    p_action_performed VARCHAR(100) DEFAULT 'UNKNOWN',
    p_action_result VARCHAR(20) DEFAULT 'SUCCESS',
    p_old_values JSONB DEFAULT '{}',
    p_new_values JSONB DEFAULT '{}',
    p_risk_level VARCHAR(20) DEFAULT 'LOW',
    p_impact_level VARCHAR(20) DEFAULT 'LOW',
    p_ip_address INET DEFAULT NULL,
    p_user_agent TEXT DEFAULT NULL,
    p_session_id VARCHAR(100) DEFAULT NULL,
    p_correlation_id VARCHAR(100) DEFAULT NULL
)
RETURNS BIGINT AS $
DECLARE
    audit_log_id BIGINT;
BEGIN
    INSERT INTO audit_logs (
        event_type,
        event_category,
        event_description,
        user_id,
        username,
        resource_type,
        resource_id,
        resource_name,
        action_performed,
        action_result,
        old_values,
        new_values,
        risk_level,
        impact_level,
        ip_address,
        user_agent,
        session_id,
        correlation_id
    ) VALUES (
        p_event_type,
        p_event_category,
        p_event_description,
        p_user_id,
        p_username,
        p_resource_type,
        p_resource_id,
        p_resource_name,
        p_action_performed,
        p_action_result,
        p_old_values,
        p_new_values,
        p_risk_level,
        p_impact_level,
        p_ip_address,
        p_user_agent,
        p_session_id,
        p_correlation_id
    ) RETURNING id INTO audit_log_id;
    
    RETURN audit_log_id;
END;
$ LANGUAGE plpgsql;

-- Create function to calculate content performance scores
CREATE OR REPLACE FUNCTION calculate_content_performance_score(
    p_content_id BIGINT,
    p_period_start TIMESTAMP WITH TIME ZONE,
    p_period_end TIMESTAMP WITH TIME ZONE
)
RETURNS DECIMAL(5,2) AS $
DECLARE
    performance_score DECIMAL(5,2) := 0.00;
    engagement_weight DECIMAL(3,2) := 0.30;
    conversion_weight DECIMAL(3,2) := 0.25;
    quality_weight DECIMAL(3,2) := 0.20;
    social_weight DECIMAL(3,2) := 0.15;
    seo_weight DECIMAL(3,2) := 0.10;
    
    avg_engagement DECIMAL(5,4);
    avg_conversion DECIMAL(5,4);
    avg_quality DECIMAL(5,2);
    avg_social DECIMAL(8,4);
    avg_seo DECIMAL(5,2);
BEGIN
    -- Calculate average metrics for the period
    SELECT 
        AVG(engagement_rate),
        AVG(conversion_rate),
        AVG(content_quality_score),
        AVG(social_engagement_score),
        AVG(seo_score)
    INTO 
        avg_engagement,
        avg_conversion,
        avg_quality,
        avg_social,
        avg_seo
    FROM content_performance
    WHERE content_id = p_content_id
      AND period_start_date >= p_period_start
      AND period_end_date <= p_period_end;
    
    -- Calculate weighted performance score
    performance_score := 
        (COALESCE(avg_engagement, 0) * 100 * engagement_weight) +
        (COALESCE(avg_conversion, 0) * 100 * conversion_weight) +
        (COALESCE(avg_quality, 0) * quality_weight) +
        (COALESCE(avg_social, 0) * social_weight) +
        (COALESCE(avg_seo, 0) * seo_weight);
    
    RETURN LEAST(performance_score, 100.00);
END;
$ LANGUAGE plpgsql;

-- Create function to archive old audit logs
CREATE OR REPLACE FUNCTION archive_old_audit_logs(
    p_retention_days INTEGER DEFAULT 365
)
RETURNS INTEGER AS $
DECLARE
    archived_count INTEGER;
    cutoff_date TIMESTAMP WITH TIME ZONE;
BEGIN
    cutoff_date := CURRENT_TIMESTAMP - (p_retention_days || ' days')::INTERVAL;
    
    -- Archive logs older than retention period (except permanent retention)
    UPDATE audit_logs 
    SET 
        is_archived = TRUE,
        archive_date = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE event_timestamp < cutoff_date
      AND retention_policy != 'PERMANENT'
      AND is_archived = FALSE;
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    RETURN archived_count;
END;
$ LANGUAGE plpgsql;

-- Create views for analytics and reporting

-- View for content performance summary
CREATE VIEW v_content_performance_summary AS
SELECT 
    cp.content_id,
    cg.title as content_title,
    cg.content_type,
    cg.industry,
    COUNT(*) as measurement_count,
    AVG(cp.view_count) as avg_view_count,
    AVG(cp.engagement_rate) as avg_engagement_rate,
    AVG(cp.conversion_rate) as avg_conversion_rate,
    AVG(cp.content_quality_score) as avg_quality_score,
    SUM(cp.revenue_generated) as total_revenue,
    MAX(cp.period_end_date) as latest_measurement,
    -- Performance trend analysis
    CASE 
        WHEN AVG(CASE WHEN cp.period_start_date >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN cp.engagement_rate END) >
             AVG(CASE WHEN cp.period_start_date >= CURRENT_TIMESTAMP - INTERVAL '14 days' 
                      AND cp.period_start_date < CURRENT_TIMESTAMP - INTERVAL '7 days' THEN cp.engagement_rate END)
        THEN 'IMPROVING'
        WHEN AVG(CASE WHEN cp.period_start_date >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN cp.engagement_rate END) <
             AVG(CASE WHEN cp.period_start_date >= CURRENT_TIMESTAMP - INTERVAL '14 days' 
                      AND cp.period_start_date < CURRENT_TIMESTAMP - INTERVAL '7 days' THEN cp.engagement_rate END)
        THEN 'DECLINING'
        ELSE 'STABLE'
    END as performance_trend
FROM content_performance cp
JOIN content_generations cg ON cp.content_id = cg.id
GROUP BY cp.content_id, cg.title, cg.content_type, cg.industry
ORDER BY avg_engagement_rate DESC, total_revenue DESC;

-- View for user behavior insights
CREATE VIEW v_user_behavior_insights AS
SELECT 
    uba.user_id,
    u.username,
    uba.user_segment,
    uba.user_type,
    COUNT(*) as measurement_count,
    AVG(uba.session_duration) as avg_session_duration,
    AVG(uba.pages_per_session) as avg_pages_per_session,
    AVG(uba.feature_adoption_score) as avg_feature_adoption_score,
    SUM(uba.content_generated) as total_content_generated,
    AVG(uba.user_satisfaction_rating) as avg_satisfaction_rating,
    MAX(uba.period_end_date) as latest_measurement,
    -- User engagement level
    CASE 
        WHEN AVG(uba.feature_adoption_score) >= 80 THEN 'POWER_USER'
        WHEN AVG(uba.feature_adoption_score) >= 60 THEN 'ENGAGED'
        WHEN AVG(uba.feature_adoption_score) >= 40 THEN 'MODERATE'
        WHEN AVG(uba.feature_adoption_score) >= 20 THEN 'LOW'
        ELSE 'MINIMAL'
    END as engagement_level
FROM user_behavior_analytics uba
LEFT JOIN users u ON uba.user_id = u.id
WHERE uba.user_id IS NOT NULL
GROUP BY uba.user_id, u.username, uba.user_segment, uba.user_type
ORDER BY avg_feature_adoption_score DESC, total_content_generated DESC;

-- View for system performance overview
CREATE VIEW v_system_performance_overview AS
SELECT 
    sm.service_name,
    sm.environment,
    COUNT(*) as measurement_count,
    AVG(sm.response_time_avg) as avg_response_time,
    AVG(sm.error_rate) as avg_error_rate,
    AVG(sm.availability_percentage) as avg_availability,
    AVG(sm.cpu_usage_percentage) as avg_cpu_usage,
    AVG(sm.memory_usage_percentage) as avg_memory_usage,
    SUM(sm.requests_total) as total_requests,
    SUM(sm.failed_requests) as total_failed_requests,
    MAX(sm.period_end_time) as latest_measurement,
    -- System health status
    CASE 
        WHEN AVG(sm.availability_percentage) >= 99.9 AND AVG(sm.error_rate) <= 0.001 THEN 'EXCELLENT'
        WHEN AVG(sm.availability_percentage) >= 99.5 AND AVG(sm.error_rate) <= 0.01 THEN 'GOOD'
        WHEN AVG(sm.availability_percentage) >= 99.0 AND AVG(sm.error_rate) <= 0.05 THEN 'FAIR'
        ELSE 'POOR'
    END as health_status
FROM system_metrics sm
GROUP BY sm.service_name, sm.environment
ORDER BY avg_availability DESC, avg_error_rate ASC;

-- View for audit log summary
CREATE VIEW v_audit_log_summary AS
SELECT 
    al.event_category,
    al.event_type,
    al.risk_level,
    COUNT(*) as event_count,
    COUNT(DISTINCT al.user_id) as unique_users,
    COUNT(CASE WHEN al.action_result = 'SUCCESS' THEN 1 END) as successful_events,
    COUNT(CASE WHEN al.action_result = 'FAILURE' THEN 1 END) as failed_events,
    MIN(al.event_timestamp) as first_event,
    MAX(al.event_timestamp) as last_event,
    COUNT(CASE WHEN al.event_timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours' THEN 1 END) as events_last_24h,
    COUNT(CASE WHEN al.event_timestamp >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as events_last_7d,
    COUNT(CASE WHEN al.gdpr_relevant = TRUE THEN 1 END) as gdpr_relevant_events,
    COUNT(CASE WHEN al.pii_involved = TRUE THEN 1 END) as pii_involved_events,
    ROUND(
        (COUNT(CASE WHEN al.action_result = 'SUCCESS' THEN 1 END)::DECIMAL / COUNT(*)) * 100, 
        2
    ) as success_rate_percent
FROM audit_logs al
WHERE al.is_archived = FALSE
GROUP BY al.event_category, al.event_type, al.risk_level
ORDER BY event_count DESC, al.risk_level DESC;

-- Add comments for documentation
COMMENT ON TABLE content_performance IS 'Stores comprehensive content performance metrics and analytics data';
COMMENT ON TABLE user_behavior_analytics IS 'Stores detailed user behavior patterns and engagement analytics';
COMMENT ON TABLE system_metrics IS 'Stores system performance metrics and monitoring data';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all system activities and security events';

COMMENT ON COLUMN content_performance.top_countries IS 'JSONB array storing top performing countries for content';
COMMENT ON COLUMN content_performance.device_breakdown IS 'JSONB object storing device type performance breakdown';
COMMENT ON COLUMN user_behavior_analytics.features_used IS 'JSONB array storing list of features used by user';
COMMENT ON COLUMN user_behavior_analytics.interaction_patterns IS 'JSONB object storing user interaction patterns and preferences';
COMMENT ON COLUMN system_metrics.threshold_breaches IS 'JSONB array storing details of threshold breaches and alerts';
COMMENT ON COLUMN audit_logs.old_values IS 'JSONB object storing previous values before changes';
COMMENT ON COLUMN audit_logs.new_values IS 'JSONB object storing new values after changes';
COMMENT ON COLUMN audit_logs.security_context IS 'JSONB object storing security-related context and metadata';

COMMENT ON VIEW v_content_performance_summary IS 'Summary view of content performance with trend analysis';
COMMENT ON VIEW v_user_behavior_insights IS 'User behavior insights with engagement levels and activity patterns';
COMMENT ON VIEW v_system_performance_overview IS 'System performance overview with health status indicators';
COMMENT ON VIEW v_audit_log_summary IS 'Audit log summary with success rates and compliance metrics';