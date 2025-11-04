-- Migration for adding version branching and tagging support
-- Requirements: 1.2, 1.5

-- Add new columns to content_versions table for branching and tagging
ALTER TABLE content_versions 
ADD COLUMN parent_version_id BIGINT,
ADD COLUMN branch_name VARCHAR(50),
ADD COLUMN is_experimental BOOLEAN DEFAULT FALSE,
ADD COLUMN version_tag VARCHAR(100),
ADD COLUMN annotation TEXT;

-- Add foreign key constraint for parent version
ALTER TABLE content_versions 
ADD CONSTRAINT fk_content_versions_parent 
FOREIGN KEY (parent_version_id) REFERENCES content_versions(id) ON DELETE SET NULL;

-- Add indexes for performance
CREATE INDEX idx_content_versions_parent_version_id ON content_versions(parent_version_id);
CREATE INDEX idx_content_versions_branch_name ON content_versions(content_id, branch_name);
CREATE INDEX idx_content_versions_experimental ON content_versions(content_id, is_experimental);
CREATE INDEX idx_content_versions_tag ON content_versions(content_id, version_tag);

-- Add composite index for branch queries
CREATE INDEX idx_content_versions_branch_version ON content_versions(content_id, branch_name, version_number);

-- Update existing records to set default values
UPDATE content_versions SET is_experimental = FALSE WHERE is_experimental IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN content_versions.parent_version_id IS 'ID of the parent version for branched versions';
COMMENT ON COLUMN content_versions.branch_name IS 'Name of the branch for experimental versions';
COMMENT ON COLUMN content_versions.is_experimental IS 'Flag indicating if this is an experimental version';
COMMENT ON COLUMN content_versions.version_tag IS 'Tag for categorizing and organizing versions';
COMMENT ON COLUMN content_versions.annotation IS 'User annotation or notes for the version';