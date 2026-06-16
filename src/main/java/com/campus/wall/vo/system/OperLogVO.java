package com.campus.wall.vo.system;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperLogVO {

    private Long id;

    private Long operatorId;

    private String operatorName;

    private String targetType;

    private Long targetId;

    private String action;

    private String reason;

    private Object beforeValue;

    private Object afterValue;

    private String ipAddress;
    private String userAgent;
    private String requestBodyDigest;

    private LocalDateTime createdAt;
}
