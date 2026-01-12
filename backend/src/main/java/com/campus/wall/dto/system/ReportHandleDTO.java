package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "处理举报请求")
public class ReportHandleDTO {

    @NotBlank(message = "处理结果不能为空")
    private String result;

    private String remark;
}
