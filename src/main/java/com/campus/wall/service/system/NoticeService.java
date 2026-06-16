package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.NoticeDTO;
import com.campus.wall.vo.system.NoticeVO;

import java.util.List;

public interface NoticeService {

    /**
     * 获取公开公告（未登录可访问）
     */
    List<NoticeVO> getPublicNotices(int limit);

    /**
     * 获取用户可见公告
     */
    PageResult<NoticeVO> getVisibleNotices(int size, Integer lastPinned, String lastPublishedAt, Long lastId);

    /**
     * 获取公告详情（公开）
     */
    NoticeVO getPublicNoticeDetail(Long id);

    /**
     * 获取公告详情（用户可见）
     */
    NoticeVO getVisibleNoticeDetail(Long id);

    /**
     * 后台：获取所有公告（分页）
     */
    PageResult<NoticeVO> queryNotices(int page, int size, Integer status, String keyword);

    /**
     * 后台：获取公告详情
     */
    NoticeVO getNoticeDetail(Long id);

    /**
     * 创建公告（草稿）
     */
    NoticeVO create(NoticeDTO dto);

    /**
     * 更新公告
     */
    NoticeVO update(Long id, NoticeDTO dto);

    /**
     * 发布公告
     */
    void publish(Long id);

    /**
     * 下线公告
     */
    void offline(Long id);

    /**
     * 删除公告
     */
    void delete(Long id);
}
