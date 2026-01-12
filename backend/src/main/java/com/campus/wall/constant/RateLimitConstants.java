package com.campus.wall.constant;

/**
 * 限流常量
 */
public final class RateLimitConstants {

    private RateLimitConstants() {}

    /**
     * IP限流 - 每分钟请求数
     */
    public static final int IP_LIMIT_PER_MINUTE = 60;

    /**
     * 用户发帖限流 - 每分钟发帖数
     */
    public static final int POST_LIMIT_PER_MINUTE = 5;

    /**
     * 用户评论限流 - 每分钟评论数
     */
    public static final int COMMENT_LIMIT_PER_MINUTE = 10;

    /**
     * 限流时间窗口（秒）
     */
    public static final int WINDOW_SECONDS = 60;
}
