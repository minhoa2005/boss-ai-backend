-- Add category_id column to content_templates table to reference template_categories
ALTER TABLE content_templates 
ADD COLUMN category_id BIGINT REFERENCES template_categories(id) ON DELETE SET NULL;

-- Create index for better performance
CREATE INDEX idx_content_templates_category_id ON content_templates(category_id);

-- Update existing templates to reference the new category system
-- Map existing category strings to the new category IDs
UPDATE content_templates 
SET category_id = (
    SELECT id FROM template_categories 
    WHERE LOWER(template_categories.name) = LOWER(content_templates.category)
    AND template_categories.parent_id IS NULL
    LIMIT 1
)
WHERE category_id IS NULL;

-- For templates that don't match existing categories, create new categories
INSERT INTO template_categories (name, slug, description, sort_order, status)
SELECT DISTINCT 
    category,
    LOWER(REPLACE(REPLACE(category, ' ', '-'), '_', '-')),
    'Auto-generated category from existing templates',
    999,
    'ACTIVE'
FROM content_templates 
WHERE category_id IS NULL 
AND category NOT IN (SELECT name FROM template_categories)
ON CONFLICT (name) DO NOTHING;

-- Update templates with the newly created categories
UPDATE content_templates 
SET category_id = (
    SELECT id FROM template_categories 
    WHERE LOWER(template_categories.name) = LOWER(content_templates.category)
    LIMIT 1
)
WHERE category_id IS NULL;

-- Update template counts in categories
UPDATE template_categories 
SET template_count = (
    SELECT COUNT(*) 
    FROM content_templates 
    WHERE content_templates.category_id = template_categories.id
    AND content_templates.status = 'ACTIVE'
);

-- Update updated_at timestamp
UPDATE template_categories SET updated_at = CURRENT_TIMESTAMP;