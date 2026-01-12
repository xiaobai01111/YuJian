package com.campus.wall.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文件上传请求")
public class FileUploadDTO {

    private Long targetId;

    private String targetType;
}
