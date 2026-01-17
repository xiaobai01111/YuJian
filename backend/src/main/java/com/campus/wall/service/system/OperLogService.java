package com.campus.wall.service.system;

/**
 * 操作日志服务接口
 */
public interface OperLogService {

    /**
     * 记录操作日志
     */
    void log(Long operatorId, String operatorName, String targetType, Long targetId, 
             String action, String reason, Object beforeValue, Object afterValue, String ipAddress);

    /**
     * 简化的日志记录
     */
    void log(String targetType, Long targetId, String action, String reason);
}
