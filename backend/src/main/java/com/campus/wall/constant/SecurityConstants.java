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
     * 普通用户角色标识
     */
    public static final String ROLE_USER = "user";

    /**
     * 全部权限标识
     */
    public static final String ALL_PERMISSION = "*";

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
