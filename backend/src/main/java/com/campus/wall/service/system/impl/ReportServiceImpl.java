package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.ReportCreateDTO;
import com.campus.wall.dto.system.ReportHandleDTO;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.system.Report;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.system.ReportMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.ReportService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.system.ReportVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    private final UserMapper userMapper;
    private final CreditService creditService;

    // 举报状态：0待处理 1已处理
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_HANDLED = 1;

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

        reportMapper.insert(report);

        log.info("用户 {} 举报帖子 {}: {}", userId, dto.getPostId(), dto.getReason());
        return report.getId();
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, ReportHandleDTO dto) {
        Long handlerId = StpUtil.getLoginIdAsLong();

        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }

        if (report.getStatus() == STATUS_HANDLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }

        report.setStatus(STATUS_HANDLED);
        report.setHandlerId(handlerId);
        report.setResult(dto.getResult());
        report.setHandledAt(LocalDateTime.now());

        reportMapper.updateById(report);

        // 如果举报核实（结果包含"核实"或"违规"），扣除被举报用户信用分
        String result = dto.getResult().toLowerCase();
        if (result.contains("核实") || result.contains("违规") || result.contains("verified")) {
            Post post = postMapper.selectById(report.getPostId());
            if (post != null) {
                creditService.penalizeForFraud(post.getUserId());
                log.info("举报核实，扣除用户 {} 信用分", post.getUserId());
            }
        }

        log.info("管理员 {} 处理举报 {}: {}", handlerId, reportId, dto.getResult());
    }

    @Override
    public PageResult<ReportVO> queryReports(Integer status, int page, int size) {
        Page<Report> reportPage = new Page<>(page, size);

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
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
            postVO.setBoard(post.getBoard());
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
}
