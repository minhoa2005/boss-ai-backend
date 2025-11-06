package ai.content.auto.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "user_behavior_analytics")
public class UserBehaviorAnalytic {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.SET_NULL)
  @JoinColumn(name = "user_id")
  private User user;

  @Size(max = 100)
  @Column(name = "session_id", length = 100)
  private String sessionId;

  @Size(max = 50)
  @Column(name = "user_segment", length = 50)
  private String userSegment;

  @Size(max = 50)
  @Column(name = "user_cohort", length = 50)
  private String userCohort;

  @Size(max = 30)
  @ColumnDefault("'REGULAR'")
  @Column(name = "user_type", length = 30)
  private String userType;

  @Size(max = 30)
  @Column(name = "subscription_tier", length = 30)
  private String subscriptionTier;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "page_views", nullable = false)
  private Integer pageViews;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "unique_page_views", nullable = false)
  private Integer uniquePageViews;

  @ColumnDefault("0")
  @Column(name = "session_duration")
  private Integer sessionDuration;

  @ColumnDefault("0.0000")
  @Column(name = "bounce_rate", precision = 5, scale = 4)
  private BigDecimal bounceRate;

  @ColumnDefault("0.00")
  @Column(name = "pages_per_session", precision = 5, scale = 2)
  private BigDecimal pagesPerSession;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "content_generated", nullable = false)
  private Integer contentGenerated;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "content_shared", nullable = false)
  private Integer contentShared;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "content_liked", nullable = false)
  private Integer contentLiked;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "content_commented", nullable = false)
  private Integer contentCommented;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "content_downloaded", nullable = false)
  private Integer contentDownloaded;

  @ColumnDefault("'[]'")
  @Column(name = "features_used")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> featuresUsed;

  @Size(max = 100)
  @Column(name = "most_used_feature", length = 100)
  private String mostUsedFeature;

  @ColumnDefault("0.00")
  @Column(name = "feature_adoption_score", precision = 5, scale = 2)
  private BigDecimal featureAdoptionScore;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "advanced_features_used", nullable = false)
  private Integer advancedFeaturesUsed;

  @Size(max = 50)
  @Column(name = "funnel_stage", length = 50)
  private String funnelStage;

  @ColumnDefault("'[]'")
  @Column(name = "conversion_events")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> conversionEvents;

  @ColumnDefault("'[]'")
  @Column(name = "drop_off_points")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> dropOffPoints;

  @ColumnDefault("'[]'")
  @Column(name = "user_journey_path")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> userJourneyPath;

  @ColumnDefault("0.00")
  @Column(name = "login_frequency", precision = 5, scale = 2)
  private BigDecimal loginFrequency;

  @ColumnDefault("0")
  @Column(name = "average_session_length")
  private Integer averageSessionLength;

  @Column(name = "peak_activity_hour")
  private Integer peakActivityHour;

  @Column(name = "peak_activity_day")
  private Integer peakActivityDay;

  @ColumnDefault("0.00")
  @Column(name = "activity_consistency_score", precision = 5, scale = 2)
  private BigDecimal activityConsistencyScore;

  @ColumnDefault("'[]'")
  @Column(name = "preferred_content_types")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> preferredContentTypes;

  @ColumnDefault("'[]'")
  @Column(name = "preferred_features")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> preferredFeatures;

  @ColumnDefault("'{}'")
  @Column(name = "interaction_patterns")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> interactionPatterns;

  @Size(max = 20)
  @ColumnDefault("'BASIC'")
  @Column(name = "customization_level", length = 20)
  private String customizationLevel;

  @Size(max = 2)
  @Column(name = "country_code", length = 2)
  private String countryCode;

  @Size(max = 100)
  @Column(name = "city", length = 100)
  private String city;

  @Size(max = 50)
  @Column(name = "timezone", length = 50)
  private String timezone;

  @Size(max = 20)
  @Column(name = "device_type", length = 20)
  private String deviceType;

  @Size(max = 50)
  @Column(name = "browser", length = 50)
  private String browser;

  @Size(max = 50)
  @Column(name = "operating_system", length = 50)
  private String operatingSystem;

  @Size(max = 20)
  @Column(name = "screen_resolution", length = 20)
  private String screenResolution;

  @Column(name = "page_load_time_avg")
  private Integer pageLoadTimeAvg;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "error_encounters", nullable = false)
  private Integer errorEncounters;

  @NotNull
  @ColumnDefault("0")
  @Column(name = "support_tickets_created", nullable = false)
  private Integer supportTicketsCreated;

  @Column(name = "user_satisfaction_rating", precision = 3, scale = 2)
  private BigDecimal userSatisfactionRating;

  @Column(name = "net_promoter_score")
  private Integer netPromoterScore;

  @Size(max = 50)
  @Column(name = "user_intent", length = 50)
  private String userIntent;

  @ColumnDefault("'[]'")
  @Column(name = "search_queries")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> searchQueries;

  @ColumnDefault("'[]'")
  @Column(name = "exit_pages")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> exitPages;

  @ColumnDefault("'[]'")
  @Column(name = "referral_sources")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String, Object> referralSources;

  @Size(max = 20)
  @NotNull
  @ColumnDefault("'DAILY'")
  @Column(name = "measurement_period", nullable = false, length = 20)
  private String measurementPeriod;

  @NotNull
  @Column(name = "period_start_date", nullable = false)
  private OffsetDateTime periodStartDate;

  @NotNull
  @Column(name = "period_end_date", nullable = false)
  private OffsetDateTime periodEndDate;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'SYSTEM'")
  @Column(name = "data_source", nullable = false, length = 50)
  private String dataSource;

  @Size(max = 50)
  @NotNull
  @ColumnDefault("'AUTOMATIC'")
  @Column(name = "collection_method", nullable = false, length = 50)
  private String collectionMethod;

  @ColumnDefault("1.00")
  @Column(name = "data_quality_score", precision = 3, scale = 2)
  private BigDecimal dataQualityScore;

  @NotNull
  @ColumnDefault("true")
  @Column(name = "privacy_compliant", nullable = false)
  private Boolean privacyCompliant = false;

  @NotNull
  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
}
