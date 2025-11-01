package ai.content.auto.constants;

/**
 * Constants for content generation and processing
 */
public final class ContentConstants {

    private ContentConstants() {
        // Utility class - prevent instantiation
    }

    // ===== CONTENT TYPES =====
    public static final String CONTENT_TYPE_BLOG = "blog";
    public static final String CONTENT_TYPE_ARTICLE = "article";
    public static final String CONTENT_TYPE_SOCIAL = "social";
    public static final String CONTENT_TYPE_FACEBOOK = "facebook";
    public static final String CONTENT_TYPE_INSTAGRAM = "instagram";
    public static final String CONTENT_TYPE_EMAIL = "email";
    public static final String CONTENT_TYPE_NEWSLETTER = "newsletter";
    public static final String CONTENT_TYPE_PRODUCT = "product";
    public static final String CONTENT_TYPE_DESCRIPTION = "description";
    public static final String CONTENT_TYPE_AD = "ad";
    public static final String CONTENT_TYPE_ADVERTISEMENT = "advertisement";
    public static final String CONTENT_TYPE_GENERAL = "general";

    // ===== TONES =====
    public static final String TONE_PROFESSIONAL = "professional";
    public static final String TONE_FORMAL = "formal";
    public static final String TONE_FRIENDLY = "friendly";
    public static final String TONE_CASUAL = "casual";
    public static final String TONE_ENTHUSIASTIC = "enthusiastic";
    public static final String TONE_EXCITING = "exciting";
    public static final String TONE_HUMOROUS = "humorous";
    public static final String TONE_FUNNY = "funny";
    public static final String TONE_AUTHORITATIVE = "authoritative";
    public static final String TONE_EXPERT = "expert";

    // ===== LANGUAGES =====
    public static final String LANGUAGE_VIETNAMESE = "vi";
    public static final String LANGUAGE_ENGLISH = "en";

    // ===== STATUS VALUES =====
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SAVED = "SAVED";
    public static final String STATUS_WORKFLOW_TRIGGERED = "WORKFLOW_TRIGGERED";
    public static final String STATUS_WORKFLOW_COMPLETED = "WORKFLOW_COMPLETED";
    public static final String STATUS_WORKFLOW_FAILED = "WORKFLOW_FAILED";

    // ===== OPENAI PARAMETERS =====
    public static final String OPENAI_ROLE_SYSTEM = "system";
    public static final String OPENAI_ROLE_USER = "user";
    public static final String OPENAI_FINISH_REASON_LENGTH = "length";
    public static final String OPENAI_AGENT_NAME = "openai";

    // ===== AI ARTIFACTS (for detection) =====
    public static final String AI_ARTIFACT_AS_AN_AI = "as an ai";
    public static final String AI_ARTIFACT_I_CANNOT = "i cannot";
    public static final String AI_ARTIFACT_IM_SORRY = "i'm sorry";
    public static final String AI_ARTIFACT_LANGUAGE_MODEL = "as a language model";

    // ===== MAX TOKENS BY CONTENT TYPE =====
    public static final long MAX_TOKENS_BLOG = 2000L;
    public static final long MAX_TOKENS_ARTICLE = 2000L;
    public static final long MAX_TOKENS_SOCIAL = 300L;
    public static final long MAX_TOKENS_FACEBOOK = 300L;
    public static final long MAX_TOKENS_INSTAGRAM = 200L;
    public static final long MAX_TOKENS_EMAIL = 800L;
    public static final long MAX_TOKENS_NEWSLETTER = 1200L;
    public static final long MAX_TOKENS_PRODUCT = 500L;
    public static final long MAX_TOKENS_DESCRIPTION = 500L;
    public static final long MAX_TOKENS_AD = 250L;
    public static final long MAX_TOKENS_ADVERTISEMENT = 250L;
    public static final long MAX_TOKENS_DEFAULT = 1000L;

    // ===== OPENAI API PARAMETERS =====
    public static final double TOP_P_DEFAULT = 0.9;
    public static final double FREQUENCY_PENALTY_DEFAULT = 0.1;
    public static final double PRESENCE_PENALTY_DEFAULT = 0.1;

