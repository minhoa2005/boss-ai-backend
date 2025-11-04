-- Workspace Collaboration Schema Migration
-- This migration creates the workspace collaboration tables for Business Enhancement Phase 1
-- Requirements: 5.1, 5.2, 5.3

-- Create workspaces table with subscription and limits tracking
CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Workspace settings stored as JSONB for flexibility
    settings JSONB DEFAULT '{}' NOT NULL,
    
    -- Ownership and billing information
    owner_id BIGINT NOT NULL,
    subscription_plan VARCHAR(50) DEFAULT 'FREE' NOT NULL,
    billing_status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    
    -- Usage limits and tracking
    member_limit INTEGER DEFAULT 5 NOT NULL,
    content_limit INTEGER DEFAULT 100 NOT NULL,
    storage_limit_mb INTEGER DEFAULT 1000 NOT NULL,
    api_calls_limit INTEGER DEFAULT 1000 NOT NULL,
    
    -- Current usage counters
    current_member_count INTEGER DEFAULT 1 NOT NULL,
    current_content_count INTEGER DEFAULT 0 NOT NULL,
    current_storage_used_mb INTEGER DEFAULT 0 NOT NULL,
    current_api_calls_used INTEGER DEFAULT 0 NOT NULL,
    
    -- Workspace status and visibility
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    visibility VARCHAR(20) DEFAULT 'PRIVATE' NOT NULL,
    
    -- Workspace branding and customization
    logo_url VARCHAR(500),
    primary_color VARCHAR(7), -- Hex color code
    secondary_color VARCHAR(7), -- Hex color code
    custom_domain VARCHAR(100),
    
    -- Collaboration features configuration
    allow_external_sharing BOOLEAN DEFAULT FALSE NOT NULL,
    require_approval_for_sharing BOOLEAN DEFAULT TRUE NOT NULL,
    enable_comments BOOLEAN DEFAULT TRUE NOT NULL,
    enable_version_control BOOLEAN DEFAULT TRUE NOT NULL,
    enable_real_time_collaboration BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Workspace metadata
    industry VARCHAR(100),
    company_size VARCHAR(20),
    use_case VARCHAR(100),
    timezone VARCHAR(50) DEFAULT 'UTC',
    language VARCHAR(10) DEFAULT 'vi',
    
    -- Billing and subscription details
    subscription_started_at TIMESTAMP WITH TIME ZONE,
    subscription_expires_at TIMESTAMP WITH TIME ZONE,
    last_billing_date TIMESTAMP WITH TIME ZONE,
    next_billing_date TIMESTAMP WITH TIME ZONE,
    monthly_cost DECIMAL(10,2) DEFAULT 0.00,
    
    -- Usage reset tracking (for monthly limits)
    usage_reset_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    -- Foreign key constraints
    CONSTRAINT fk_workspaces_owner_id 
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_workspaces_name_not_empty 
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_workspaces_member_limit_positive 
        CHECK (member_limit > 0),
    CONSTRAINT chk_workspaces_content_limit_positive 
        CHECK (content_limit > 0),
    CONSTRAINT chk_workspaces_storage_limit_positive 
        CHECK (storage_limit_mb > 0),
    CONSTRAINT chk_workspaces_api_calls_limit_positive 
        CHECK (api_calls_limit > 0),
    CONSTRAINT chk_workspaces_current_counts_non_negative 
        CHECK (
            current_member_count >= 0 AND 
            current_content_count >= 0 AND 
            current_storage_used_mb >= 0 AND 
            current_api_calls_used >= 0
        ),
    CONSTRAINT chk_workspaces_subscription_plan_valid 
        CHECK (subscription_plan IN ('FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM')),
    CONSTRAINT chk_workspaces_billing_status_valid 
        CHECK (billing_status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'PAST_DUE', 'TRIAL')),
    CONSTRAINT chk_workspaces_status_valid 
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED', 'DELETED')),
    CONSTRAINT chk_workspaces_visibility_valid 
        CHECK (visibility IN ('PRIVATE', 'INTERNAL', 'PUBLIC')),
    CONSTRAINT chk_workspaces_company_size_valid 
        CHECK (company_size IS NULL OR company_size IN ('1-10', '11-50', '51-200', '201-1000', '1000+')),
    CONSTRAINT chk_workspaces_color_format 
        CHECK (
            (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$') AND
            (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$')
        ),
    CONSTRAINT chk_workspaces_usage_within_limits 
        CHECK (
            current_member_count <= member_limit AND
            current_content_count <= content_limit AND
            current_storage_used_mb <= storage_limit_mb AND
            current_api_calls_used <= api_calls_limit
        ),
    CONSTRAINT chk_workspaces_subscription_dates_logical 
        CHECK (
            subscription_expires_at IS NULL OR 
            subscription_started_at IS NULL OR 
            subscription_expires_at >= subscription_started_at
        )
);

-- Create workspace_members table with role-based permissions
CREATE TABLE workspace_members (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- Role and permissions
    role VARCHAR(50) DEFAULT 'MEMBER' NOT NULL,
    permissions JSONB DEFAULT '{}' NOT NULL,
    
    -- Member status and invitation details
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    invited_by BIGINT,
    invitation_token VARCHAR(255),
    invitation_expires_at TIMESTAMP WITH TIME ZONE,
    invitation_sent_at TIMESTAMP WITH TIME ZONE,
    
    -- Member activity and engagement
    last_active_at TIMESTAMP WITH TIME ZONE,
    total_content_created INTEGER DEFAULT 0 NOT NULL,
    total_content_shared INTEGER DEFAULT 0 NOT NULL,
    total_comments_made INTEGER DEFAULT 0 NOT NULL,
    
    -- Member preferences within workspace
    notification_preferences JSONB DEFAULT '{}' NOT NULL,
    workspace_settings JSONB DEFAULT '{}' NOT NULL,
    
    -- Access control and security
    can_invite_members BOOLEAN DEFAULT FALSE NOT NULL,
    can_manage_content BOOLEAN DEFAULT FALSE NOT NULL,
    can_export_data BOOLEAN DEFAULT FALSE NOT NULL,
    can_view_analytics BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Member onboarding and training
    onboarding_completed BOOLEAN DEFAULT FALSE NOT NULL,
    onboarding_completed_at TIMESTAMP WITH TIME ZONE,
    training_modules_completed TEXT[] DEFAULT '{}',
    
    -- Audit fields
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_workspace_members_workspace_id 
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_invited_by 
        FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Unique constraint to prevent duplicate memberships
    CONSTRAINT uk_workspace_members_workspace_user 
        UNIQUE(workspace_id, user_id),
    
    -- Check constraints for data integrity
    CONSTRAINT chk_workspace_members_role_valid 
        CHECK (role IN ('OWNER', 'ADMIN', 'EDITOR', 'VIEWER', 'MEMBER', 'GUEST')),
    CONSTRAINT chk_workspace_members_status_valid 
        CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED', 'LEFT', 'REMOVED')),
    CONSTRAINT chk_workspace_members_activity_counts_non_negative 
        CHECK (
            total_content_created >= 0 AND 
            total_content_shared >= 0 AND 
            total_comments_made >= 0
        ),
    CONSTRAINT chk_workspace_members_invitation_token_with_status 
        CHECK (
            (status = 'INVITED' AND invitation_token IS NOT NULL) OR 
            (status != 'INVITED' AND invitation_token IS NULL)
        ),
    CONSTRAINT chk_workspace_members_invitation_expiry_logical 
        CHECK (
            invitation_expires_at IS NULL OR 
            invitation_sent_at IS NULL OR 
            invitation_expires_at >= invitation_sent_at
        )
);

-- Create workspace_content_shares table for content collaboration
CREATE TABLE workspace_content_shares (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    
    -- Sharing details
    shared_by BIGINT NOT NULL,
    permission_level VARCHAR(20) DEFAULT 'VIEW' NOT NULL,
    
    -- Collaboration features
    allow_comments BOOLEAN DEFAULT TRUE NOT NULL,
    allow_suggestions BOOLEAN DEFAULT TRUE NOT NULL,
    allow_editing BOOLEAN DEFAULT FALSE NOT NULL,
    allow_downloading BOOLEAN DEFAULT FALSE NOT NULL,
    allow_copying BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Content sharing metadata
    share_title VARCHAR(500),
    share_description TEXT,
    share_tags TEXT[] DEFAULT '{}',
    
    -- Access control and security
    is_public_link BOOLEAN DEFAULT FALSE NOT NULL,
    public_link_token VARCHAR(255),
    public_link_expires_at TIMESTAMP WITH TIME ZONE,
    password_protected BOOLEAN DEFAULT FALSE NOT NULL,
    access_password_hash VARCHAR(255),
    
    -- Usage tracking and analytics
    view_count INTEGER DEFAULT 0 NOT NULL,
    download_count INTEGER DEFAULT 0 NOT NULL,
    comment_count INTEGER DEFAULT 0 NOT NULL,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    last_commented_at TIMESTAMP WITH TIME ZONE,
    
    -- Content versioning for shared content
    shared_version_number INTEGER,
    auto_update_shared_version BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Collaboration workflow
    approval_status VARCHAR(20) DEFAULT 'APPROVED' NOT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    
    -- Expiration and lifecycle management
    expires_at TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN DEFAULT FALSE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    archived_by BIGINT,
    
    -- Audit fields
    shared_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_workspace_content_shares_workspace_id 
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_content_shares_content_id 
        FOREIGN KEY (content_id) REFERENCES content_generations(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_content_shares_shared_by 
        FOREIGN KEY (shared_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_workspace_content_shares_approved_by 
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_workspace_content_shares_archived_by 
        FOREIGN KEY (archived_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Unique constraint to prevent duplicate shares
    CONSTRAINT uk_workspace_content_shares_workspace_content 
        UNIQUE(workspace_id, content_id),
    
    -- Check constraints for data integrity
    CONSTRAINT chk_workspace_content_shares_permission_level_valid 
        CHECK (permission_level IN ('VIEW', 'COMMENT', 'EDIT', 'ADMIN')),
    CONSTRAINT chk_workspace_content_shares_approval_status_valid 
        CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_workspace_content_shares_counts_non_negative 
        CHECK (
            view_count >= 0 AND 
            download_count >= 0 AND 
            comment_count >= 0
        ),
    CONSTRAINT chk_workspace_content_shares_shared_version_positive 
        CHECK (shared_version_number IS NULL OR shared_version_number > 0),
    CONSTRAINT chk_workspace_content_shares_public_link_token_consistency 
        CHECK (
            (is_public_link = TRUE AND public_link_token IS NOT NULL) OR 
            (is_public_link = FALSE AND public_link_token IS NULL)
        ),
    CONSTRAINT chk_workspace_content_shares_password_consistency 
        CHECK (
            (password_protected = TRUE AND access_password_hash IS NOT NULL) OR 
            (password_protected = FALSE AND access_password_hash IS NULL)
        ),
    CONSTRAINT chk_workspace_content_shares_expiry_logical 
        CHECK (expires_at IS NULL OR expires_at > shared_at),
    CONSTRAINT chk_workspace_content_shares_archive_consistency 
        CHECK (
            (is_archived = TRUE AND archived_at IS NOT NULL AND archived_by IS NOT NULL) OR 
            (is_archived = FALSE AND archived_at IS NULL AND archived_by IS NULL)
        )
);

-- Create workspace_activity_logs table for audit trail
CREATE TABLE workspace_activity_logs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT,
    
    -- Activity details
    activity_type VARCHAR(50) NOT NULL,
    activity_category VARCHAR(50) NOT NULL,
    activity_description TEXT NOT NULL,
    
    -- Target resource information
    target_resource_type VARCHAR(50),
    target_resource_id BIGINT,
    target_resource_name VARCHAR(500),
    
    -- Activity context and metadata
    activity_metadata JSONB DEFAULT '{}' NOT NULL,
    
    -- Request and session information
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(100),
    request_id VARCHAR(100),
    
    -- Geographic and device information
    country_code VARCHAR(2),
    city VARCHAR(100),
    device_type VARCHAR(20),
    browser VARCHAR(50),
    operating_system VARCHAR(50),
    
    -- Activity impact and severity
    impact_level VARCHAR(20) DEFAULT 'LOW' NOT NULL,
    severity VARCHAR(20) DEFAULT 'INFO' NOT NULL,
    
    -- Related activities and correlation
    parent_activity_id BIGINT,
    correlation_id VARCHAR(100),
    
    -- Activity outcome and status
    status VARCHAR(20) DEFAULT 'SUCCESS' NOT NULL,
    error_message TEXT,
    error_code VARCHAR(50),
    
    -- Performance and timing
    processing_time_ms BIGINT,
    
    -- Audit and compliance
    compliance_tags TEXT[] DEFAULT '{}',
    retention_policy VARCHAR(50) DEFAULT 'STANDARD',
    
    -- Timestamp information
    activity_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_workspace_activity_logs_workspace_id 
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_activity_logs_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_workspace_activity_logs_parent_activity_id 
        FOREIGN KEY (parent_activity_id) REFERENCES workspace_activity_logs(id) ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_workspace_activity_logs_activity_type_not_empty 
        CHECK (LENGTH(TRIM(activity_type)) > 0),
    CONSTRAINT chk_workspace_activity_logs_activity_category_not_empty 
        CHECK (LENGTH(TRIM(activity_category)) > 0),
    CONSTRAINT chk_workspace_activity_logs_activity_description_not_empty 
        CHECK (LENGTH(TRIM(activity_description)) > 0),
    CONSTRAINT chk_workspace_activity_logs_impact_level_valid 
        CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_workspace_activity_logs_severity_valid 
        CHECK (severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL')),
    CONSTRAINT chk_workspace_activity_logs_status_valid 
        CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL', 'CANCELLED')),
    CONSTRAINT chk_workspace_activity_logs_retention_policy_valid 
        CHECK (retention_policy IN ('STANDARD', 'EXTENDED', 'PERMANENT', 'MINIMAL')),
    CONSTRAINT chk_workspace_activity_logs_processing_time_positive 
        CHECK (processing_time_ms IS NULL OR processing_time_ms >= 0),
    CONSTRAINT chk_workspace_activity_logs_device_type_valid 
        CHECK (device_type IS NULL OR device_type IN ('DESKTOP', 'MOBILE', 'TABLET', 'API', 'SYSTEM'))
);

-- Create indexes for performance optimization

-- Workspaces Indexes
CREATE INDEX idx_workspaces_owner_id ON workspaces(owner_id);
CREATE INDEX idx_workspaces_status ON workspaces(status);
CREATE INDEX idx_workspaces_subscription_plan ON workspaces(subscription_plan);
CREATE INDEX idx_workspaces_billing_status ON workspaces(billing_status);
CREATE INDEX idx_workspaces_visibility ON workspaces(visibility);
CREATE INDEX idx_workspaces_created_at ON workspaces(created_at);
CREATE INDEX idx_workspaces_subscription_expires_at ON workspaces(subscription_expires_at) WHERE subscription_expires_at IS NOT NULL;
CREATE INDEX idx_workspaces_usage_reset_date ON workspaces(usage_reset_date);
CREATE INDEX idx_workspaces_industry ON workspaces(industry) WHERE industry IS NOT NULL;

-- Composite indexes for common queries
CREATE INDEX idx_workspaces_owner_status ON workspaces(owner_id, status);
CREATE INDEX idx_workspaces_plan_status ON workspaces(subscription_plan, billing_status);
CREATE INDEX idx_workspaces_status_visibility ON workspaces(status, visibility);

-- JSONB indexes for settings
CREATE INDEX idx_workspaces_settings ON workspaces USING GIN(settings);

-- Full-text search indexes
CREATE INDEX idx_workspaces_name_search ON workspaces USING GIN(to_tsvector('english', name));
CREATE INDEX idx_workspaces_description_search ON workspaces USING GIN(to_tsvector('english', COALESCE(description, '')));

-- Workspace Members Indexes
CREATE INDEX idx_workspace_members_workspace_id ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user_id ON workspace_members(user_id);
CREATE INDEX idx_workspace_members_role ON workspace_members(role);
CREATE INDEX idx_workspace_members_status ON workspace_members(status);
CREATE INDEX idx_workspace_members_invited_by ON workspace_members(invited_by) WHERE invited_by IS NOT NULL;
CREATE INDEX idx_workspace_members_last_active_at ON workspace_members(last_active_at) WHERE last_active_at IS NOT NULL;
CREATE INDEX idx_workspace_members_joined_at ON workspace_members(joined_at);
CREATE INDEX idx_workspace_members_invitation_expires_at ON workspace_members(invitation_expires_at) WHERE invitation_expires_at IS NOT NULL;

-- Composite indexes for workspace members
CREATE INDEX idx_workspace_members_workspace_status ON workspace_members(workspace_id, status);
CREATE INDEX idx_workspace_members_workspace_role ON workspace_members(workspace_id, role);
CREATE INDEX idx_workspace_members_user_status ON workspace_members(user_id, status);
CREATE INDEX idx_workspace_members_status_invited ON workspace_members(status, invitation_expires_at) WHERE status = 'INVITED';

-- JSONB indexes for member data
CREATE INDEX idx_workspace_members_permissions ON workspace_members USING GIN(permissions);
CREATE INDEX idx_workspace_members_notification_preferences ON workspace_members USING GIN(notification_preferences);
CREATE INDEX idx_workspace_members_workspace_settings ON workspace_members USING GIN(workspace_settings);

-- Array indexes for training modules
CREATE INDEX idx_workspace_members_training_modules ON workspace_members USING GIN(training_modules_completed);

-- Workspace Content Shares Indexes
CREATE INDEX idx_workspace_content_shares_workspace_id ON workspace_content_shares(workspace_id);
CREATE INDEX idx_workspace_content_shares_content_id ON workspace_content_shares(content_id);
CREATE INDEX idx_workspace_content_shares_shared_by ON workspace_content_shares(shared_by);
CREATE INDEX idx_workspace_content_shares_permission_level ON workspace_content_shares(permission_level);
CREATE INDEX idx_workspace_content_shares_approval_status ON workspace_content_shares(approval_status);
CREATE INDEX idx_workspace_content_shares_shared_at ON workspace_content_shares(shared_at);
CREATE INDEX idx_workspace_content_shares_expires_at ON workspace_content_shares(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_workspace_content_shares_is_public_link ON workspace_content_shares(is_public_link) WHERE is_public_link = TRUE;
CREATE INDEX idx_workspace_content_shares_is_archived ON workspace_content_shares(is_archived);
CREATE INDEX idx_workspace_content_shares_view_count ON workspace_content_shares(view_count DESC);
CREATE INDEX idx_workspace_content_shares_last_viewed_at ON workspace_content_shares(last_viewed_at) WHERE last_viewed_at IS NOT NULL;

-- Composite indexes for content shares
CREATE INDEX idx_workspace_content_shares_workspace_status ON workspace_content_shares(workspace_id, approval_status);
CREATE INDEX idx_workspace_content_shares_workspace_archived ON workspace_content_shares(workspace_id, is_archived);
CREATE INDEX idx_workspace_content_shares_shared_by_date ON workspace_content_shares(shared_by, shared_at DESC);
CREATE INDEX idx_workspace_content_shares_public_expires ON workspace_content_shares(is_public_link, expires_at) WHERE is_public_link = TRUE;

-- Array indexes for share tags
CREATE INDEX idx_workspace_content_shares_tags ON workspace_content_shares USING GIN(share_tags);

-- Full-text search indexes for shares
CREATE INDEX idx_workspace_content_shares_title_search ON workspace_content_shares USING GIN(to_tsvector('english', COALESCE(share_title, '')));
CREATE INDEX idx_workspace_content_shares_description_search ON workspace_content_shares USING GIN(to_tsvector('english', COALESCE(share_description, '')));

-- Workspace Activity Logs Indexes
CREATE INDEX idx_workspace_activity_logs_workspace_id ON workspace_activity_logs(workspace_id);
CREATE INDEX idx_workspace_activity_logs_user_id ON workspace_activity_logs(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_workspace_activity_logs_activity_type ON workspace_activity_logs(activity_type);
CREATE INDEX idx_workspace_activity_logs_activity_category ON workspace_activity_logs(activity_category);
CREATE INDEX idx_workspace_activity_logs_activity_timestamp ON workspace_activity_logs(activity_timestamp);
CREATE INDEX idx_workspace_activity_logs_target_resource_type ON workspace_activity_logs(target_resource_type) WHERE target_resource_type IS NOT NULL;
CREATE INDEX idx_workspace_activity_logs_target_resource_id ON workspace_activity_logs(target_resource_id) WHERE target_resource_id IS NOT NULL;
CREATE INDEX idx_workspace_activity_logs_impact_level ON workspace_activity_logs(impact_level);
CREATE INDEX idx_workspace_activity_logs_severity ON workspace_activity_logs(severity);
CREATE INDEX idx_workspace_activity_logs_status ON workspace_activity_logs(status);
CREATE INDEX idx_workspace_activity_logs_parent_activity_id ON workspace_activity_logs(parent_activity_id) WHERE parent_activity_id IS NOT NULL;
CREATE INDEX idx_workspace_activity_logs_correlation_id ON workspace_activity_logs(correlation_id) WHERE correlation_id IS NOT NULL;

-- Composite indexes for activity logs
CREATE INDEX idx_workspace_activity_logs_workspace_timestamp ON workspace_activity_logs(workspace_id, activity_timestamp DESC);
CREATE INDEX idx_workspace_activity_logs_workspace_type ON workspace_activity_logs(workspace_id, activity_type);
CREATE INDEX idx_workspace_activity_logs_workspace_category ON workspace_activity_logs(workspace_id, activity_category);
CREATE INDEX idx_workspace_activity_logs_user_timestamp ON workspace_activity_logs(user_id, activity_timestamp DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_workspace_activity_logs_type_timestamp ON workspace_activity_logs(activity_type, activity_timestamp DESC);
CREATE INDEX idx_workspace_activity_logs_severity_timestamp ON workspace_activity_logs(severity, activity_timestamp DESC) WHERE severity IN ('ERROR', 'FATAL');
CREATE INDEX idx_workspace_activity_logs_target_resource ON workspace_activity_logs(target_resource_type, target_resource_id) WHERE target_resource_type IS NOT NULL AND target_resource_id IS NOT NULL;

-- JSONB indexes for activity metadata
CREATE INDEX idx_workspace_activity_logs_metadata ON workspace_activity_logs USING GIN(activity_metadata);

-- Array indexes for compliance tags
CREATE INDEX idx_workspace_activity_logs_compliance_tags ON workspace_activity_logs USING GIN(compliance_tags);

-- Full-text search indexes for activity logs
CREATE INDEX idx_workspace_activity_logs_description_search ON workspace_activity_logs USING GIN(to_tsvector('english', activity_description));

-- Create triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$ language 'plpgsql';

-- Apply triggers to tables with updated_at columns
CREATE TRIGGER update_workspaces_updated_at 
    BEFORE UPDATE ON workspaces 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workspace_members_updated_at 
    BEFORE UPDATE ON workspace_members 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workspace_content_shares_updated_at 
    BEFORE UPDATE ON workspace_content_shares 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create function to update workspace member counts
CREATE OR REPLACE FUNCTION update_workspace_member_count()
RETURNS TRIGGER AS $
DECLARE
    workspace_id_to_update BIGINT;
    new_member_count INTEGER;
BEGIN
    -- Determine which workspace to update
    IF TG_OP = 'DELETE' THEN
        workspace_id_to_update := OLD.workspace_id;
    ELSE
        workspace_id_to_update := NEW.workspace_id;
    END IF;
    
    -- Calculate new member count (only active members)
    SELECT COUNT(*) 
    INTO new_member_count
    FROM workspace_members 
    WHERE workspace_id = workspace_id_to_update 
      AND status = 'ACTIVE';
    
    -- Update workspace member count
    UPDATE workspaces 
    SET 
        current_member_count = new_member_count,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = workspace_id_to_update;
    
    RETURN COALESCE(NEW, OLD);
END;
$ LANGUAGE plpgsql;

-- Create trigger to automatically update workspace member counts
CREATE TRIGGER update_workspace_member_count_trigger
    AFTER INSERT OR UPDATE OR DELETE ON workspace_members
    FOR EACH ROW EXECUTE FUNCTION update_workspace_member_count();

-- Create function to update workspace content counts
CREATE OR REPLACE FUNCTION update_workspace_content_count()
RETURNS TRIGGER AS $
DECLARE
    workspace_id_to_update BIGINT;
    new_content_count INTEGER;
BEGIN
    -- Determine which workspace to update
    IF TG_OP = 'DELETE' THEN
        workspace_id_to_update := OLD.workspace_id;
    ELSE
        workspace_id_to_update := NEW.workspace_id;
    END IF;
    
    -- Calculate new content count (only approved and non-archived shares)
    SELECT COUNT(*) 
    INTO new_content_count
    FROM workspace_content_shares 
    WHERE workspace_id = workspace_id_to_update 
      AND approval_status = 'APPROVED'
      AND is_archived = FALSE;
    
    -- Update workspace content count
    UPDATE workspaces 
    SET 
        current_content_count = new_content_count,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = workspace_id_to_update;
    
    RETURN COALESCE(NEW, OLD);
END;
$ LANGUAGE plpgsql;

-- Create trigger to automatically update workspace content counts
CREATE TRIGGER update_workspace_content_count_trigger
    AFTER INSERT OR UPDATE OR DELETE ON workspace_content_shares
    FOR EACH ROW EXECUTE FUNCTION update_workspace_content_count();

-- Create function to log workspace activities
CREATE OR REPLACE FUNCTION log_workspace_activity(
    p_workspace_id BIGINT,
    p_user_id BIGINT,
    p_activity_type VARCHAR(50),
    p_activity_category VARCHAR(50),
    p_activity_description TEXT,
    p_target_resource_type VARCHAR(50) DEFAULT NULL,
    p_target_resource_id BIGINT DEFAULT NULL,
    p_target_resource_name VARCHAR(500) DEFAULT NULL,
    p_activity_metadata JSONB DEFAULT '{}',
    p_impact_level VARCHAR(20) DEFAULT 'LOW',
    p_severity VARCHAR(20) DEFAULT 'INFO'
)
RETURNS BIGINT AS $
DECLARE
    activity_log_id BIGINT;
BEGIN
    INSERT INTO workspace_activity_logs (
        workspace_id,
        user_id,
        activity_type,
        activity_category,
        activity_description,
        target_resource_type,
        target_resource_id,
        target_resource_name,
        activity_metadata,
        impact_level,
        severity
    ) VALUES (
        p_workspace_id,
        p_user_id,
        p_activity_type,
        p_activity_category,
        p_activity_description,
        p_target_resource_type,
        p_target_resource_id,
        p_target_resource_name,
        p_activity_metadata,
        p_impact_level,
        p_severity
    ) RETURNING id INTO activity_log_id;
    
    RETURN activity_log_id;
END;
$ LANGUAGE plpgsql;

-- Create function to validate workspace limits
CREATE OR REPLACE FUNCTION validate_workspace_limits()
RETURNS TRIGGER AS $
BEGIN
    -- Check member limit
    IF NEW.current_member_count > NEW.member_limit THEN
        RAISE EXCEPTION 'Member count (%) exceeds workspace limit (%)', 
            NEW.current_member_count, NEW.member_limit;
    END IF;
    
    -- Check content limit
    IF NEW.current_content_count > NEW.content_limit THEN
        RAISE EXCEPTION 'Content count (%) exceeds workspace limit (%)', 
            NEW.current_content_count, NEW.content_limit;
    END IF;
    
    -- Check storage limit
    IF NEW.current_storage_used_mb > NEW.storage_limit_mb THEN
        RAISE EXCEPTION 'Storage usage (% MB) exceeds workspace limit (% MB)', 
            NEW.current_storage_used_mb, NEW.storage_limit_mb;
    END IF;
    
    -- Check API calls limit
    IF NEW.current_api_calls_used > NEW.api_calls_limit THEN
        RAISE EXCEPTION 'API calls usage (%) exceeds workspace limit (%)', 
            NEW.current_api_calls_used, NEW.api_calls_limit;
    END IF;
    
    RETURN NEW;
END;
$ LANGUAGE plpgsql;

-- Create trigger to validate workspace limits
CREATE TRIGGER validate_workspace_limits_trigger
    BEFORE UPDATE ON workspaces
    FOR EACH ROW EXECUTE FUNCTION validate_workspace_limits();

-- Create function to auto-expire invitations
CREATE OR REPLACE FUNCTION cleanup_expired_invitations()
RETURNS INTEGER AS $
DECLARE
    expired_count INTEGER;
BEGIN
    -- Update expired invitations to 'EXPIRED' status
    UPDATE workspace_members 
    SET 
        status = 'EXPIRED',
        updated_at = CURRENT_TIMESTAMP
    WHERE status = 'INVITED' 
      AND invitation_expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS expired_count = ROW_COUNT;
    
    RETURN expired_count;
END;
$ LANGUAGE plpgsql;

-- Create function to reset monthly usage counters
CREATE OR REPLACE FUNCTION reset_monthly_usage_counters()
RETURNS INTEGER AS $
DECLARE
    reset_count INTEGER;
BEGIN
    -- Reset API calls counter for workspaces where usage_reset_date has passed
    UPDATE workspaces 
    SET 
        current_api_calls_used = 0,
        usage_reset_date = usage_reset_date + INTERVAL '1 month',
        updated_at = CURRENT_TIMESTAMP
    WHERE usage_reset_date <= CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS reset_count = ROW_COUNT;
    
    RETURN reset_count;
END;
$ LANGUAGE plpgsql;

-- Create views for easy access to workspace data

-- View for workspace overview with member and content statistics
CREATE VIEW v_workspace_overview AS
SELECT 
    w.id,
    w.name,
    w.description,
    w.owner_id,
    u.username as owner_username,
    u.email as owner_email,
    w.subscription_plan,
    w.billing_status,
    w.status,
    w.visibility,
    w.current_member_count,
    w.member_limit,
    w.current_content_count,
    w.content_limit,
    w.current_storage_used_mb,
    w.storage_limit_mb,
    w.current_api_calls_used,
    w.api_calls_limit,
    w.created_at,
    w.updated_at,
    -- Calculate usage percentages
    ROUND((w.current_member_count::DECIMAL / w.member_limit) * 100, 2) as member_usage_percent,
    ROUND((w.current_content_count::DECIMAL / w.content_limit) * 100, 2) as content_usage_percent,
    ROUND((w.current_storage_used_mb::DECIMAL / w.storage_limit_mb) * 100, 2) as storage_usage_percent,
    ROUND((w.current_api_calls_used::DECIMAL / w.api_calls_limit) * 100, 2) as api_usage_percent,
    -- Calculate days since creation
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - w.created_at)) / 86400.0 as days_since_creation,
    -- Check if workspace is approaching limits
    CASE 
        WHEN w.current_member_count >= w.member_limit * 0.9 THEN TRUE 
        ELSE FALSE 
    END as approaching_member_limit,
    CASE 
        WHEN w.current_content_count >= w.content_limit * 0.9 THEN TRUE 
        ELSE FALSE 
    END as approaching_content_limit,
    CASE 
        WHEN w.current_storage_used_mb >= w.storage_limit_mb * 0.9 THEN TRUE 
        ELSE FALSE 
    END as approaching_storage_limit,
    CASE 
        WHEN w.current_api_calls_used >= w.api_calls_limit * 0.9 THEN TRUE 
        ELSE FALSE 
    END as approaching_api_limit
FROM workspaces w
JOIN users u ON w.owner_id = u.id
ORDER BY w.created_at DESC;

-- View for workspace member analytics
CREATE VIEW v_workspace_member_analytics AS
SELECT 
    wm.workspace_id,
    w.name as workspace_name,
    wm.user_id,
    u.username,
    u.email,
    wm.role,
    wm.status,
    wm.joined_at,
    wm.last_active_at,
    wm.total_content_created,
    wm.total_content_shared,
    wm.total_comments_made,
    wm.onboarding_completed,
    -- Calculate member engagement metrics
    CASE 
        WHEN wm.last_active_at IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.last_active_at)) / 86400.0
        ELSE NULL
    END as days_since_last_active,
    CASE 
        WHEN wm.joined_at IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.joined_at)) / 86400.0
        ELSE 0
    END as days_since_joined,
    -- Calculate activity rates
    CASE 
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.joined_at)) > 0 THEN
            wm.total_content_created / (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.joined_at)) / 86400.0)
        ELSE 0
    END as content_creation_rate_per_day,
    CASE 
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.joined_at)) > 0 THEN
            wm.total_comments_made / (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wm.joined_at)) / 86400.0)
        ELSE 0
    END as comment_rate_per_day,
    -- Member activity level
    CASE 
        WHEN wm.last_active_at IS NULL THEN 'NEVER_ACTIVE'
        WHEN wm.last_active_at >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 'HIGHLY_ACTIVE'
        WHEN wm.last_active_at >= CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 'MODERATELY_ACTIVE'
        WHEN wm.last_active_at >= CURRENT_TIMESTAMP - INTERVAL '90 days' THEN 'LOW_ACTIVITY'
        ELSE 'INACTIVE'
    END as activity_level
