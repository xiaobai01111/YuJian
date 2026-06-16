package com.campus.wall.dto.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeDTO {

    private String title;

    private String content;

    /**
     * 可见范围：ALL全校 DEPT部门 USERS指定用户
     */
    private String scopeType;

    /**
     * 部门ID或用户ID列表
     */
    private List<Long> scopeIds;

    private Boolean isPinned;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
