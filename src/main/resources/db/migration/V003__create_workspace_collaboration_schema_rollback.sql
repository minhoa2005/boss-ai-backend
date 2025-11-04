-- Workspace Collaboration Schema Rollback
-- This rollback script removes all workspace collaboration tables and related objects
-- Requirements: 5.1, 5.2, 5.3

-- Drop views first (they depend on tables)
DROP VIEW IF EXISTS v_workspace_activity_summary;
DROP VIEW IF EXISTS v_workspace_content_analytics;
DROP VIEW IF EXISTS v_workspace_member_analytics;
DROP VIEW IF EXISTS v_workspace_overview;

-- Drop triggers (they depend on functions and tables)
DROP TRIGGER IF EXISTS validate_workspace_limits_trigger ON workspaces;
DROP TRIGGER IF EXISTS update_workspace_content_count_trigger ON workspace_content_shares;
DROP TRIGGER IF EXISTS update_workspace_member_count_trigger ON workspace_members;
DROP TRIGGER IF EXISTS update_workspace_content_shares_updated_at ON workspace_content_shares;
DROP TRIGGER IF EXISTS update_workspace_members_updated_at ON workspace_members;
DROP TRIGGER IF EXISTS update_workspaces_updated_at ON workspaces;

-- Drop functions
DROP FUNCTION IF EXISTS reset_monthly_usage_counters();
DROP FUNCTION IF EXISTS cleanup_expired_invitations();
DROP FUNCTION IF EXISTS validate_workspace_limits();
DROP FUNCTION IF EXISTS log_workspace_activity(BIGINT, BIGINT, VARCHAR(50), VARCHAR(50), TEXT, VARCHAR(50), BIGINT, VARCHAR(500), JSONB, VARCHAR(20), VARCHAR(20));
DROP FUNCTION IF EXISTS update_workspace_content_count();
DROP FUNCTION IF EXISTS update_workspace_member_count();

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS workspace_activity_logs;
DROP TABLE IF EXISTS workspace_content_shares;
DROP TABLE IF EXISTS workspace_members;
DROP TABLE IF EXISTS workspaces;

-- Note: We don't drop the update_updated_at_column() function as it's shared with other migrations