    // ===== QUALITY THRESHOLDS =====
    public static final double QUALITY_SCORE_THRESHOLD = 3.0;
    public static final int MIN_CONTENT_LENGTH = 20;
    public static final int OPTIMAL_MIN_LENGTH = 100;
    public static final int OPTIMAL_MAX_LENGTH = 2000;
    public static final int MIN_SENTENCE_LENGTH = 50;

    // ===== READABILITY PARAMETERS =====
    public static final int OPTIMAL_WORDS_PER_SENTENCE_MIN = 10;
    public static final int OPTIMAL_WORDS_PER_SENTENCE_MAX = 25;
    public static final int COMPLEX_SENTENCE_THRESHOLD = 30;
    public static final int SIMPLE_SENTENCE_THRESHOLD = 5;

    // ===== COST CALCULATION =====
    public static final double COST_PER_1K_TOKENS = 0.0004; // GPT-4o-mini average rate
    public static final int TOKEN_ESTIMATION_RATIO = 4; // 1 token ≈ 4 characters
    public static final int MAX_PROMPT_TOKENS = 4000;

    // ===== RETRY CONFIGURATION =====
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final long RETRY_DELAY_BASE_MS = 1000L;

    // ===== AI PROVIDER =====
    public static final String AI_PROVIDER_OPENAI = "OpenAI";
    public static final String DEFAULT_AI_MODEL = "gpt-3.5-turbo";

    // ===== CONTENT DESCRIPTIONS =====
    public static final String DESC_BLOG = "Bài viết blog (800-1500 từ, có cấu trúc rõ ràng)";
    public static final String DESC_ARTICLE = "Bài viết chuyên sâu (1000-2000 từ, có tính chuyên môn cao)";
    public static final String DESC_SOCIAL = "Bài đăng mạng xã hội (50-200 từ, hấp dẫn và viral)";
    public static final String DESC_FACEBOOK = "Bài đăng Facebook (100-300 từ, tương tác cao)";
    public static final String DESC_INSTAGRAM = "Caption Instagram (50-150 từ, có hashtag)";
    public static final String DESC_EMAIL = "Email marketing (200-500 từ, có CTA rõ ràng)";
    public static final String DESC_NEWSLETTER = "Bản tin email (300-800 từ, thông tin hữu ích)";
    public static final String DESC_PRODUCT = "Mô tả sản phẩm (100-300 từ, tập trung vào lợi ích)";
    public static final String DESC_AD = "Nội dung quảng cáo (50-200 từ, thuyết phục mạnh)";

    // ===== TONE DESCRIPTIONS =====
    public static final String DESC_TONE_PROFESSIONAL = "Chuyên nghiệp, trang trọng, đáng tin cậy";
    public static final String DESC_TONE_FRIENDLY = "Thân thiện, gần gũi, dễ tiếp cận";
    public static final String DESC_TONE_ENTHUSIASTIC = "Nhiệt huyết, hứng khởi, tràn đầy năng lượng";
    public static final String DESC_TONE_HUMOROUS = "Hài hước, vui vẻ, nhẹ nhàng";
    public static final String DESC_TONE_AUTHORITATIVE = "Có thẩm quyền, chuyên gia, thuyết phục";
    public static final String DESC_TONE_CASUAL = "Thoải mái, không trang trọng, tự nhiên";

    // ===== LANGUAGE DESCRIPTIONS =====
    public static final String DESC_LANGUAGE_VI = "Tiếng Việt (tự nhiên, phù hợp văn hóa Việt)";
    public static final String DESC_LANGUAGE_EN = "English (natural, clear, engaging)";
    public static final String CATEGORY_TONE = "tone";
    public static final String CATEGORY_CONTENT_TYPE = "content_type";
    public static final String CATEGORY_TARGET_AUDIENCE = "target_audience";
    public static final String CATEGORY_LANGUAGE = "language";
    public static final String CATEGORY_INDUSTRY = "industry";
}