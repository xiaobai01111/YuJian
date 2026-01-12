package com.campus.wall.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文件视图")
public class FileVO {

    private Long id;
    private String filename;
    private String path;
    private String url;
    private Long size;
    private String mimeType;
    private LocalDateTime createdAt;
}
