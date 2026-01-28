package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "提交学号认证请求")
public class SubmitStudentIdDTO {

    @NotBlank(message = "学号不能为空")
    @Schema(description = "学号")
    private String studentId;
}
