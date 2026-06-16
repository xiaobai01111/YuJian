package com.campus.wall.integration;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import com.campus.wall.dto.post.CommentQueryDTO;
import com.campus.wall.entity.post.Comment;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.CommentMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.post.CommentService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.support.IntegrationTestBase;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.vo.post.CommentConsoleVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Transactional
@SuppressWarnings("removal")
class CommentServiceImplIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private DataScopeService dataScopeService;

    private Long userId;
    private Long otherUserId;
    private Long postId;
    private SaTokenContext previousContext;

    @BeforeEach
    void setUp() {
        previousContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        commentMapper.delete(null);
        postMapper.delete(null);
        userId = ensureUser("comment_user_1");
        otherUserId = ensureUser("comment_user_2");
        postId = ensurePost(userId);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
        SaManager.setSaTokenContext(previousContext);
    }

    @Test
    void queryComments_ordersByCreatedAtDesc() {
        when(dataScopeService.resolveScope(1L)).thenReturn(DataScopeService.DataScope.all());

        Comment older = buildComment(postId, userId, "older", LocalDateTime.now().minusDays(1));
        Comment newer = buildComment(postId, userId, "newer", LocalDateTime.now());
        commentMapper.insert(older);
        commentMapper.insert(newer);

        CommentQueryDTO query = new CommentQueryDTO();
        query.setPage(1);
        query.setSize(10);

        List<CommentConsoleVO> records = commentService.queryCommentsForConsole(query).getRecords();
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getContent()).isEqualTo("newer");
        assertThat(records.get(1).getContent()).isEqualTo("older");
    }

    @Test
    void queryComments_appliesSelfScope() {
        Comment mine = buildComment(postId, userId, "mine", LocalDateTime.now());
        Comment others = buildComment(postId, otherUserId, "others", LocalDateTime.now().minusHours(1));
        commentMapper.insert(mine);
        commentMapper.insert(others);

        StpUtil.logout();
        StpUtil.login(userId);
        when(dataScopeService.resolveScope(userId)).thenReturn(DataScopeService.DataScope.selfOnly());
        when(dataScopeService.buildUserScopeExistsSql(any(), anyString())).thenReturn(null);

        CommentQueryDTO query = new CommentQueryDTO();
        query.setPage(1);
        query.setSize(10);

        List<CommentConsoleVO> records = commentService.queryCommentsForConsole(query).getRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getAuthor().getId()).isEqualTo(userId);
    }

    private Long ensureUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("pass");
        user.setNickname(username);
        user.setDeptId(1L);
        user.setStatus(0);
        user.setUserType(0);
        user.setVerifyStatus(0);
        user.setCreditScore(100);
        userMapper.insert(user);
        return user.getId();
    }

    private Long ensurePost(Long userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setBoard("HOME");
        post.setTitle("post");
        post.setContent("content");
        post.setStatus(0);
        post.setShowOnHome(true);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
        return post.getId();
    }

    private Comment buildComment(Long postId, Long userId, String content, LocalDateTime createdAt) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setStatus(0);
        comment.setCreatedAt(createdAt);
        return comment;
    }
}
