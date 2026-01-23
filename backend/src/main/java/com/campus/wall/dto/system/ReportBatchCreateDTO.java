package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量举报请求")
public class ReportBatchCreateDTO {

    @NotEmpty(message = "帖子ID不能为空")
    private List<Long> postIds;

    @NotBlank(message = "举报理由不能为空")
    @Size(max = 500, message = "举报理由最多500个字符")
    private String reason;
}
