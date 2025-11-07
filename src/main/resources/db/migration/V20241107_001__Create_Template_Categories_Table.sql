-- Create template_categories table for hierarchical categorization
CREATE TABLE template_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    slug VARCHAR(100) UNIQUE,
    parent_id BIGINT REFERENCES template_categories(id) ON DELETE SET NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    template_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    icon VARCHAR(50),
    color VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_template_categories_parent_id ON template_categories(parent_id);
CREATE INDEX idx_template_categories_status ON template_categories(status);
CREATE INDEX idx_template_categories_slug ON template_categories(slug);
CREATE INDEX idx_template_categories_sort_order ON template_categories(sort_order);
CREATE INDEX idx_template_categories_template_count ON template_categories(template_count);

-- Insert default categories
INSERT INTO template_categories (name, description, slug, sort_order, icon, color) VALUES
('Marketing', 'Marketing and promotional content templates', 'marketing', 1, 'mdi-bullhorn', '#FF6B35'),
('Business', 'Business communication and documentation templates', 'business', 2, 'mdi-briefcase', '#004E89'),
('Social Media', 'Social media posts and content templates', 'social-media', 3, 'mdi-share-variant', '#FF006E'),
('Email', 'Email marketing and communication templates', 'email', 4, 'mdi-email', '#8338EC'),
('Blog', 'Blog posts and article templates', 'blog', 5, 'mdi-post', '#3A86FF'),
('Technical', 'Technical documentation and content templates', 'technical', 6, 'mdi-code-tags', '#06FFA5'),
('Creative', 'Creative writing and storytelling templates', 'creative', 7, 'mdi-palette', '#FFBE0B'),
('Educational', 'Educational and training content templates', 'educational', 8, 'mdi-school', '#FB8500');

-- Insert subcategories for Marketing
INSERT INTO template_categories (name, description, slug, parent_id, sort_order, icon, color) VALUES
('Ad Copy', 'Advertisement copy templates', 'ad-copy', (SELECT id FROM template_categories WHERE slug = 'marketing'), 1, 'mdi-advertisement', '#FF6B35'),
('Product Descriptions', 'Product description templates', 'product-descriptions', (SELECT id FROM template_categories WHERE slug = 'marketing'), 2, 'mdi-package-variant', '#FF6B35'),
('Landing Pages', 'Landing page content templates', 'landing-pages', (SELECT id FROM template_categories WHERE slug = 'marketing'), 3, 'mdi-web', '#FF6B35');

-- Insert subcategories for Social Media
INSERT INTO template_categories (name, description, slug, parent_id, sort_order, icon, color) VALUES
('Facebook Posts', 'Facebook post templates', 'facebook-posts', (SELECT id FROM template_categories WHERE slug = 'social-media'), 1, 'mdi-facebook', '#FF006E'),
('Twitter Posts', 'Twitter/X post templates', 'twitter-posts', (SELECT id FROM template_categories WHERE slug = 'social-media'), 2, 'mdi-twitter', '#FF006E'),
('Instagram Posts', 'Instagram post templates', 'instagram-posts', (SELECT id FROM template_categories WHERE slug = 'social-media'), 3, 'mdi-instagram', '#FF006E'),
('LinkedIn Posts', 'LinkedIn post templates', 'linkedin-posts', (SELECT id FROM template_categories WHERE slug = 'social-media'), 4, 'mdi-linkedin', '#FF006E');