package com.campus.wall.service.system;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.dto.system.NoticeDTO;
import com.campus.wall.common.ResultCode;
import com.campus.wall.entity.system.SysNotice;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysNoticeMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.system.impl.NoticeServiceImpl;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private SysNoticeMapper noticeMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OperLogService operLogService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(5L);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void publish_alreadyPublished_throws() {
        SysNotice notice = new SysNotice();
        notice.setId(1L);
        notice.setStatus(1);

        when(noticeMapper.selectById(1L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.publish(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("公告已发布");
    }

    @Test
    void offline_notPublished_throws() {
        SysNotice notice = new SysNotice();
        notice.setId(2L);
        notice.setStatus(0);

        when(noticeMapper.selectById(2L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.offline(2L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("只能下线已发布的公告");
    }

    @Test
    void getVisibleNotices_incompleteCursor_throws() {
        User user = new User();
        user.setId(5L);
        user.setDeptId(1L);
        when(userMapper.selectById(5L)).thenReturn(user);

        assertThatThrownBy(() -> noticeService.getVisibleNotices(10, 1, null, null))
            .isInstanceOf(BusinessException.class)
            .hasMessage("游标参数不完整");
    }

    @Test
    void publish_missingNotice_throws() {
        when(noticeMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.publish(9L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode())
            .hasMessage("公告不存在");
    }

    @Test
    void getPublicNoticeDetail_notPublished_throwsNotFound() {
        SysNotice notice = new SysNotice();
        notice.setId(11L);
        notice.setStatus(0);
        when(noticeMapper.selectById(11L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.getPublicNoticeDetail(11L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getPublicNoticeDetail_nonPublicScope_throwsNotFound() {
        SysNotice notice = new SysNotice();
        notice.setId(12L);
        notice.setStatus(1);
        notice.setScopeType("DEPT");
        when(noticeMapper.selectById(12L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.getPublicNoticeDetail(12L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getVisibleNoticeDetail_outOfPeriod_throwsNotFound() {
        SysNotice notice = new SysNotice();
        notice.setId(13L);
        notice.setStatus(1);
        notice.setScopeType("ALL");
        notice.setStartAt(LocalDateTime.now().plusDays(1));
        when(noticeMapper.selectById(13L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.getVisibleNoticeDetail(13L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("公告已过期");
    }

    @Test
    void getVisibleNoticeDetail_forbiddenByScope_throwsForbidden() {
        SysNotice notice = new SysNotice();
        notice.setId(14L);
        notice.setStatus(1);
        notice.setScopeType("USERS");
        notice.setScopeIds(List.of(99L));
        when(noticeMapper.selectById(14L)).thenReturn(notice);

        assertThatThrownBy(() -> noticeService.getVisibleNoticeDetail(14L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void getVisibleNotices_badCursorTime_throws() {
        User user = new User();
        user.setId(5L);
        user.setDeptId(1L);
        when(userMapper.selectById(5L)).thenReturn(user);

        assertThatThrownBy(() -> noticeService.getVisibleNotices(10, 1, "bad-time", 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("lastPublishedAt 格式错误");
    }

    @Test
    void create_invalidTitle_throws() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle(" ");
        dto.setContent("ok");

        assertThatThrownBy(() -> noticeService.create(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("标题不能为空");
    }

    @Test
    void create_scopeTypeInvalid_throws() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("t");
        dto.setContent("ok");
        dto.setScopeType("INVALID");

        assertThatThrownBy(() -> noticeService.create(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("可见范围类型无效");
    }

    @Test
    void create_deptScopeWithoutIds_throws() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("t");
        dto.setContent("ok");
        dto.setScopeType("DEPT");
        dto.setScopeIds(List.of());

        assertThatThrownBy(() -> noticeService.create(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("部门/用户可见范围必须指定ID列表");
    }

    @Test
    void create_success_sanitizesContentAndDraftStatus() {
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("校园通知");
        dto.setContent("<script>x</script><b>ok</b>");
        dto.setScopeType("ALL");
        dto.setIsPinned(true);
        dto.setScopeIds(java.util.Arrays.asList(1L, 1L, null));

        var vo = noticeService.create(dto);

        verify(noticeMapper).insert(any(SysNotice.class));
        assertThat(vo.getStatus()).isEqualTo(0);
        assertThat(vo.getContent()).doesNotContain("<script>");
    }

    @Test
    void update_missingNotice_throws() {
        when(noticeMapper.selectById(100L)).thenReturn(null);
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle("t");
        dto.setContent("c");

        assertThatThrownBy(() -> noticeService.update(100L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void publish_success_updatesStatusAndPublishedTime() {
        SysNotice notice = new SysNotice();
        notice.setId(15L);
        notice.setStatus(0);
        when(noticeMapper.selectById(15L)).thenReturn(notice);

        noticeService.publish(15L);

        assertThat(notice.getStatus()).isEqualTo(1);
        assertThat(notice.getPublishedAt()).isNotNull();
        verify(noticeMapper).updateById(notice);
    }

    @Test
    void offline_success_updatesStatus() {
        SysNotice notice = new SysNotice();
        notice.setId(16L);
        notice.setStatus(1);
        when(noticeMapper.selectById(16L)).thenReturn(notice);

        noticeService.offline(16L);

        assertThat(notice.getStatus()).isEqualTo(2);
        verify(noticeMapper).updateById(notice);
    }

    @Test
    void delete_missingNotice_throwsNotFound() {
        when(noticeMapper.selectById(17L)).thenReturn(null);

        assertThatThrownBy(() -> noticeService.delete(17L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getPublicNotices_usesCacheOnSecondCall() {
        SysNotice notice = new SysNotice();
        notice.setId(18L);
        notice.setTitle("N");
        notice.setStatus(1);
        when(noticeMapper.selectPublicNotices(any(Integer.class), any(LocalDateTime.class)))
            .thenReturn(List.of(notice));

        var first = noticeService.getPublicNotices(10);
        var second = noticeService.getPublicNotices(10);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(noticeMapper).selectPublicNotices(any(Integer.class), any(LocalDateTime.class));
    }
}
