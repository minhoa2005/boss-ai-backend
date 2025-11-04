-- Rollback script for Content Versioning Schema Migration
-- This script safely removes all content versioning tables and related objects
-- Use this script to rollback V001__create_content_versioning_schema.sql

-- Drop views first (dependent objects)
DROP VIEW IF EXISTS v_content_comparison_analytics;
DROP VIEW IF EXISTS v_content_version_history;

-- Drop triggers
DROP TRIGGER IF EXISTS validate_content_version_comparisons_versions ON content_version_comparisons;
DROP TRIGGER IF EXISTS update_content_version_comparisons_updated_at ON content_version_comparisons;
DROP TRIGGER IF EXISTS update_content_versions_updated_at ON content_versions;

-- Drop functions
DROP FUNCTION IF EXISTS validate_comparison_versions();
DROP FUNCTION IF EXISTS validate_version_exists(BIGINT, INTEGER);
DROP FUNCTION IF EXISTS get_next_version_number(BIGINT);
DROP FUNCTION IF EXISTS update_updated_at_column();

-- Drop tables (in reverse dependency order)
DROP TABLE IF EXISTS content_version_comparisons;
DROP TABLE IF EXISTS content_versions;

-- Note: We don't drop the content_generations table as it's part of the existing schema
-- Note: We don't drop the users table as it's part of the existing schema

-- Verification queries to ensure clean rollback
-- These should return 0 rows after successful rollback
-- SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('content_versions', 'content_version_comparisons');
-- SELECT COUNT(*) FROM information_schema.views WHERE table_name IN ('v_content_version_history', 'v_content_comparison_analytics');
-- SELECT COUNT(*) FROM information_schema.routines WHERE routine_name IN ('get_next_version_number', 'validate_version_exists', 'validate_comparison_versions', 'update_updated_at_column');