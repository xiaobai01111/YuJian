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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final PlatformTransactionManager transactionManager;

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
    public void handleReports(ReportBatchHandleDTO dto) {
        if (dto == null || dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<Long> failedIds = new ArrayList<>();
        for (Long reportId : dto.getIds()) {
            try {
                txTemplate.execute(status -> {
                    handleReportInternal(reportId, dto.getResult(), dto.getRemark());
                    return null;
                });
            } catch (Exception ex) {
                failedIds.add(reportId);
                log.warn("批量处理举报失败 reportId={} reason={}", reportId, ex.getMessage());
            }
        }
        if (!failedIds.isEmpty()) {
            throw new BusinessException("部分举报处理失败：" + failedIds.size() + " 条");
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
        if (!dataScopeService.canAccessUser(operatorId, report.getReporterId())) {
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
        if (!dataScopeService.canAccessUser(operatorId, report.getReporterId())) {
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
        if (!dataScopeService.canAccessUser(operatorId, report.getReporterId())) {
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

        List<ReportVO> records = toReportVOList(result.getRecords());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
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
        List<ReportVO> records = toReportVOList(result.getRecords());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
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
        if (report == null) {
            return null;
        }
        Map<Long, User> userMap = new HashMap<>();
        Map<Long, Post> postMap = new HashMap<>();
        Map<Long, List<String>> boardMap = new HashMap<>();

        if (report.getReporterId() != null) {
            User reporter = userMapper.selectById(report.getReporterId());
            if (reporter != null) {
                userMap.put(reporter.getId(), reporter);
            }
        }
        if (report.getHandlerId() != null) {
            User handler = userMapper.selectById(report.getHandlerId());
            if (handler != null) {
                userMap.put(handler.getId(), handler);
            }
        }
        if (report.getPostId() != null) {
            Post post = postMapper.selectById(report.getPostId());
            if (post != null) {
                postMap.put(post.getId(), post);
                List<PostBoard> boards = postBoardMapper.selectList(
                    new LambdaQueryWrapper<PostBoard>().eq(PostBoard::getPostId, post.getId())
                );
                if (boards != null && !boards.isEmpty()) {
                    List<String> boardNames = new ArrayList<>();
                    for (PostBoard board : boards) {
                        if (board != null && board.getBoard() != null) {
                            boardNames.add(board.getBoard());
                        }
                    }
                    if (!boardNames.isEmpty()) {
                        boardMap.put(post.getId(), boardNames);
                    }
                }
            }
        }
        return toReportVO(report, userMap, postMap, boardMap);
    }

    private List<ReportVO> toReportVOList(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> postIdSet = new HashSet<>();
        for (Report report : reports) {
            if (report == null) {
                continue;
            }
            if (report.getReporterId() != null) {
                userIdSet.add(report.getReporterId());
            }
            if (report.getHandlerId() != null) {
                userIdSet.add(report.getHandlerId());
            }
            if (report.getPostId() != null) {
                postIdSet.add(report.getPostId());
            }
        }
        Map<Long, User> userMap = userIdSet.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(new ArrayList<>(userIdSet)).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        Map<Long, Post> postMap = postIdSet.isEmpty()
            ? Map.of()
            : postMapper.selectBatchIds(new ArrayList<>(postIdSet)).stream()
                .collect(Collectors.toMap(Post::getId, post -> post, (a, b) -> a));
        Map<Long, List<String>> boardMap = loadBoardsByPostIds(new ArrayList<>(postIdSet));
        return reports.stream()
            .map(report -> toReportVO(report, userMap, postMap, boardMap))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Map<Long, List<String>> loadBoardsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }
        List<PostBoard> boards = postBoardMapper.selectList(
            new LambdaQueryWrapper<PostBoard>().in(PostBoard::getPostId, postIds)
        );
        if (boards == null || boards.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> result = new HashMap<>();
        for (PostBoard board : boards) {
            if (board == null || board.getPostId() == null || board.getBoard() == null) {
                continue;
            }
            result.computeIfAbsent(board.getPostId(), key -> new ArrayList<>()).add(board.getBoard());
        }
        return result;
    }

    private ReportVO toReportVO(Report report,
                                Map<Long, User> userMap,
                                Map<Long, Post> postMap,
                                Map<Long, List<String>> boardMap) {
        if (report == null) {
            return null;
        }
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setResult(report.getResult());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setHandledAt(report.getHandledAt());

        // 举报者信息
        User reporter = report.getReporterId() == null ? null : userMap.get(report.getReporterId());
        if (reporter != null) {
            UserVO reporterVO = new UserVO();
            reporterVO.setId(reporter.getId());
            reporterVO.setUsername(reporter.getUsername());
            reporterVO.setNickname(reporter.getNickname());
            vo.setReporter(reporterVO);
        }

        // 帖子信息（简化）
        Post post = report.getPostId() == null ? null : postMap.get(report.getPostId());
        if (post != null) {
            PostVO postVO = new PostVO();
            postVO.setId(post.getId());
            postVO.setTitle(post.getTitle());
            List<String> boards = boardMap.get(post.getId());
            if (boards != null && !boards.isEmpty()) {
                postVO.setBoard(boards.getFirst());
                postVO.setBoards(boards);
            } else {
                postVO.setBoard(post.getBoard());
                postVO.setBoards(List.of());
            }
            vo.setPost(postVO);
        }

        // 处理者信息
        if (report.getHandlerId() != null) {
            User handler = userMap.get(report.getHandlerId());
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
        String deptScopeSql = dataScopeService.buildUserScopeExistsSql(scope, "reports.reporter_id");
        if (deptScopeSql == null) {
            if (scope.isAllowSelf() && userId != null) {
                wrapper.eq(Report::getReporterId, userId);
            } else {
                wrapper.eq(Report::getId, -1L);
            }
            return;
        }
        if (scope.isAllowSelf() && userId != null) {
            wrapper.and(w -> w.eq(Report::getReporterId, userId).or().apply(deptScopeSql));
        } else {
            wrapper.apply(deptScopeSql);
        }
    }

    private void ensureCanAccessReport(Report report) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        Post post = postMapper.selectById(report.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "帖子不存在");
        }
        if (!dataScopeService.canAccessUser(operatorId, report.getReporterId())) {
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
        if (!dataScopeService.canAccessUser(handlerId, report.getReporterId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权处理该举报");
        }
        if (!handlerId.equals(report.getReporterId()) && !StringUtils.hasText(remark)) {
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
