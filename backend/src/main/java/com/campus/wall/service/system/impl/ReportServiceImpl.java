package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.ReportBatchCreateDTO;
import com.campus.wall.dto.system.ReportBatchHandleDTO;
import com.campus.wall.dto.system.ReportCreateDTO;
import com.campus.wall.dto.system.ReportHandleDTO;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.system.Report;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.system.ReportMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.system.ReportService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.vo.common.BatchActionResultVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.system.ReportVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 举报服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final PostBoardMapper postBoardMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;
    private final DataScopeService dataScopeService;
    private final OperLogService operLogService;

    // 举报状态：0待处理 1已处理
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_HANDLED = 1;
    private static final int POST_STATUS_DELETED = 2;

    @Override
    @Transactional
    public Long createReport(ReportCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 检查帖子是否存在
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }

        // 检查是否已举报过
        Report existing = reportMapper.selectOne(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, userId)
                        .eq(Report::getPostId, dto.getPostId())
                        .eq(Report::getStatus, STATUS_PENDING)
        );
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已举报过该帖子，请等待处理");
        }

        // 创建举报记录
        Report report = new Report();
        report.setReporterId(userId);
        report.setPostId(dto.getPostId());
        report.setReason(dto.getReason());
        report.setStatus(STATUS_PENDING);
        report.setDeleted(0);

        reportMapper.insert(report);

        log.info("用户 {} 举报帖子 {}: {}", userId, dto.getPostId(), dto.getReason());
        return report.getId();
    }

    @Override
    @Transactional
    public BatchActionResultVO createReports(ReportBatchCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        BatchActionResultVO result = new BatchActionResultVO();

        if (dto == null || dto.getPostIds() == null || dto.getPostIds().isEmpty()) {
            result.setRequested(0);
            result.setSuccess(0);
            result.setSkipped(0);
            return result;
        }

        List<Long> uniqueIds = dto.getPostIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        result.setRequested(uniqueIds.size());

        if (uniqueIds.isEmpty()) {
            result.setSuccess(0);
            result.setSkipped(0);
            return result;
        }

        List<Post> posts = postMapper.selectBatchIds(uniqueIds);
        List<Long> validIds = posts.stream()
                .filter(post -> post.getStatus() == null || post.getStatus() != POST_STATUS_DELETED)
                .map(Post::getId)
                .collect(Collectors.toList());

        if (validIds.isEmpty()) {
            result.setSuccess(0);
            result.setSkipped(result.getRequested());
            return result;
        }

        List<Report> existing = reportMapper.selectList(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, userId)
                        .in(Report::getPostId, validIds)
                        .eq(Report::getStatus, STATUS_PENDING)
        );
        Set<Long> existingIds = existing.stream()
                .map(Report::getPostId)
                .collect(Collectors.toSet());

        int successCount = 0;
        for (Long postId : validIds) {
            if (existingIds.contains(postId)) {
                continue;
            }
            Report report = new Report();
            report.setReporterId(userId);
            report.setPostId(postId);
            report.setReason(dto.getReason());
            report.setStatus(STATUS_PENDING);
            report.setDeleted(0);
            reportMapper.insert(report);
            successCount++;
        }

        result.setSuccess(successCount);
        result.setSkipped(result.getRequested() - successCount);
        return result;
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, ReportHandleDTO dto) {
        handleReportInternal(reportId, dto.getResult(), dto.getRemark());
    }

    @Override
    @Transactional
    public void handleReports(ReportBatchHandleDTO dto) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        for (Long reportId : dto.getIds()) {
            handleReportInternal(reportId, dto.getResult(), dto.getRemark());
        }
    }

    @Override
    @Transactional
    public void deleteReportByAdmin(Long reportId, String reason) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该举报");
        }
        if (!Objects.equals(operatorId, report.getReporterId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        if (report.getDeleted() != null && report.getDeleted() == 1) {
            return;
        }
        report.setDeleted(1);
        reportMapper.updateById(report);
        operLogService.log(operatorId, null, "report", reportId, "delete", reason, null, null, null);
        log.info("管理员删除举报: {}", reportId);
    }

    @Override
    @Transactional
    public void restoreReportByAdmin(Long reportId, String reason) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权恢复该举报");
        }
        if (!Objects.equals(operatorId, report.getReporterId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        if (report.getDeleted() == null || report.getDeleted() == 0) {
            return;
        }
        report.setDeleted(0);
        reportMapper.updateById(report);
        operLogService.log(operatorId, null, "report", reportId, "restore", reason, null, null, null);
        log.info("管理员恢复举报: {}", reportId);
    }

    @Override
    @Transactional
    public void purgeReportByAdmin(Long reportId, String reason) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            return;
        }
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!dataScopeService.canAccessUser(operatorId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该举报");
        }
        if (!Objects.equals(operatorId, report.getReporterId()) && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写操作原因");
        }
        if (report.getDeleted() == null || report.getDeleted() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报未在回收站");
        }
        reportMapper.deleteById(reportId);
        operLogService.log(operatorId, null, "report", reportId, "purge", reason, null, null, null);
        log.info("管理员彻底删除举报: {}", reportId);
    }

    @Override
    public PageResult<ReportVO> queryReports(Integer status, int page, int size) {
        Page<Report> reportPage = new Page<>(page, size);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getDeleted, 0);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        applyReportDataScope(wrapper);
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(reportPage, wrapper);

        List<ReportVO> records = result.getRecords().stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<ReportVO> queryDeletedReports(Integer status, int page, int size) {
        Page<Report> reportPage = new Page<>(page, size);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getDeleted, 1);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        applyReportDataScope(wrapper);
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(reportPage, wrapper);
        List<ReportVO> records = result.getRecords().stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ReportVO getReportDetail(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        if (report.getDeleted() != null && report.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        ensureCanAccessReport(report);
        return toReportVO(report);
    }

    private ReportVO toReportVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setResult(report.getResult());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setHandledAt(report.getHandledAt());

        // 举报者信息
        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null) {
            UserVO reporterVO = new UserVO();
            reporterVO.setId(reporter.getId());
            reporterVO.setUsername(reporter.getUsername());
            reporterVO.setNickname(reporter.getNickname());
            vo.setReporter(reporterVO);
        }

        // 帖子信息（简化）
        Post post = postMapper.selectById(report.getPostId());
        if (post != null) {
            PostVO postVO = new PostVO();
            postVO.setId(post.getId());
            postVO.setTitle(post.getTitle());
            List<String> boards = postBoardMapper.selectList(
                            new LambdaQueryWrapper<PostBoard>()
                                    .eq(PostBoard::getPostId, post.getId())
                    ).stream()
                    .map(PostBoard::getBoard)
                    .collect(Collectors.toList());
            if (!boards.isEmpty()) {
                postVO.setBoard(boards.get(0));
                postVO.setBoards(boards);
            } else {
                postVO.setBoard(post.getBoard());
                postVO.setBoards(List.of());
            }
            vo.setPost(postVO);
        }

        // 处理者信息
        if (report.getHandlerId() != null) {
            User handler = userMapper.selectById(report.getHandlerId());
            if (handler != null) {
                UserVO handlerVO = new UserVO();
                handlerVO.setId(handler.getId());
                handlerVO.setUsername(handler.getUsername());
                handlerVO.setNickname(handler.getNickname());
                vo.setHandler(handlerVO);
            }
        }

        return vo;
    }

    private void applyReportDataScope(LambdaQueryWrapper<Report> wrapper) {
        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(userId);
        if (scope.isAllowAll()) {
            return;
        }
        List<Long> allowedUserIds = dataScopeService.resolveAllowedUserIds(scope, userId);
        if (allowedUserIds.isEmpty()) {
            wrapper.eq(Report::getId, -1L);
            return;
        }
        List<Long> postIds = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .select(Post::getId)
                        .in(Post::getUserId, allowedUserIds)
        ).stream().map(Post::getId).collect(Collectors.toList());
        if (postIds.isEmpty()) {
            wrapper.eq(Report::getId, -1L);
            return;
        }
        wrapper.in(Report::getPostId, postIds);
    }

    private void ensureCanAccessReport(Report report) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!dataScopeService.canAccessUser(operatorId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看该举报");
        }
    }

    private void handleReportInternal(Long reportId, String result, String remark) {
        Long handlerId = StpUtil.getLoginIdAsLong();

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        if (report.getDeleted() != null && report.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已删除");
        }
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!dataScopeService.canAccessUser(handlerId, post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该举报");
        }
        if (!handlerId.equals(post.getUserId()) && !StringUtils.hasText(remark)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写处理说明");
        }
        if (report.getStatus() == STATUS_HANDLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }

        report.setStatus(STATUS_HANDLED);
        report.setHandlerId(handlerId);
        report.setResult(result);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        String normalized = result != null ? result.toLowerCase() : "";
        if (normalized.contains("核实") || normalized.contains("违规") || normalized.contains("verified")) {
            creditService.penalizeForFraud(post.getUserId());
            log.info("举报核实，扣除用户 {} 信用分", post.getUserId());
        }

        operLogService.log(handlerId, null, "report", reportId, "handle", remark, null, null, null);
        log.info("管理员 {} 处理举报 {}: {}", handlerId, reportId, result);
    }
}
