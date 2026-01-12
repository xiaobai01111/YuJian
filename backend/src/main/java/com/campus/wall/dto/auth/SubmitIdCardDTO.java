package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "提交学生证审核请求")
public class SubmitIdCardDTO {

    @NotBlank(message = "学生证图片URL不能为空")
    @Schema(description = "学生证图片URL")
    private String imageUrl;

    @Schema(description = "学号")
    private String studentId;
}
