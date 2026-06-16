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
     * 登录限流 - 每分钟请求数（按账号）
     */
    public static final int LOGIN_LIMIT_PER_MINUTE_PER_USER = 8;

    /**
     * 登录限流 - 每分钟请求数（按设备）
     */
    public static final int LOGIN_LIMIT_PER_MINUTE_PER_DEVICE = 12;

    /**
     * 登录失败后触发验证码门槛（连续失败次数）
     */
    public static final int LOGIN_FAIL_CAPTCHA_THRESHOLD = 3;

    /**
     * 登录失败后触发临时锁定（连续失败次数）
     */
    public static final int LOGIN_FAIL_LOCK_THRESHOLD = 6;

    /**
     * 登录失败计数窗口（秒）
     */
    public static final int LOGIN_FAIL_WINDOW_SECONDS = 15 * 60;

    /**
     * 登录临时锁定时长（秒）
     */
    public static final int LOGIN_LOCK_SECONDS = 15 * 60;

    /**
     * 登录验证码有效时长（秒）
     */
    public static final int LOGIN_CAPTCHA_TTL_SECONDS = 2 * 60;

    /**
     * 登录验证码获取限流 - 每分钟请求数（按IP）
     */
    public static final int LOGIN_CAPTCHA_LIMIT_PER_MINUTE = 20;

    /**
     * Refresh Token 有效时长（秒）
     */
    public static final int REFRESH_TOKEN_TTL_SECONDS = 30 * 24 * 60 * 60;

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
     * 文件上传限流 - 每分钟上传数（按用户）
     */
    public static final int FILE_UPLOAD_LIMIT_PER_MINUTE = 10;

    /**
     * 文件上传限流 - 每分钟上传数（按IP）
     */
    public static final int FILE_UPLOAD_LIMIT_PER_MINUTE_PER_IP = 20;

    /**
     * 限流时间窗口（秒）
     */
    public static final int WINDOW_SECONDS = 60;
}
