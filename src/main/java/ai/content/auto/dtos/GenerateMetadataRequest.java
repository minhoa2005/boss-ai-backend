package ai.content.auto.dtos;

import lombok.Data;

@Data
public class GenerateMetadataRequest {
    private String industry;
    private String businessProfile;
    private String communicationGoal;
    private String title;
    private String content;
}
