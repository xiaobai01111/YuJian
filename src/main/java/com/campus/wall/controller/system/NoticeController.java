package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.NoticeDTO;
import com.campus.wall.service.system.NoticeService;
import com.campus.wall.vo.system.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // ========== 公开接口（未登录可访问）==========

    /**
     * 获取公开公告列表（首页公告栏）
     */
    @GetMapping("/api/v1/notices/public")
    public R<List<NoticeVO>> getPublicNotices(
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(noticeService.getPublicNotices(limit));
    }

    /**
     * 获取公开公告详情
     */
    @GetMapping("/api/v1/notices/public/{id}")
    public R<NoticeVO> getPublicNoticeDetail(@PathVariable Long id) {
        return R.ok(noticeService.getPublicNoticeDetail(id));
    }

    // ========== 登录用户接口 ==========

    /**
     * 获取用户可见公告列表
     */
    @SaCheckLogin
    @GetMapping("/api/v1/notices")
    public R<PageResult<NoticeVO>> getVisibleNotices(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer lastPinned,
            @RequestParam(required = false) String lastPublishedAt,
            @RequestParam(required = false) Long lastId) {
        return R.ok(noticeService.getVisibleNotices(size, lastPinned, lastPublishedAt, lastId));
    }

    /**
     * 获取用户可见公告详情
     */
    @SaCheckLogin
    @GetMapping("/api/v1/notices/{id}")
    public R<NoticeVO> getVisibleNoticeDetail(@PathVariable Long id) {
        return R.ok(noticeService.getVisibleNoticeDetail(id));
    }

    // ========== 后台管理接口 ==========

    /**
     * 后台：查询公告列表
     */
    @GetMapping("/api/v1/console/notices")
    public R<PageResult<NoticeVO>> queryNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return R.ok(noticeService.queryNotices(page, size, status, keyword));
    }

    /**
     * 后台：获取公告详情
     */
    @GetMapping("/api/v1/console/notices/{id}")
    public R<NoticeVO> getNoticeDetail(@PathVariable Long id) {
        return R.ok(noticeService.getNoticeDetail(id));
    }

    /**
     * 后台：创建公告（草稿）
     */
    @PostMapping("/api/v1/console/notices")
    public R<NoticeVO> create(@RequestBody NoticeDTO dto) {
        return R.ok(noticeService.create(dto));
    }

    /**
     * 后台：更新公告
     */
    @PutMapping("/api/v1/console/notices/{id}")
    public R<NoticeVO> update(@PathVariable Long id, @RequestBody NoticeDTO dto) {
        return R.ok(noticeService.update(id, dto));
    }

    /**
     * 后台：发布公告
     */
    @PutMapping("/api/v1/console/notices/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return R.ok();
    }

    /**
     * 后台：下线公告
     */
    @PutMapping("/api/v1/console/notices/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return R.ok();
    }

    /**
     * 后台：删除公告
     */
    @DeleteMapping("/api/v1/console/notices/{id}")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return R.ok();
    }
}
