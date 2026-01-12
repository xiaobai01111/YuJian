package com.campus.wall.service.system;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.system.AnnouncementDTO;
import com.campus.wall.vo.system.AnnouncementVO;

/**
 * 公告服务接口
 */
public interface AnnouncementService {

    /**
     * 发布公告
     */
    Long publish(AnnouncementDTO dto);

    /**
     * 更新公告
     */
    void update(Long id, AnnouncementDTO dto);

    /**
     * 下架公告
     */
    void unpublish(Long id);

    /**
     * 获取公告列表（前台）
     */
    PageResult<AnnouncementVO> getPublicAnnouncements(int page, int size);

    /**
     * 获取公告列表（管理端）
     */
    PageResult<AnnouncementVO> getAllAnnouncements(int page, int size);
}
