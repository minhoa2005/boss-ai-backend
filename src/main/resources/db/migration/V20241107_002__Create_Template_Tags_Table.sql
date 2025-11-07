-- Create template_tags table for flexible tagging system
CREATE TABLE template_tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(100) UNIQUE,
    description VARCHAR(200),
    usage_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    color VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_template_tags_name ON template_tags(name);
CREATE INDEX idx_template_tags_slug ON template_tags(slug);
CREATE INDEX idx_template_tags_status ON template_tags(status);
CREATE INDEX idx_template_tags_usage_count ON template_tags(usage_count DESC);

-- Insert popular tags
INSERT INTO template_tags (name, slug, description, color) VALUES
('beginner', 'beginner', 'Templates suitable for beginners', '#4CAF50'),
('advanced', 'advanced', 'Templates for advanced users', '#FF5722'),
('quick', 'quick', 'Quick and simple templates', '#2196F3'),
('detailed', 'detailed', 'Comprehensive and detailed templates', '#9C27B0'),
('professional', 'professional', 'Professional business templates', '#607D8B'),
('casual', 'casual', 'Casual and informal templates', '#FF9800'),
('formal', 'formal', 'Formal communication templates', '#795548'),
('creative', 'creative', 'Creative and artistic templates', '#E91E63'),
('technical', 'technical', 'Technical and specialized templates', '#009688'),
('sales', 'sales', 'Sales and conversion focused templates', '#8BC34A'),
('informative', 'informative', 'Educational and informative templates', '#03A9F4'),
('persuasive', 'persuasive', 'Persuasive and compelling templates', '#FF6B35'),
('announcement', 'announcement', 'Announcement and news templates', '#673AB7'),
('tutorial', 'tutorial', 'Step-by-step tutorial templates', '#FFC107'),
('review', 'review', 'Review and feedback templates', '#CDDC39'),
('comparison', 'comparison', 'Comparison and analysis templates', '#00BCD4'),
('listicle', 'listicle', 'List-based content templates', '#FFEB3B'),
('howto', 'howto', 'How-to guide templates', '#8BC34A'),
('case-study', 'case-study', 'Case study templates', '#FF5722'),
('interview', 'interview', 'Interview and Q&A templates', '#9E9E9E');