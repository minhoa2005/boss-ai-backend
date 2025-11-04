-- Analytics and Audit Schema Rollback Migration
-- This rollback script removes the analytics and audit tables created in V004
-- Requirements: 4.1, 4.2, 7.1, 7.2

-- Drop views first (due to dependencies)
DROP VIEW IF EXISTS v_audit_log_summary;
DROP VIEW IF EXISTS v_system_performance_overview;
DROP VIEW IF EXISTS v_user_behavior_insights;
DROP VIEW IF EXISTS v_content_performance_summary;

-- Drop functions
DROP FUNCTION IF EXISTS archive_old_audit_logs(INTEGER);
DROP FUNCTION IF EXISTS calculate_content_performance_score(BIGINT, TIMESTAMP WITH TIME ZONE, TIMESTAMP WITH TIME ZONE);
DROP FUNCTION IF EXISTS log_audit_event(VARCHAR(50), VARCHAR(50), TEXT, BIGINT, VARCHAR(100), VARCHAR(50), VARCHAR(100), VARCHAR(500), VARCHAR(100), VARCHAR(20), JSONB, JSONB, VARCHAR(20), VARCHAR(20), INET, TEXT, VARCHAR(100), VARCHAR(100));
DROP FUNCTION IF EXISTS generate_audit_event_id();

-- Drop triggers
DROP TRIGGER IF EXISTS generate_audit_event_id_trigger ON audit_logs;
DROP TRIGGER IF EXISTS update_audit_logs_updated_at ON audit_logs;
DROP TRIGGER IF EXISTS update_user_behavior_analytics_updated_at ON user_behavior_analytics;
DROP TRIGGER IF EXISTS update_content_performance_updated_at ON content_performance;

-- Drop indexes for audit_logs
DROP INDEX IF EXISTS idx_audit_logs_regulatory_requirements;
DROP INDEX IF EXISTS idx_audit_logs_compliance_tags;
DROP INDEX IF EXISTS idx_audit_logs_changed_fields;
DROP INDEX IF EXISTS idx_audit_logs_related_events;
DROP INDEX IF EXISTS idx_audit_logs_security_context;
DROP INDEX IF EXISTS idx_audit_logs_geographic_location;
DROP INDEX IF EXISTS idx_audit_logs_new_values;
DROP INDEX IF EXISTS idx_audit_logs_old_values;
DROP INDEX IF EXISTS idx_audit_logs_response_body;
DROP INDEX IF EXISTS idx_audit_logs_request_body;
DROP INDEX IF EXISTS idx_audit_logs_request_headers;
DROP INDEX IF EXISTS idx_audit_logs_session_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_risk_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_result_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_action_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_resource_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_category_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_type_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_user_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_data_classification;
DROP INDEX IF EXISTS idx_audit_logs_alert_triggered;
DROP INDEX IF EXISTS idx_audit_logs_pii_involved;
DROP INDEX IF EXISTS idx_audit_logs_gdpr_relevant;
DROP INDEX IF EXISTS idx_audit_logs_is_archived;
DROP INDEX IF EXISTS idx_audit_logs_parent_event_id;
DROP INDEX IF EXISTS idx_audit_logs_trace_id;
DROP INDEX IF EXISTS idx_audit_logs_correlation_id;
DROP INDEX IF EXISTS idx_audit_logs_ip_address;
DROP INDEX IF EXISTS idx_audit_logs_impact_level;
DROP INDEX IF EXISTS idx_audit_logs_risk_level;
DROP INDEX IF EXISTS idx_audit_logs_created_at;
DROP INDEX IF EXISTS idx_audit_logs_event_timestamp;
DROP INDEX IF EXISTS idx_audit_logs_action_result;
DROP INDEX IF EXISTS idx_audit_logs_action_performed;
DROP INDEX IF EXISTS idx_audit_logs_resource_id;
DROP INDEX IF EXISTS idx_audit_logs_resource_type;
DROP INDEX IF EXISTS idx_audit_logs_session_id;
DROP INDEX IF EXISTS idx_audit_logs_username;
DROP INDEX IF EXISTS idx_audit_logs_user_id;
DROP INDEX IF EXISTS idx_audit_logs_event_category;
DROP INDEX IF EXISTS idx_audit_logs_event_type;
DROP INDEX IF EXISTS idx_audit_logs_event_id;
DROP INDEX IF EXISTS idx_audit_logs_business_impact_search;
DROP INDEX IF EXISTS idx_audit_logs_event_description_search;

