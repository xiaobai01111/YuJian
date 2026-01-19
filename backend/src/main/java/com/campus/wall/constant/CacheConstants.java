package com.campus.wall.constant;

/**
 * 缓存常量
 */
public final class CacheConstants {

    private CacheConstants() {}

    /**
     * 缓存前缀
     */
    public static final String PREFIX = "campus:";

    /**
     * 用户信息缓存
     */
    public static final String USER_INFO = PREFIX + "user:info:";

    /**
     * 用户权限缓存
     */
    public static final String USER_PERMS = PREFIX + "user:perms:";

    /**
     * 帖子详情缓存
     */
    public static final String POST_DETAIL = PREFIX + "post:detail:";

    /**
     * 热门帖子缓存
     */
    public static final String POST_HOT = PREFIX + "post:hot";

    /**
     * 敏感词列表缓存
     */
    public static final String SENSITIVE_WORDS = PREFIX + "sensitive:words";

    /**
     * 验证码缓存
     */
    public static final String VERIFY_CODE = PREFIX + "verify:code:";

    /**
     * IP限流缓存
     */
    public static final String RATE_LIMIT_IP = PREFIX + "rate:ip:";

    /**
     * 用户发帖限流缓存
     */
    public static final String RATE_LIMIT_POST = PREFIX + "rate:post:";

    /**
     * 在线用户 Token 集合
     */
    public static final String ONLINE_TOKENS = PREFIX + "online:tokens";

    /**
     * 默认缓存TTL（秒）
     */
    public static final int DEFAULT_TTL = 300;

    /**
     * 长期缓存TTL（秒）
     */
    public static final int LONG_TTL = 3600;

    /**
     * 空值缓存TTL（秒）
     */
    public static final int NULL_TTL = 60;
}
