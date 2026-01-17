package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.entity.system.SysOperLog;
import com.campus.wall.mapper.system.SysOperLogMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.OperLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper operLogMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void log(Long operatorId, String operatorName, String targetType, Long targetId,
                    String action, String reason, Object beforeValue, Object afterValue, String ipAddress) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setOperatorId(operatorId);
            operLog.setOperatorName(operatorName);
            operLog.setTargetType(targetType);
            operLog.setTargetId(targetId);
            operLog.setAction(action);
            operLog.setReason(reason);
            operLog.setIpAddress(ipAddress);

            if (beforeValue != null) {
                operLog.setBeforeValue(objectMapper.writeValueAsString(beforeValue));
            }
            if (afterValue != null) {
                operLog.setAfterValue(objectMapper.writeValueAsString(afterValue));
            }

            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public void log(String targetType, Long targetId, String action, String reason) {
        try {
            Long operatorId = StpUtil.getLoginIdAsLong();
            var user = userMapper.selectById(operatorId);
            String operatorName = user != null ? user.getUsername() : null;
            log(operatorId, operatorName, targetType, targetId, action, reason, null, null, null);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
}
