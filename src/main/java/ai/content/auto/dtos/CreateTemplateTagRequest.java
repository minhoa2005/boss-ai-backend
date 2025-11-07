package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateTagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 50, message = "Tag name must not exceed 50 characters")
    private String name;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;
}