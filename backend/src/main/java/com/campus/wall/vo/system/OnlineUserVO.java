package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在线用户信息
 */
@Data
public class OnlineUserVO {

    @Schema(description = "Token")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "登录IP")
    private String ipaddr;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "登录时间")
    private String loginTime;

    @Schema(description = "最近活跃时间（毫秒）")
    private Long lastActiveTime;

    @Schema(description = "Token剩余有效期（秒）")
    private Long tokenTimeout;

    @Schema(description = "Token临时有效期（秒）")
    private Long tokenActiveTimeout;
}
