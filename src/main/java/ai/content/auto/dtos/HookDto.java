package ai.content.auto.dtos;

import lombok.Data;
import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class HookDto {
    private Long hook_id;
    private String hook;
    private String industry;
    private String target_audience;
    private String tone;
    private String content_type;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime created_at;
}
