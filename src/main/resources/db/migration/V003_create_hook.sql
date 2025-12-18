CREATE TABLE hooks (
    hook_id SERIAL PRIMARY KEY,
    hook TEXT NOT NULL,
    industry VARCHAR(100),         -- Nhập dữ liệu industry (fashion, sports_fitness...)
    target_audience VARCHAR(100),  -- Nhập dữ liệu target_audience (business_owners...)
    tone VARCHAR(100),             -- Nhập dữ liệu tone (professional, exciting...)
    content_type VARCHAR(100),     -- Nhập dữ liệu content_type (article, blog...)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO hooks (hook, industry, target_audience, tone, content_type)
VALUES
  ('fear_crisis', 'finance', 'business_owners', 'authoritative', 'email'),
  ('desire_benefit', 'fashion', 'luxury_seekers', 'exciting', 'social'),
  ('empathy', 'non_profit', 'parents', 'friendly', 'social'),
  ('authority', 'consulting', 'professionals', 'expert', 'article'),
  ('question_open', 'education', 'students', 'enthusiastic', 'facebook'),
  ('pain_point', 'healthcare', 'seniors', 'friendly', 'description'),
  ('loss_aversion', 'real_estate', 'budget_conscious', 'formal', 'ad'),
  ('contrast', 'food_beverage', 'health_conscious', 'casual', 'blog'),
  ('celebrity_reference', 'entertainment', 'young_adults', 'humorous', 'social'),
  ('warning_trap', 'finance', 'entrepreneurs', 'authoritative', 'newsletter'),
  ('exaggerated_fear', 'healthcare', 'parents', 'expert', 'article'),
  ('exaggerated_pain', 'retail', 'general_public', 'humorous', 'advertisement'),
  ('secret_reveal', 'travel_tourism', 'early_adopters', 'exciting', 'social'),
  ('learn_fast', 'technology', 'students', 'enthusiastic', 'blog'),
  ('best_vs_worst', 'technology', 'tech_savvy', 'expert', 'article'),
  ('emotional_connection', 'non_profit', 'general_public', 'friendly', 'newsletter'),
  ('extreme_number', 'finance', 'professionals', 'authoritative', 'email'),
  ('perspective_shift', 'consulting', 'business_owners', 'professional', 'article'),
  ('future_trend', 'automotive', 'early_adopters', 'exciting', 'social'),
  ('origin_story', 'manufacturing', 'business_owners', 'professional', 'blog'),
  ('challenge', 'sports_fitness', 'young_adults', 'enthusiastic', 'facebook'),
  ('journey', 'travel_tourism', 'luxury_seekers', 'casual', 'blog'),
  ('money', 'retail', 'budget_conscious', 'exciting', 'ad'),
  ('review_intro', 'automotive', 'middle_aged', 'expert', 'product'),
  ('field_combination', 'consulting', 'entrepreneurs', 'professional', 'article'),
  ('surprise_gift', 'retail', 'parents', 'friendly', 'email');