FROM workspace_members wm
JOIN workspaces w ON wm.workspace_id = w.id
JOIN users u ON wm.user_id = u.id
WHERE wm.status = 'ACTIVE'
ORDER BY wm.workspace_id, wm.last_active_at DESC;

-- View for workspace content sharing analytics
CREATE VIEW v_workspace_content_analytics AS
SELECT 
    wcs.workspace_id,
    w.name as workspace_name,
    wcs.content_id,
    cg.title as content_title,
    cg.content_type,
    wcs.shared_by,
    u.username as shared_by_username,
    wcs.permission_level,
    wcs.approval_status,
    wcs.view_count,
    wcs.download_count,
    wcs.comment_count,
    wcs.shared_at,
    wcs.last_viewed_at,
    wcs.is_public_link,
    wcs.expires_at,
    wcs.is_archived,
    -- Calculate engagement metrics
    CASE 
        WHEN wcs.last_viewed_at IS NOT NULL THEN 
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wcs.last_viewed_at)) / 86400.0
        ELSE NULL
    END as days_since_last_viewed,
    EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wcs.shared_at)) / 86400.0 as days_since_shared,
    -- Calculate engagement rates
    CASE 
        WHEN EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wcs.shared_at)) > 0 THEN
            wcs.view_count / (EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - wcs.shared_at)) / 86400.0)
        ELSE 0
    END as views_per_day,
    CASE 
        WHEN wcs.view_count > 0 THEN 
            (wcs.download_count::DECIMAL / wcs.view_count) * 100
        ELSE 0
    END as download_conversion_rate,
    CASE 
        WHEN wcs.view_count > 0 THEN 
            (wcs.comment_count::DECIMAL / wcs.view_count) * 100
        ELSE 0
    END as comment_engagement_rate,
    -- Content popularity level
    CASE 
        WHEN wcs.view_count >= 100 THEN 'HIGH'
        WHEN wcs.view_count >= 20 THEN 'MEDIUM'
        WHEN wcs.view_count >= 5 THEN 'LOW'
        ELSE 'MINIMAL'
    END as popularity_level
