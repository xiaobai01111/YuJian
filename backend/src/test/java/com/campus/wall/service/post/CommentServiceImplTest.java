package com.campus.wall.service.post;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.post.CommentBatchDeleteDTO;
import com.campus.wall.dto.post.CommentCreateDTO;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.dto.post.CommentUpdateDTO;
import com.campus.wall.entity.post.Comment;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.CommentMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.AnonymousMappingService;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.post.impl.CommentServiceImpl;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.util.BoardUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostBoardMapper postBoardMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private AnonymousMappingService anonymousMappingService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private OperLogService operLogService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private CommentServiceImpl commentService;

    private StpInterface previousStpInterface;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return Collections.emptyList();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return Collections.emptyList();
            }
        });
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
        SaManager.setStpInterface(previousStpInterface);
    }

    @Test
    void deleteComment_notOwner_throwsForbidden() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setUserId(2L);
        comment.setPostId(3L);
        comment.setStatus(0);

        when(commentMapper.selectById(10L)).thenReturn(comment);

        assertThatThrownBy(() -> commentService.deleteComment(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("无权删除此评论");
    }

    @Test
    void deleteComment_ownerUpdatesStatus() {
        Comment comment = new Comment();
        comment.setId(11L);
        comment.setUserId(1L);
        comment.setPostId(5L);
        comment.setStatus(0);

        when(commentMapper.selectById(11L)).thenReturn(comment);

        commentService.deleteComment(11L);

        verify(commentMapper).updateById(comment);
        verify(postMapper).updateCommentCount(5L, -1);
    }

    @Test
    void deleteComment_alreadyDeleted_noop() {
        Comment comment = new Comment();
        comment.setId(12L);
        comment.setUserId(1L);
        comment.setPostId(6L);
        comment.setStatus(1);

        when(commentMapper.selectById(12L)).thenReturn(comment);

        commentService.deleteComment(12L);

        verify(commentMapper, never()).updateById(any());
        verify(postMapper, never()).updateCommentCount(any(), anyInt());
    }

    @Test
    void deleteCommentByAdmin_requiresReasonWhenNotOwner() {
        Comment comment = new Comment();
        comment.setId(13L);
        comment.setUserId(2L);
        comment.setPostId(7L);
        comment.setStatus(0);

        Post post = new Post();
        post.setId(7L);

        when(commentMapper.selectById(13L)).thenReturn(comment);
        when(postMapper.selectById(7L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> commentService.deleteCommentByAdmin(13L, null))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("请填写操作原因");
    }

    @Test
    void createComment_postMissing_throwsNotFound() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setContent("hello");
        when(postMapper.selectById(10L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.createComment(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createComment_postLocked_throwsForbidden() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setContent("hello");
        Post post = new Post();
        post.setId(10L);
        post.setUserId(2L);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(redisTemplate.hasKey("post:lock:10")).thenReturn(true);

        assertThatThrownBy(() -> commentService.createComment(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void createComment_sensitiveWord_throws() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setContent("bad");
        Post post = new Post();
        post.setId(10L);
        post.setUserId(2L);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(redisTemplate.hasKey("post:lock:10")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("bad")).thenReturn(true);

        assertThatThrownBy(() -> commentService.createComment(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD.getCode());
    }

    @Test
    void createComment_parentInvalid_throwsBadRequest() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setParentId(99L);
        dto.setContent("hello");
        Post post = new Post();
        post.setId(10L);
        post.setUserId(2L);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(redisTemplate.hasKey("post:lock:10")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("hello")).thenReturn(false);
        when(commentMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.createComment(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void createComment_treeHoleNonOwner_assignsAnonymousTag() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setContent("hello");
        Post post = new Post();
        post.setId(10L);
        post.setBoard("treehole");
        post.setUserId(2L);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(redisTemplate.hasKey("post:lock:10")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("hello")).thenReturn(false);
        when(anonymousMappingService.generateAnonymousTag(1L, 10L)).thenReturn("匿名A");
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            return 1;
        }).when(commentMapper).insert(any(Comment.class));

        Long id = commentService.createComment(dto);

        assertThat(id).isEqualTo(100L);
        verify(commentMapper).insert(argThat(c -> "匿名A".equals(c.getAnonymousId())));
        verify(postMapper).updateCommentCount(10L, 1);
    }

    @Test
    void createComment_treeHoleOwner_usesLouZhuTag() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(10L);
        dto.setContent("hello");
        Post post = new Post();
        post.setId(10L);
        post.setBoard("treehole");
        post.setUserId(1L);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(redisTemplate.hasKey("post:lock:10")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("hello")).thenReturn(false);

        commentService.createComment(dto);

        verify(commentMapper).insert(argThat(c -> "楼主".equals(c.getAnonymousId()) && Boolean.TRUE.equals(c.getIsOwner())));
    }

    @Test
    void restoreCommentByAdmin_success_updatesStatusAndCount() {
        Comment comment = new Comment();
        comment.setId(20L);
        comment.setUserId(2L);
        comment.setPostId(7L);
        comment.setStatus(1);
        Post post = new Post();
        post.setId(7L);
        when(commentMapper.selectById(20L)).thenReturn(comment);
        when(postMapper.selectById(7L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        commentService.restoreCommentByAdmin(20L, "恢复");

        assertThat(comment.getStatus()).isEqualTo(0);
        verify(commentMapper).updateById(comment);
        verify(postMapper).updateCommentCount(7L, 1);
    }

    @Test
    void purgeCommentByAdmin_notDeleted_throws() {
        Comment comment = new Comment();
        comment.setId(21L);
        comment.setUserId(2L);
        comment.setPostId(7L);
        comment.setStatus(0);
        when(commentMapper.selectById(21L)).thenReturn(comment);

        assertThatThrownBy(() -> commentService.purgeCommentByAdmin(21L, "x"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void queryCommentsForConsole_allScope_returnsAuthorInfo() {
        when(dataScopeService.resolveScope(1L)).thenReturn(DataScopeService.DataScope.all());

        Comment comment = new Comment();
        comment.setId(30L);
        comment.setPostId(10L);
        comment.setUserId(2L);
        comment.setContent("hello");
        comment.setStatus(0);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(comment));
        page.setTotal(1);
        when(commentMapper.selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Comment>>any()
        )).thenReturn(page);

        User user = new User();
        user.setId(2L);
        user.setUsername("u2");
        user.setNickname("n2");
        when(userMapper.selectBatchIds(List.of(2L))).thenReturn(List.of(user));

        CommentQueryDTO query = new CommentQueryDTO();
        query.setPage(1);
        query.setSize(10);

        var result = commentService.queryCommentsForConsole(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getAuthor().getUsername()).isEqualTo("u2");
    }

    @Test
    void getPostComments_postMissing_throwsNotFound() {
        when(postMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.getPostComments(99L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getPostComments_buildsTreeAndLoadsAuthor() {
        Post post = new Post();
        post.setId(40L);
        post.setBoard(BoardUtil.BOARD_HELP);
        when(postMapper.selectById(40L)).thenReturn(post);
        when(postBoardMapper.selectCount(any())).thenReturn(0L);

        Comment root = new Comment();
        root.setId(401L);
        root.setPostId(40L);
        root.setUserId(2L);
        root.setStatus(0);
        root.setParentId(0L);
        root.setContent("root");
        Comment child = new Comment();
        child.setId(402L);
        child.setPostId(40L);
        child.setUserId(3L);
        child.setStatus(0);
        child.setParentId(401L);
        child.setContent("child");
        when(commentMapper.selectList(any())).thenReturn(List.of(root, child));

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("u2");
        u2.setNickname("n2");
        User u3 = new User();
        u3.setId(3L);
        u3.setUsername("u3");
        u3.setNickname("n3");
        when(userMapper.selectBatchIds(List.of(2L, 3L))).thenReturn(List.of(u2, u3));

        var result = commentService.getPostComments(40L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(401L);
        assertThat(result.getFirst().getAuthor()).isNotNull();
        assertThat(result.getFirst().getChildren()).hasSize(1);
        assertThat(result.getFirst().getChildren().getFirst().getId()).isEqualTo(402L);
    }

    @Test
    void getPostCommentsPage_anonymousPost_hidesAuthors() {
        Post post = new Post();
        post.setId(41L);
        post.setBoard(BoardUtil.BOARD_TREE_HOLE);
        when(postMapper.selectById(41L)).thenReturn(post);

        Comment root = new Comment();
        root.setId(411L);
        root.setPostId(41L);
        root.setUserId(2L);
        root.setStatus(0);
        root.setParentId(0L);
        root.setContent("root");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(root));
        page.setTotal(1L);
        when(commentMapper.selectPage(
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Comment>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Comment>>any()
        )).thenReturn(page);

        Comment child = new Comment();
        child.setId(412L);
        child.setPostId(41L);
        child.setUserId(3L);
        child.setStatus(0);
        child.setParentId(411L);
        child.setContent("child");
        when(commentMapper.selectList(any())).thenReturn(List.of(child));

        User u2 = new User();
        u2.setId(2L);
        User u3 = new User();
        u3.setId(3L);
        when(userMapper.selectBatchIds(List.of(2L, 3L))).thenReturn(List.of(u2, u3));

        var result = commentService.getPostCommentsPage(41L, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getAuthor()).isNull();
        assertThat(result.getRecords().getFirst().getChildren()).hasSize(1);
        assertThat(result.getRecords().getFirst().getChildren().getFirst().getAuthor()).isNull();
    }

    @Test
    void updateCommentByAdmin_deletedComment_throws() {
        Comment comment = new Comment();
        comment.setId(501L);
        comment.setStatus(1);
        when(commentMapper.selectById(501L)).thenReturn(comment);

        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("updated");

        assertThatThrownBy(() -> commentService.updateCommentByAdmin(501L, dto, "x"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void updateCommentByAdmin_blankContent_throws() {
        Comment comment = new Comment();
        comment.setId(502L);
        comment.setPostId(52L);
        comment.setUserId(1L);
        comment.setStatus(0);
        when(commentMapper.selectById(502L)).thenReturn(comment);
        Post post = new Post();
        post.setId(52L);
        when(postMapper.selectById(52L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 1L)).thenReturn(true);

        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("   ");

        assertThatThrownBy(() -> commentService.updateCommentByAdmin(502L, dto, null))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void updateCommentByAdmin_sensitiveContent_throws() {
        Comment comment = new Comment();
        comment.setId(503L);
        comment.setPostId(53L);
        comment.setUserId(1L);
        comment.setStatus(0);
        when(commentMapper.selectById(503L)).thenReturn(comment);
        Post post = new Post();
        post.setId(53L);
        when(postMapper.selectById(53L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 1L)).thenReturn(true);
        when(sensitiveWordService.containsSensitiveWord("bad")).thenReturn(true);

        CommentUpdateDTO dto = new CommentUpdateDTO();
        dto.setContent("bad");

        assertThatThrownBy(() -> commentService.updateCommentByAdmin(503L, dto, null))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.CONTENT_CONTAINS_SENSITIVE_WORD.getCode());
    }

    @Test
    void deleteCommentsByAdmin_missingReason_throws() {
        CommentBatchDeleteDTO dto = new CommentBatchDeleteDTO();
        dto.setIds(List.of(601L));

        Comment comment = new Comment();
        comment.setId(601L);
        comment.setPostId(61L);
        comment.setUserId(2L);
        comment.setStatus(0);
        when(commentMapper.selectBatchIds(List.of(601L))).thenReturn(List.of(comment));
        Post post = new Post();
        post.setId(61L);
        when(postMapper.selectBatchIds(List.of(61L))).thenReturn(List.of(post));

        assertThatThrownBy(() -> commentService.deleteCommentsByAdmin(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void deleteCommentsByAdmin_updateWrapperFailure_throwsPartialFailure() {
        CommentBatchDeleteDTO dto = new CommentBatchDeleteDTO();
        dto.setIds(List.of(701L, 702L));
        dto.setReason("批量删除");

        Comment c1 = new Comment();
        c1.setId(701L);
        c1.setPostId(71L);
        c1.setUserId(2L);
        c1.setStatus(0);
        Comment c2 = new Comment();
        c2.setId(702L);
        c2.setPostId(71L);
        c2.setUserId(3L);
        c2.setStatus(0);
        when(commentMapper.selectBatchIds(List.of(701L, 702L))).thenReturn(List.of(c1, c2));
        Post post = new Post();
        post.setId(71L);
        when(postMapper.selectBatchIds(List.of(71L))).thenReturn(List.of(post));
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        when(dataScopeService.canAccessUser(1L, 3L)).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        org.mockito.Mockito.lenient().doThrow(new RuntimeException("mock fail")).when(commentMapper).update(
            isNull(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Comment>>any()
        );

        assertThatThrownBy(() -> commentService.deleteCommentsByAdmin(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("部分评论删除失败：2 条");
    }
}
