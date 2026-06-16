package com.campus.wall.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "身份审核视图")
public class VerificationVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String imageUrl;
    private String verifyMethod;
    private String studentId;
    private Integer status;
    private String rejectReason;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
