package com.campus.wall.constant;

/**
 * 信用分变动常量
 */
public final class CreditConstants {

    private CreditConstants() {}

    /**
     * 交易完成奖励
     */
    public static final int TRADE_COMPLETE_REWARD = 5;

    /**
     * 每日活跃奖励
     */
    public static final int DAILY_ACTIVE_REWARD = 1;

    /**
     * 欺诈行为惩罚
     */
    public static final int FRAUD_PENALTY = -20;

    /**
     * 违规内容惩罚
     */
    public static final int VIOLATION_PENALTY = -10;

    /**
     * 被举报成功惩罚
     */
    public static final int REPORT_PENALTY = -5;
}
