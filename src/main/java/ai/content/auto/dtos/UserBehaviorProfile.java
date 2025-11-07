package ai.content.auto.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User Behavior Profile
 * 
 * Represents a user's content generation behavior patterns and preferences
 */
@Data
public class UserBehaviorProfile {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Preferred industries based on usage history
     */
    private List<String> preferredIndustries = new ArrayList<>();

    /**
     * Preferred content types based on usage history
     */
    private List<String> preferredContentTypes = new ArrayList<>();

    /**
     * Preferred tones based on usage history
     */
    private List<String> preferredTones = new ArrayList<>();

    /**
     * Preferred languages based on usage history
     */
    private List<String> preferredLanguages = new ArrayList<>();

    /**
     * Preferred target audiences based on usage history
     */
    private List<String> preferredAudiences = new ArrayList<>();

    /**
     * Industries where user had successful generations
     */
    private Set<String> successfulIndustries = new HashSet<>();

    /**
     * Content types where user had successful generations
     */
    private Set<String> successfulContentTypes = new HashSet<>();

    /**
     * Tones where user had successful generations
     */
    private Set<String> successfulTones = new HashSet<>();

    /**
     * Template IDs used recently (last 7 days)
     */
    private Set<Long> recentlyUsedTemplateIds = new HashSet<>();

    /**
     * Total number of content generations
     */
    private Integer totalGenerations = 0;

    /**
     * Average quality score of user's generations
     */
    private Double averageQualityScore = 0.0;
}
