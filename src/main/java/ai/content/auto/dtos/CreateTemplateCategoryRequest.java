package ai.content.auto.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTemplateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;

    private Long parentId;

    private Integer sortOrder = 0;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;
}