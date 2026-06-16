package com.campus.wall.constant;

/**
 * 安全相关常量
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /**
     * 版主角色标识
     */
    public static final String ROLE_MODERATOR = "moderator";

    /**
     * 系统管理员角色标识
     */
    public static final String ROLE_ADMIN = "admin";

    /**
     * 系统部门ID（根部门，禁止删除）
     */
    public static final long SYSTEM_DEPT_ID = 1L;

    /**
     * 系统部门名称
     */
    public static final String SYSTEM_DEPT_NAME = "系统部门";

    /**
     * 数据权限范围: 1-全部
     */
    public static final int DATA_SCOPE_ALL = 1;

    /**
     * 数据权限范围: 2-自定义
     */
    public static final int DATA_SCOPE_CUSTOM = 2;

    /**
     * 数据权限范围: 3-本部门
     */
    public static final int DATA_SCOPE_DEPT = 3;

    /**
     * 数据权限范围: 4-本部门及以下
     */
    public static final int DATA_SCOPE_DEPT_AND_CHILD = 4;

    /**
     * 数据权限范围: 5-仅本人
     */
    public static final int DATA_SCOPE_SELF = 5;

    /**
     * 普通用户角色标识
     */
    public static final String ROLE_USER = "user";

    /**
     * 全部权限标识
     */
    public static final String ALL_PERMISSION = "*";

    /**
     * Token 会话：用户名
     */
    public static final String TOKEN_SESSION_USERNAME = "token_session_username";

    /**
     * Token 会话：昵称
     */
    public static final String TOKEN_SESSION_NICKNAME = "token_session_nickname";

    /**
     * Token 会话：登录 IP
     */
    public static final String TOKEN_SESSION_IP = "token_session_ip";

    /**
     * Token 会话：用户代理
     */
    public static final String TOKEN_SESSION_USER_AGENT = "token_session_user_agent";

    /**
     * Token 会话：登录时间
     */
    public static final String TOKEN_SESSION_LOGIN_TIME = "token_session_login_time";

    /**
     * Token 会话：绑定的 Refresh Token
     */
    public static final String TOKEN_SESSION_REFRESH_TOKEN = "token_session_refresh_token";

    /**
     * Refresh Token Cookie 名称
     */
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * 市集发帖最低信用分
     */
    public static final int MARKET_MIN_CREDIT_SCORE = 60;

    /**
     * 信用分上限
     */
    public static final int MAX_CREDIT_SCORE = 100;

    /**
     * 信用分下限
     */
    public static final int MIN_CREDIT_SCORE = 0;
}
