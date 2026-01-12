package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "公告请求")
public class AnnouncementDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;
}
