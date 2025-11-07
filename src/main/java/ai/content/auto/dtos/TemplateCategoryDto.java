package ai.content.auto.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class TemplateCategoryDto {
    private Long id;
    private String name;
    private String description;
    private String slug;
    private Long parentId;
    private String parentName;
    private List<TemplateCategoryDto> children;
    private Integer sortOrder;
    private Integer templateCount;
    private String status;
    private String icon;
    private String color;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;
}