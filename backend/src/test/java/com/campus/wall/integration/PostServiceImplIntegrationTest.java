package com.campus.wall.integration;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import com.campus.wall.dto.post.PostQueryDTO;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.post.PostService;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.support.IntegrationTestBase;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.vo.post.PostVO;
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
class PostServiceImplIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PostService postService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private DataScopeService dataScopeService;

    private Long userId;
    private Long otherUserId;
    private SaTokenContext previousContext;

    @BeforeEach
    void setUp() {
        previousContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        postMapper.delete(null);
        userId = ensureUser("post_user_1");
        otherUserId = ensureUser("post_user_2");
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
        SaManager.setSaTokenContext(previousContext);
    }

    @Test
    void queryPosts_ordersByLatest() {
        when(dataScopeService.resolveScope(1L)).thenReturn(DataScopeService.DataScope.all());

        Post older = buildPost(userId, "HOME", "older", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        Post newer = buildPost(userId, "HOME", "newer", LocalDateTime.now(), LocalDateTime.now());
        postMapper.insert(older);
        postMapper.insert(newer);

        PostQueryDTO query = new PostQueryDTO();
        query.setPage(1);
        query.setSize(10);
        query.setOrderBy("latest");

        List<PostVO> records = postService.queryPosts(query).getRecords();
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getTitle()).isEqualTo("newer");
        assertThat(records.get(1).getTitle()).isEqualTo("older");
    }

    @Test
    void queryPosts_ordersByHot() {
        when(dataScopeService.resolveScope(1L)).thenReturn(DataScopeService.DataScope.all());

        Post low = buildPost(userId, "HOME", "low", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2));
        Post high = buildPost(userId, "HOME", "high", LocalDateTime.now().minusDays(1), LocalDateTime.now());
        postMapper.insert(low);
        postMapper.insert(high);

        PostQueryDTO query = new PostQueryDTO();
        query.setPage(1);
        query.setSize(10);
        query.setOrderBy("hot");

        List<PostVO> records = postService.queryPosts(query).getRecords();
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getTitle()).isEqualTo("high");
        assertThat(records.get(1).getTitle()).isEqualTo("low");
    }

    @Test
    void queryPostsForConsole_appliesSelfScope() {
        Long selfUserId = 1L;
        Post mine = buildPost(selfUserId, "HOME", "mine", LocalDateTime.now(), LocalDateTime.now());
        Post others = buildPost(otherUserId, "HOME", "others", LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1));
        postMapper.insert(mine);
        postMapper.insert(others);

        StpUtil.logout();
        StpUtil.login(selfUserId);
        when(dataScopeService.resolveScope(selfUserId)).thenReturn(DataScopeService.DataScope.selfOnly());
        when(dataScopeService.buildUserScopeExistsSql(any(), anyString())).thenReturn(null);

        PostQueryDTO query = new PostQueryDTO();
        query.setPage(1);
        query.setSize(10);

        List<PostVO> records = postService.queryPostsForConsole(query).getRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getTitle()).isEqualTo("mine");
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

    private Post buildPost(Long userId, String board, String title, LocalDateTime createdAt, LocalDateTime lastInteractionAt) {
        Post post = new Post();
        post.setUserId(userId);
        post.setBoard(board);
        post.setTitle(title);
        post.setContent("content");
        post.setStatus(0);
        post.setShowOnHome(true);
        post.setCreatedAt(createdAt);
        post.setUpdatedAt(createdAt);
        post.setLastInteractionAt(lastInteractionAt);
        return post;
    }
}
