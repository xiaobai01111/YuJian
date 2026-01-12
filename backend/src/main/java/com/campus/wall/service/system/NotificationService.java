package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.vo.system.NotificationVO;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 获取用户通知列表
     */
    PageResult<NotificationVO> getUserNotifications(Long userId, int page, int size);

    /**
     * 获取未读通知数量
     */
    long getUnreadCount(Long userId);

    /**
     * 标记通知为已读
     */
    void markAsRead(Long notificationId);

    /**
     * 标记所有通知为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 创建点赞通知
     */
    void createLikeNotification(Long postId, Long likerId);

    /**
     * 创建评论通知
     */
    void createCommentNotification(Long postId, Long commenterId, String content);

    /**
     * 创建系统通知
     */
    void createSystemNotification(Long userId, String title, String content);
}
