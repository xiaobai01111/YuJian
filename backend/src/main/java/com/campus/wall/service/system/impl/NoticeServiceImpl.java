package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.NoticeDTO;
import com.campus.wall.entity.system.SysNotice;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysNoticeMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.NoticeService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.vo.system.NoticeVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final SysNoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final OperLogService operLogService;
    private final ObjectMapper objectMapper;

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PUBLIC_FETCH = 50;
    
    // 公开公告缓存（60秒过期）
    private volatile List<NoticeVO> publicNoticesCache = null;
    private volatile long publicNoticesCacheTime = 0;
    private static final long CACHE_TTL_MS = 60_000;

    @Override
    public List<NoticeVO> getPublicNotices(int limit) {
        int effectiveLimit = normalizeSize(limit, 10);
        int fetchSize = MAX_PUBLIC_FETCH;
        long now = System.currentTimeMillis();
        
        // 使用缓存（60秒内有效）
        if (publicNoticesCache != null && (now - publicNoticesCacheTime) < CACHE_TTL_MS) {
            return publicNoticesCache.stream().limit(effectiveLimit).collect(Collectors.toList());
        }
        
        List<SysNotice> notices = noticeMapper.selectPublicNotices(fetchSize, LocalDateTime.now());
        List<NoticeVO> result = toVOList(notices);
        
        // 更新缓存
        publicNoticesCache = result;
        publicNoticesCacheTime = now;
        
        return result.stream().limit(effectiveLimit).collect(Collectors.toList());
    }
    
    // 发布/下线/删除公告时清除缓存
    private void clearPublicNoticesCache() {
        publicNoticesCache = null;
        publicNoticesCacheTime = 0;
    }

    @Override
    public PageResult<NoticeVO> getVisibleNotices(int page, int size) {
        int currentPage = normalizePage(page);
        int pageSize = normalizeSize(size, 10);
        
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        LocalDateTime now = LocalDateTime.now();
        
        int offset = (currentPage - 1) * pageSize;
        List<SysNotice> notices = noticeMapper.selectVisibleNotices(userId, deptId, now, pageSize, offset);
        long total = noticeMapper.countVisibleNotices(userId, deptId, now);

        List<NoticeVO> pageData = toVOList(notices);
        return PageResult.of(pageData, total, pageSize, currentPage);
    }

    @Override
    public NoticeVO getPublicNoticeDetail(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (notice.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        // 允许 scope_type 为 NULL/空/ALL（与列表查询一致），统一返回 404 避免泄露受限公告存在
        if (!isPublicScope(notice)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (!isInValidPeriod(notice)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return toVO(notice);
    }

    @Override
    public NoticeVO getVisibleNoticeDetail(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (notice.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (!isInValidPeriod(notice)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告已过期");
        }
        if (!canUserViewNotice(notice)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此公告");
        }
        return toVO(notice);
    }

    @Override
    public PageResult<NoticeVO> queryNotices(int page, int size, Integer status, String keyword) {
        int currentPage = normalizePage(page);
        int pageSize = normalizeSize(size, 10);
        LambdaQueryWrapper<SysNotice> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SysNotice::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(SysNotice::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(SysNotice::getIsPinned)
               .orderByDesc(SysNotice::getCreatedAt);

        Page<SysNotice> pageResult = noticeMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);
        List<NoticeVO> voList = toVOList(pageResult.getRecords());
        return PageResult.of(voList, pageResult.getTotal(), pageSize, currentPage);
    }

    @Override
    public NoticeVO getNoticeDetail(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        return toVO(notice);
    }

    @Override
    @Transactional
    public NoticeVO create(NoticeDTO dto) {
        validateDTO(dto);
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        SysNotice notice = new SysNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(sanitizeContent(dto.getContent()));
        notice.setScopeType(dto.getScopeType() != null ? dto.getScopeType() : "ALL");
        notice.setScopeIds(toJson(dto.getScopeIds()));
        notice.setIsPinned(dto.getIsPinned() != null ? dto.getIsPinned() : false);
        notice.setStartAt(dto.getStartAt());
        notice.setEndAt(dto.getEndAt());
        notice.setStatus(STATUS_DRAFT);
        notice.setCreatedBy(operatorId);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        
        noticeMapper.insert(notice);
        
        operLogService.log(operatorId, null, "notice", notice.getId(), "create", null,
                null, Map.of("title", notice.getTitle(), "status", "草稿"), null);
        
        return toVO(notice);
    }

    @Override
    @Transactional
    public NoticeVO update(Long id, NoticeDTO dto) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        validateDTO(dto);
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        Map<String, Object> before = Map.of(
            "title", notice.getTitle(),
            "scopeType", notice.getScopeType(),
            "isPinned", notice.getIsPinned()
        );
        
        notice.setTitle(dto.getTitle());
        notice.setContent(sanitizeContent(dto.getContent()));
        notice.setScopeType(dto.getScopeType() != null ? dto.getScopeType() : "ALL");
        notice.setScopeIds(toJson(dto.getScopeIds()));
        notice.setIsPinned(dto.getIsPinned() != null ? dto.getIsPinned() : false);
        notice.setStartAt(dto.getStartAt());
        notice.setEndAt(dto.getEndAt());
        notice.setUpdatedBy(operatorId);
        notice.setUpdatedAt(LocalDateTime.now());
        
        noticeMapper.updateById(notice);
        clearPublicNoticesCache();
        
        Map<String, Object> after = Map.of(
            "title", notice.getTitle(),
            "scopeType", notice.getScopeType(),
            "isPinned", notice.getIsPinned()
        );
        operLogService.log(operatorId, null, "notice", id, "update", null, before, after, null);
        
        return toVO(notice);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (notice.getStatus() == STATUS_PUBLISHED) {
            throw new BusinessException("公告已发布");
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        int oldStatus = notice.getStatus();
        
        notice.setStatus(STATUS_PUBLISHED);
        notice.setPublishedAt(LocalDateTime.now());
        notice.setUpdatedBy(operatorId);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        clearPublicNoticesCache();
        
        operLogService.log(operatorId, null, "notice", id, "publish", null,
                Map.of("status", getStatusText(oldStatus)),
                Map.of("status", "已发布"), null);
    }

    @Override
    @Transactional
    public void offline(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (notice.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException("只能下线已发布的公告");
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        
        notice.setStatus(STATUS_OFFLINE);
        notice.setUpdatedBy(operatorId);
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        clearPublicNoticesCache();
        
        operLogService.log(operatorId, null, "notice", id, "offline", null,
                Map.of("status", "已发布"),
                Map.of("status", "已下线"), null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        Map<String, Object> before = Map.of(
            "title", notice.getTitle(),
            "status", getStatusText(notice.getStatus())
        );
        
        noticeMapper.deleteById(id);
        clearPublicNoticesCache();
        
        operLogService.log(operatorId, null, "notice", id, "delete", null, before, null, null);
    }

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 50000;
    private static final int MAX_SCOPE_IDS = 1000;

    private void validateDTO(NoticeDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("标题不能为空");
        }
        if (dto.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new BusinessException("标题长度不能超过" + MAX_TITLE_LENGTH + "字符");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new BusinessException("内容不能为空");
        }
        if (dto.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("内容长度不能超过" + MAX_CONTENT_LENGTH + "字符");
        }
        String scopeType = dto.getScopeType();
        if (scopeType != null && !List.of("ALL", "DEPT", "USERS").contains(scopeType)) {
            throw new BusinessException("可见范围类型无效");
        }
        if (("DEPT".equals(scopeType) || "USERS".equals(scopeType)) 
                && (dto.getScopeIds() == null || dto.getScopeIds().isEmpty())) {
            throw new BusinessException("部门/用户可见范围必须指定ID列表");
        }
        if (dto.getScopeIds() != null && dto.getScopeIds().size() > MAX_SCOPE_IDS) {
            throw new BusinessException("可见范围ID数量不能超过" + MAX_SCOPE_IDS);
        }
        if (dto.getStartAt() != null && dto.getEndAt() != null && dto.getStartAt().isAfter(dto.getEndAt())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
    }

    private boolean isInValidPeriod(SysNotice notice) {
        LocalDateTime now = LocalDateTime.now();
        if (notice.getStartAt() != null && now.isBefore(notice.getStartAt())) {
            return false;
        }
        if (notice.getEndAt() != null && now.isAfter(notice.getEndAt())) {
            return false;
        }
        return true;
    }

    private boolean canUserViewNotice(SysNotice notice) {
        String scopeType = notice.getScopeType();
        // NULL/空/ALL 均视为全校可见
        if (scopeType == null || scopeType.isEmpty() || "ALL".equals(scopeType)) {
            return true;
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        List<Long> scopeIds = fromJsonStrict(notice.getScopeIds());
        
        if ("DEPT".equals(scopeType)) {
            return user != null && user.getDeptId() != null && scopeIds.contains(user.getDeptId());
        }
        if ("USERS".equals(scopeType)) {
            return scopeIds.contains(userId);
        }
        return false;
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<Long> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            // 返回空列表而非抛异常，避免单条坏数据拖垮整个列表
            return Collections.emptyList();
        }
    }

    private List<Long> fromJsonStrict(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("公告可见范围数据损坏，请联系管理员");
        }
    }

    private List<NoticeVO> toVOList(List<SysNotice> notices) {
        if (notices == null || notices.isEmpty()) {
            return List.of();
        }
        Map<Long, User> creatorMap = loadCreators(notices);
        return notices.stream()
                .map(notice -> toVO(notice, creatorMap))
                .collect(Collectors.toList());
    }

    private Map<Long, User> loadCreators(List<SysNotice> notices) {
        List<Long> creatorIds = notices.stream()
                .map(SysNotice::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (creatorIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(creatorIds);
        return users.stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private NoticeVO toVO(SysNotice notice) {
        return toVO(notice, Map.of());
    }

    private NoticeVO toVO(SysNotice notice, Map<Long, User> creatorMap) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        vo.setStatusText(getStatusText(notice.getStatus()));
        vo.setScopeTypeText(getScopeTypeText(notice.getScopeType()));
        vo.setScopeIds(fromJson(notice.getScopeIds()));
        vo.setContent(sanitizeContent(vo.getContent()));

        if (notice.getCreatedByName() != null && !notice.getCreatedByName().isBlank()) {
            vo.setCreatedByName(notice.getCreatedByName());
        } else if (notice.getCreatedBy() != null) {
            User creator = creatorMap.get(notice.getCreatedBy());
            if (creator != null) {
                vo.setCreatedByName(creator.getNickname());
            }
        }
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_PUBLISHED -> "已发布";
            case STATUS_OFFLINE -> "已下线";
            default -> "未知";
        };
    }

    private String getScopeTypeText(String scopeType) {
        if (scopeType == null) return "全校";
        return switch (scopeType) {
            case "ALL" -> "全校";
            case "DEPT" -> "指定部门";
            case "USERS" -> "指定用户";
            default -> "全校";
        };
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size, int defaultSize) {
        int actual = size > 0 ? size : defaultSize;
        return Math.min(actual, MAX_PAGE_SIZE);
    }

    private boolean isPublicScope(SysNotice notice) {
        String scopeType = notice.getScopeType();
        return scopeType == null || scopeType.isBlank() || "ALL".equals(scopeType);
    }

    private String sanitizeContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        return Jsoup.clean(content, Safelist.basic());
    }
}
