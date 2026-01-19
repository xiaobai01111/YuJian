package com.campus.wall.vo.system;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String ipaddr;

    private String loginLocation;

    private String browser;

    private String os;

    private Integer status;

    private String msg;

    private String userAgent;

    private LocalDateTime loginTime;
}
