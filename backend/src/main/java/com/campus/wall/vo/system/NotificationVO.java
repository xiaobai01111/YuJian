package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "通知视图")
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
