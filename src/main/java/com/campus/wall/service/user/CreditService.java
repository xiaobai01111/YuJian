package com.campus.wall.service.user;

/**
 * 信用分服务接口
 */
public interface CreditService {

    /**
     * 获取用户信用分
     */
    Integer getCreditScore(Long userId);

    /**
     * 检查用户是否可以在市集发帖（信用分 >= 60）
     */
    boolean canPostInMarket(Long userId);

    /**
     * 举报核实扣分 (-20)
     */
    void penalizeForFraud(Long userId);

    /**
     * 交易成功加分 (+5)
     */
    void rewardForTransaction(Long userId);

    /**
     * 每日活跃加分 (+1，幂等）
     */
    void rewardDailyActive(Long userId);

    /**
     * 调整信用分（内部使用）
     * @param userId 用户ID
     * @param delta 变化值（正数加分，负数扣分）
     * @param reason 原因
     */
    void adjustCreditScore(Long userId, int delta, String reason);
}
