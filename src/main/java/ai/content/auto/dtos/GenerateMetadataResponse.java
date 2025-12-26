package ai.content.auto.dtos;

import lombok.Data;

@Data
public class GenerateMetadataResponse {
    private String contentType;
    private String tone;
    private String targetAudience;
}
