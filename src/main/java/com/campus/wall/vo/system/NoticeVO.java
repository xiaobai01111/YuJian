package com.campus.wall.vo.system;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeVO {

    private Long id;

    private String title;

    private String content;

    /**
     * 状态：0草稿 1已发布 2已下线
     */
    private Integer status;

    private String statusText;

    /**
     * 可见范围：ALL全校 DEPT部门 USERS指定用户
     */
    private String scopeType;

    private String scopeTypeText;

    /**
     * 部门ID或用户ID列表
     */
    private List<Long> scopeIds;

    private Boolean isPinned;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private LocalDateTime publishedAt;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