FROM workspace_content_shares wcs
JOIN workspaces w ON wcs.workspace_id = w.id
JOIN content_generations cg ON wcs.content_id = cg.id
JOIN users u ON wcs.shared_by = u.id
WHERE wcs.approval_status = 'APPROVED' AND wcs.is_archived = FALSE
ORDER BY wcs.workspace_id, wcs.view_count DESC, wcs.shared_at DESC;

-- View for workspace activity summary
CREATE VIEW v_workspace_activity_summary AS
SELECT 
    wal.workspace_id,
    w.name as workspace_name,
    wal.activity_category,
    COUNT(*) as activity_count,
    COUNT(DISTINCT wal.user_id) as unique_users,
    MIN(wal.activity_timestamp) as first_activity,
    MAX(wal.activity_timestamp) as last_activity,
    COUNT(CASE WHEN wal.activity_timestamp >= CURRENT_TIMESTAMP - INTERVAL '24 hours' THEN 1 END) as activities_last_24h,
    COUNT(CASE WHEN wal.activity_timestamp >= CURRENT_TIMESTAMP - INTERVAL '7 days' THEN 1 END) as activities_last_7d,
    COUNT(CASE WHEN wal.activity_timestamp >= CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 1 END) as activities_last_30d,
    COUNT(CASE WHEN wal.status = 'SUCCESS' THEN 1 END) as successful_activities,
    COUNT(CASE WHEN wal.status = 'FAILURE' THEN 1 END) as failed_activities,
    ROUND(
        (COUNT(CASE WHEN wal.status = 'SUCCESS' THEN 1 END)::DECIMAL / COUNT(*)) * 100, 
        2
    ) as success_rate_percent
