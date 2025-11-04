-- Rollback script for Templates and Presets Schema Migration
-- This script safely removes all templates and presets related tables and objects
-- Run this script if you need to rollback the V002 migration

-- Drop views first (they depend on tables)
DROP VIEW IF EXISTS v_template_usage_analytics;
DROP VIEW IF EXISTS v_user_preset_analytics;
DROP VIEW IF EXISTS v_popular_templates;

-- Drop triggers (they depend on functions and tables)
DROP TRIGGER IF EXISTS ensure_single_default_preset_trigger ON user_presets;
DROP TRIGGER IF EXISTS validate_template_config_on_insert_update ON content_templates;
DROP TRIGGER IF EXISTS update_template_stats_on_usage ON template_usage_logs;
DROP TRIGGER IF EXISTS update_user_presets_updated_at ON user_presets;
DROP TRIGGER IF EXISTS update_content_templates_updated_at ON content_templates;

-- Drop functions (they may be referenced by triggers)
DROP FUNCTION IF EXISTS ensure_single_default_preset();
DROP FUNCTION IF EXISTS validate_template_configuration();
DROP FUNCTION IF EXISTS update_preset_usage_stats();
DROP FUNCTION IF EXISTS update_template_usage_stats();

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS template_usage_logs;
DROP TABLE IF EXISTS user_presets;
DROP TABLE IF EXISTS content_templates;

-- Note: We don't drop the update_updated_at_column() function as it's shared
-- with other tables from V001 migration. Only drop if rolling back all migrations.

-- Verification queries to confirm rollback
SELECT 'Templates and presets schema rollback completed successfully' as status;

-- Check that tables no longer exist
SELECT 
    CASE 
        WHEN NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'content_templates') AND
             NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_presets') AND
             NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'template_usage_logs')
        THEN 'All templates and presets tables successfully removed'
        ELSE 'WARNING: Some tables may still exist'
    END as verification_result;

-- Check that views no longer exist
SELECT 
    CASE 
        WHEN NOT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'v_popular_templates') AND
             NOT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'v_user_preset_analytics') AND
             NOT EXISTS (SELECT 1 FROM information_schema.views WHERE table_name = 'v_template_usage_analytics')
        THEN 'All templates and presets views successfully removed'
        ELSE 'WARNING: Some views may still exist'
    END as view_verification_result;