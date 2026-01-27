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
     * 登录限流 - 每分钟请求数（按IP）
     */
    public static final int LOGIN_LIMIT_PER_MINUTE = 10;

    /**
     * 注册限流 - 每分钟请求数（按IP）
     */
    public static final int REGISTER_LIMIT_PER_MINUTE = 5;

    /**
     * 邮箱验证码限流 - 每分钟请求数（按IP）
     */
    public static final int EMAIL_CODE_LIMIT_PER_MINUTE = 3;
    
    /**
     * 邮箱验证码限流 - 同一邮箱冷却时间（秒）
     * 同一邮箱在此时间内只能发送一次验证码
     */
    public static final int EMAIL_CODE_COOLDOWN_SECONDS = 60;
    
    /**
     * 邮箱验证码限流 - 同一邮箱每小时最大次数
     */
    public static final int EMAIL_CODE_LIMIT_PER_HOUR = 5;

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
