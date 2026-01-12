package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "公告视图")
public class AnnouncementVO {

    private Long id;
    private String title;
    private String content;
    private String publisherName;
    private LocalDateTime createdAt;
}
