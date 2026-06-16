package com.campus.wall.service.system;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.system.ReportBatchCreateDTO;
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
import com.campus.wall.service.system.impl.ReportServiceImpl;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.vo.system.ReportVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostBoardMapper postBoardMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CreditService creditService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private OperLogService operLogService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void createReport_postMissing_throwsNotFound() {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setPostId(10L);
        dto.setReason("spam");
        when(postMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> reportService.createReport(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createReport_duplicatePending_throwsBadRequest() {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setPostId(10L);
        dto.setReason("spam");
        when(postMapper.selectById(10L)).thenReturn(post(10L, 2L, 0));
        when(reportMapper.selectOne(any())).thenReturn(new Report());

        assertThatThrownBy(() -> reportService.createReport(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void createReport_success_insertsRecord() {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setPostId(10L);
        dto.setReason("spam");
        when(postMapper.selectById(10L)).thenReturn(post(10L, 2L, 0));
        when(reportMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(100L);
            return 1;
        }).when(reportMapper).insert(any(Report.class));

        Long id = reportService.createReport(dto);

        assertThat(id).isEqualTo(100L);
        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    void createReports_emptyInput_returnsZero() {
        var result = reportService.createReports(new ReportBatchCreateDTO());
        assertThat(result.getRequested()).isEqualTo(0);
        assertThat(result.getSuccess()).isEqualTo(0);
    }

    @Test
    void createReports_filtersDeletedAndExisting() {
        ReportBatchCreateDTO dto = new ReportBatchCreateDTO();
        dto.setPostIds(List.of(10L, 11L, 10L));
        dto.setReason("spam");
        when(postMapper.selectBatchIds(List.of(10L, 11L)))
            .thenReturn(List.of(post(10L, 2L, 0), post(11L, 3L, 2)));

        Report existing = new Report();
        existing.setPostId(10L);
        when(reportMapper.selectList(any())).thenReturn(List.of(existing));

        var result = reportService.createReports(dto);

        assertThat(result.getRequested()).isEqualTo(2);
        assertThat(result.getSuccess()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(2);
    }

    @Test
    void handleReport_missing_throwsNotFound() {
        when(reportMapper.selectById(1L)).thenReturn(null);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规");
        dto.setRemark("说明");

        assertThatThrownBy(() -> reportService.handleReport(1L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void handleReport_deleted_throwsBadRequest() {
        Report report = report(1L, 2L, 10L, 0, 1);
        when(reportMapper.selectById(1L)).thenReturn(report);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规");
        dto.setRemark("说明");

        assertThatThrownBy(() -> reportService.handleReport(1L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void handleReport_forbiddenByDataScope_throwsForbidden() {
        Report report = report(1L, 2L, 10L, 0, 0);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(false);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规");
        dto.setRemark("说明");

        assertThatThrownBy(() -> reportService.handleReport(1L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void handleReport_nonOwnerWithoutRemark_throwsBadRequest() {
        Report report = report(1L, 2L, 10L, 0, 0);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规");
        dto.setRemark(" ");

        assertThatThrownBy(() -> reportService.handleReport(1L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void handleReport_alreadyHandled_throwsBadRequest() {
        Report report = report(1L, 2L, 10L, 1, 0);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规");
        dto.setRemark("说明");

        assertThatThrownBy(() -> reportService.handleReport(1L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void handleReport_success_penalizesAndLogs() {
        Report report = report(1L, 2L, 10L, 0, 0);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 9L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        ReportHandleDTO dto = new ReportHandleDTO();
        dto.setResult("违规属实");
        dto.setRemark("已核实");

        reportService.handleReport(1L, dto);

        assertThat(report.getStatus()).isEqualTo(1);
        assertThat(report.getHandlerId()).isEqualTo(1L);
        verify(reportMapper).updateById(report);
        verify(creditService).penalizeForFraud(9L);
        verify(operLogService).log(eq(1L), eq(null), eq("report"), eq(1L), eq("handle"), eq("已核实"), eq(null), eq(null), eq(null));
    }

    @Test
    void deleteReportByAdmin_notFound_throws() {
        when(reportMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> reportService.deleteReportByAdmin(1L, "x"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteReportByAdmin_reasonRequiredForNonOwner_throws() {
        Report report = report(1L, 2L, 10L, 0, 0);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.deleteReportByAdmin(1L, " "))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void restoreAndPurge_flowChecks() {
        Report report = report(1L, 2L, 10L, 0, 1);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        reportService.restoreReportByAdmin(1L, "恢复");
        assertThat(report.getDeleted()).isEqualTo(0);
        verify(reportMapper).updateById(report);

        // 未在回收站不允许彻底删除
        assertThatThrownBy(() -> reportService.purgeReportByAdmin(1L, "彻底删除"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void purgeReport_whenDeleted_executesHardDelete() {
        Report report = report(1L, 2L, 10L, 0, 1);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 5L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        reportService.purgeReportByAdmin(1L, "彻底删除");

        verify(reportMapper).deleteById(1L);
    }

    @Test
    void getReportDetail_deleted_throwsNotFound() {
        Report report = report(1L, 2L, 10L, 0, 1);
        when(reportMapper.selectById(1L)).thenReturn(report);

        assertThatThrownBy(() -> reportService.getReportDetail(1L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getReportDetail_success_buildsVo() {
        Report report = report(1L, 2L, 10L, 0, 0);
        report.setHandlerId(3L);
        when(reportMapper.selectById(1L)).thenReturn(report);
        when(postMapper.selectById(10L)).thenReturn(post(10L, 9L, 0));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "reporter"));
        when(userMapper.selectById(3L)).thenReturn(user(3L, "handler"));
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));

        ReportVO vo = reportService.getReportDetail(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getReporter()).isNotNull();
        assertThat(vo.getHandler()).isNotNull();
        assertThat(vo.getPost()).isNotNull();
        assertThat(vo.getPost().getBoard()).isEqualTo(BoardUtil.BOARD_HELP);
    }

    @Test
    void queryReports_withAllScope_returnsPagedVo() {
        when(dataScopeService.resolveScope(1L)).thenReturn(DataScopeService.DataScope.all());
        Report report = report(1L, 2L, 10L, 0, 0);
        report.setHandlerId(3L);
        Page<Report> page = new Page<>(1, 10);
        page.setRecords(List.of(report));
        page.setTotal(1);
        when(reportMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<Report>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Report>>any()
        )).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(2L, "reporter"), user(3L, "handler")));
        when(postMapper.selectBatchIds(any())).thenReturn(List.of(post(10L, 9L, 0)));
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_MARKET)));

        PageResult<ReportVO> result = reportService.queryReports(null, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getPost().getBoard()).isEqualTo(BoardUtil.BOARD_MARKET);
    }

    private Post post(Long id, Long userId, Integer status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setStatus(status);
        post.setBoard(BoardUtil.BOARD_HELP);
        post.setTitle("title-" + id);
        return post;
    }

    private Report report(Long id, Long reporterId, Long postId, Integer status, Integer deleted) {
        Report report = new Report();
        report.setId(id);
        report.setReporterId(reporterId);
        report.setPostId(postId);
        report.setStatus(status);
        report.setDeleted(deleted);
        report.setReason("r");
        return report;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        return user;
    }

    private PostBoard board(Long postId, String board) {
        PostBoard postBoard = new PostBoard();
        postBoard.setPostId(postId);
        postBoard.setBoard(board);
        return postBoard;
    }
}