-- Drop indexes for system_metrics
DROP INDEX IF EXISTS idx_system_metrics_threshold_breaches;
DROP INDEX IF EXISTS idx_system_metrics_time_range;
DROP INDEX IF EXISTS idx_system_metrics_environment_time;
DROP INDEX IF EXISTS idx_system_metrics_service_environment;
DROP INDEX IF EXISTS idx_system_metrics_service_time;
DROP INDEX IF EXISTS idx_system_metrics_alerts_triggered;
DROP INDEX IF EXISTS idx_system_metrics_availability_percentage;
DROP INDEX IF EXISTS idx_system_metrics_memory_usage_percentage;
DROP INDEX IF EXISTS idx_system_metrics_cpu_usage_percentage;
DROP INDEX IF EXISTS idx_system_metrics_error_rate;
DROP INDEX IF EXISTS idx_system_metrics_response_time_avg;
DROP INDEX IF EXISTS idx_system_metrics_collection_timestamp;
DROP INDEX IF EXISTS idx_system_metrics_measurement_period;
DROP INDEX IF EXISTS idx_system_metrics_period_end_time;
DROP INDEX IF EXISTS idx_system_metrics_period_start_time;
DROP INDEX IF EXISTS idx_system_metrics_environment;
DROP INDEX IF EXISTS idx_system_metrics_instance_id;
DROP INDEX IF EXISTS idx_system_metrics_service_name;

-- Drop indexes for user_behavior_analytics
DROP INDEX IF EXISTS idx_user_behavior_analytics_interaction_patterns;
DROP INDEX IF EXISTS idx_user_behavior_analytics_preferred_content_types;
DROP INDEX IF EXISTS idx_user_behavior_analytics_conversion_events;
DROP INDEX IF EXISTS idx_user_behavior_analytics_features_used;
DROP INDEX IF EXISTS idx_user_behavior_analytics_type_period;
DROP INDEX IF EXISTS idx_user_behavior_analytics_segment_period;
DROP INDEX IF EXISTS idx_user_behavior_analytics_user_period;
DROP INDEX IF EXISTS idx_user_behavior_analytics_device_type;
DROP INDEX IF EXISTS idx_user_behavior_analytics_country_code;
DROP INDEX IF EXISTS idx_user_behavior_analytics_satisfaction_rating;
DROP INDEX IF EXISTS idx_user_behavior_analytics_engagement_score;
DROP INDEX IF EXISTS idx_user_behavior_analytics_funnel_stage;
DROP INDEX IF EXISTS idx_user_behavior_analytics_measurement_period;
DROP INDEX IF EXISTS idx_user_behavior_analytics_period_start_date;
DROP INDEX IF EXISTS idx_user_behavior_analytics_user_type;
DROP INDEX IF EXISTS idx_user_behavior_analytics_user_segment;
DROP INDEX IF EXISTS idx_user_behavior_analytics_session_id;
DROP INDEX IF EXISTS idx_user_behavior_analytics_user_id;

-- Drop indexes for content_performance
DROP INDEX IF EXISTS idx_content_performance_age_demographics;
DROP INDEX IF EXISTS idx_content_performance_device_breakdown;
DROP INDEX IF EXISTS idx_content_performance_top_countries;
DROP INDEX IF EXISTS idx_content_performance_content_version;
DROP INDEX IF EXISTS idx_content_performance_period_range;
DROP INDEX IF EXISTS idx_content_performance_content_period;
DROP INDEX IF EXISTS idx_content_performance_revenue_generated;
DROP INDEX IF EXISTS idx_content_performance_view_count;
DROP INDEX IF EXISTS idx_content_performance_conversion_rate;
DROP INDEX IF EXISTS idx_content_performance_engagement_rate;
DROP INDEX IF EXISTS idx_content_performance_performance_trend;
DROP INDEX IF EXISTS idx_content_performance_data_source;
DROP INDEX IF EXISTS idx_content_performance_measurement_period;
DROP INDEX IF EXISTS idx_content_performance_period_end_date;
DROP INDEX IF EXISTS idx_content_performance_period_start_date;
DROP INDEX IF EXISTS idx_content_performance_version_number;
DROP INDEX IF EXISTS idx_content_performance_content_id;

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS system_metrics;
DROP TABLE IF EXISTS user_behavior_analytics;
DROP TABLE IF EXISTS content_performance;

-- Note: The update_updated_at_column() function is shared with other migrations
-- so we don't drop it here to avoid breaking other tables that use it