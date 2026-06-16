package com.campus.wall.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "提交学生证审核请求")
public class SubmitIdCardDTO {

    @NotBlank(message = "学生证图片不能为空")
    @Schema(description = "学生证图片文件ID（上传后返回的文件ID）")
    private String imageUrl;

    @Schema(description = "学号")
    private String studentId;
}