FROM workspace_activity_logs wal
JOIN workspaces w ON wal.workspace_id = w.id
GROUP BY wal.workspace_id, w.name, wal.activity_category
ORDER BY wal.workspace_id, activity_count DESC;

-- Add comments for documentation
COMMENT ON TABLE workspaces IS 'Stores team workspaces with subscription tracking, usage limits, and collaboration settings';
COMMENT ON TABLE workspace_members IS 'Stores workspace membership with role-based permissions and activity tracking';
COMMENT ON TABLE workspace_content_shares IS 'Stores content sharing within workspaces with collaboration features and access control';
COMMENT ON TABLE workspace_activity_logs IS 'Comprehensive audit trail for all workspace activities and user actions';

COMMENT ON COLUMN workspaces.settings IS 'JSONB field storing workspace-specific configuration and preferences';
COMMENT ON COLUMN workspace_members.permissions IS 'JSONB field storing granular permissions for workspace member';
COMMENT ON COLUMN workspace_members.notification_preferences IS 'JSONB field storing member notification settings';
COMMENT ON COLUMN workspace_content_shares.share_tags IS 'Array of tags for organizing and categorizing shared content';
COMMENT ON COLUMN workspace_activity_logs.activity_metadata IS 'JSONB field storing detailed activity context and metadata';

COMMENT ON VIEW v_workspace_overview IS 'Comprehensive workspace overview with usage statistics and limit monitoring';
COMMENT ON VIEW v_workspace_member_analytics IS 'Member engagement analytics with activity levels and participation metrics';
COMMENT ON VIEW v_workspace_content_analytics IS 'Content sharing analytics with engagement rates and popularity metrics';
COMMENT ON VIEW v_workspace_activity_summary IS 'Workspace activity summary with success rates and trend analysis';