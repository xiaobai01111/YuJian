package com.campus.wall.service.user.impl;

import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.user.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 信用分服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    // 信用分上限
    private static final int MAX_CREDIT_SCORE = 100;
    // 信用分下限
    private static final int MIN_CREDIT_SCORE = 0;
    // 市集发帖最低信用分
    private static final int MARKET_MIN_CREDIT = 60;

    // 举报核实扣分
    private static final int FRAUD_PENALTY = -20;
    // 交易成功加分
    private static final int TRANSACTION_REWARD = 5;
    // 每日活跃加分
    private static final int DAILY_ACTIVE_REWARD = 1;

    // Redis key 前缀
    private static final String DAILY_ACTIVE_KEY_PREFIX = "credit:daily_active:";

    @Override
    public Integer getCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user.getCreditScore() != null ? user.getCreditScore() : MAX_CREDIT_SCORE;
    }

    @Override
    public boolean canPostInMarket(Long userId) {
        Integer creditScore = getCreditScore(userId);
        return creditScore >= MARKET_MIN_CREDIT;
    }

    @Override
    @Transactional
    public void penalizeForFraud(Long userId) {
        adjustCreditScore(userId, FRAUD_PENALTY, "举报核实扣分");
    }

    @Override
    @Transactional
    public void rewardForTransaction(Long userId) {
        adjustCreditScore(userId, TRANSACTION_REWARD, "交易成功加分");
    }

    @Override
    @Transactional
    public void rewardDailyActive(Long userId) {
        // 检查今日是否已奖励（幂等）
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String key = DAILY_ACTIVE_KEY_PREFIX + userId + ":" + today;

        Boolean alreadyRewarded = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(alreadyRewarded)) {
            log.debug("用户 {} 今日已获得活跃奖励", userId);
            return;
        }

        // 设置标记，24小时后过期
        redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);

        adjustCreditScore(userId, DAILY_ACTIVE_REWARD, "每日活跃加分");
    }

    @Override
    @Transactional
    public void adjustCreditScore(Long userId, int delta, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        int currentScore = user.getCreditScore() != null ? user.getCreditScore() : MAX_CREDIT_SCORE;
        int newScore = currentScore + delta;

        // 限制在 [MIN, MAX] 范围内
        newScore = Math.max(MIN_CREDIT_SCORE, Math.min(MAX_CREDIT_SCORE, newScore));

        user.setCreditScore(newScore);
        userMapper.updateById(user);

        log.info("用户 {} 信用分变更: {} -> {} ({})", userId, currentScore, newScore, reason);
    }
}
