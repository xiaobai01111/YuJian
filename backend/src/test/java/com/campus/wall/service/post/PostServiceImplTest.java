package com.campus.wall.service.post;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.post.PostCreateDTO;
import com.campus.wall.dto.post.PostUpdateDTO;
import com.campus.wall.entity.post.Bookmark;
import com.campus.wall.entity.post.Like;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.post.BookmarkMapper;
import com.campus.wall.mapper.post.LikeMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.content.SensitiveWordService;
import com.campus.wall.service.file.FileService;
import com.campus.wall.service.post.impl.PostServiceImpl;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.security.RateLimitService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.support.SaTokenTestContext;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.vo.file.FileVO;
import com.campus.wall.vo.post.PostVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;
    @Mock
    private LikeMapper likeMapper;
    @Mock
    private BookmarkMapper bookmarkMapper;
    @Mock
    private PostBoardMapper postBoardMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private FileService fileService;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private com.campus.wall.service.user.CreditService creditService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private OperLogService operLogService;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private PostServiceImpl postService;

    private StpInterface previousStpInterface;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return List.of();
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
    void likePost_existingLike_noop() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(likeMapper.selectOne(any())).thenReturn(new Like());

        postService.likePost(10L);

        verify(likeMapper, never()).insert(any(Like.class));
        verify(postMapper, never()).updateLikeCount(anyLong(), anyInt());
    }

    @Test
    void likePost_newLike_insertsAndUpdatesCounters() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(likeMapper.selectOne(any())).thenReturn(null);

        postService.likePost(10L);

        verify(likeMapper).insert(any(Like.class));
        verify(postMapper).updateLikeCount(10L, 1);
        verify(postMapper).updateLastInteractionAt(10L);
    }

    @Test
    void unlikePost_deletedPositive_updatesCount() {
        when(likeMapper.delete(any())).thenReturn(1);

        postService.unlikePost(10L);

        verify(postMapper).updateLikeCount(10L, -1);
    }

    @Test
    void unlikePost_deletedZero_noop() {
        when(likeMapper.delete(any())).thenReturn(0);

        postService.unlikePost(10L);

        verify(postMapper, never()).updateLikeCount(anyLong(), anyInt());
    }

    @Test
    void bookmarkPost_existing_noop() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(bookmarkMapper.selectOne(any())).thenReturn(new Bookmark());

        postService.bookmarkPost(10L);

        verify(bookmarkMapper, never()).insert(any(Bookmark.class));
    }

    @Test
    void bookmarkPost_newBookmark_inserts() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(bookmarkMapper.selectOne(any())).thenReturn(null);

        postService.bookmarkPost(10L);

        verify(bookmarkMapper).insert(any(Bookmark.class));
    }

    @Test
    void bookmarkPosts_emptyInput_returnsZeroedResult() {
        var result = postService.bookmarkPosts(List.of());

        assertThat(result.getRequested()).isEqualTo(0);
        assertThat(result.getSuccess()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
    }

    @Test
    void bookmarkPosts_skipDeletedAndExisting() {
        Post p10 = post(10L, 2L, 0);
        Post p11 = post(11L, 2L, 2);
        when(postMapper.selectBatchIds(List.of(10L, 11L, 12L))).thenReturn(List.of(p10, p11));

        Bookmark existing = new Bookmark();
        existing.setPostId(10L);
        when(bookmarkMapper.selectList(any())).thenReturn(List.of(existing));

        var result = postService.bookmarkPosts(List.of(10L, 11L, 12L));

        assertThat(result.getRequested()).isEqualTo(3);
        assertThat(result.getSuccess()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(3);
    }

    @Test
    void recordPostView_firstView_returnsTrueAndIncrements() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> ops = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(ops);
        when(ops.add("post:view:10", "1")).thenReturn(1L);

        boolean added = postService.recordPostView(10L);

        assertThat(added).isTrue();
        verify(postMapper).incrementViewCount(10L);
    }

    @Test
    void recordPostView_repeatView_returnsFalse() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> ops = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(ops);
        when(ops.add("post:view:10", "1")).thenReturn(0L);

        boolean added = postService.recordPostView(10L);

        assertThat(added).isFalse();
        verify(postMapper, never()).incrementViewCount(any());
    }

    @Test
    void recordPostView_deletedPost_throws() {
        Post post = post(10L, 2L, 2);
        when(postMapper.selectById(10L)).thenReturn(post);

        assertThatThrownBy(() -> postService.recordPostView(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getPostDetail_deletedPost_throwsNotFound() {
        Post post = post(10L, 2L, 2);
        when(postMapper.selectById(10L)).thenReturn(post);

        assertThatThrownBy(() -> postService.getPostDetail(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void getPostDetail_withFilesAndAuthor_returnsComposedVo() {
        Post post = post(10L, 2L, 0);
        post.setTitle("title");
        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        User author = new User();
        author.setId(2L);
        author.setUsername("u2");
        author.setNickname("Nick");
        author.setVerifyStatus(2);
        when(userMapper.selectById(2L)).thenReturn(author);
        when(sysRoleMapper.selectRoleKeysByUserId(2L)).thenReturn(List.of());
        FileVO file = new FileVO();
        file.setId(99L);
        when(fileService.getFilesByTarget(10L, "post")).thenReturn(List.of(file));

        PostVO vo = postService.getPostDetail(10L);

        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getBoard()).isEqualTo(BoardUtil.BOARD_HELP);
        assertThat(vo.getAuthor()).isNotNull();
        assertThat(vo.getFiles()).hasSize(1);
    }

    @Test
    void markAsResolved_notOwner_throwsForbidden() {
        Post post = post(10L, 2L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);

        assertThatThrownBy(() -> postService.markAsResolved(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void markAsResolved_wrongBoard_throws() {
        Post post = post(10L, 1L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_MARKET)));

        assertThatThrownBy(() -> postService.markAsResolved(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void markAsResolved_success_updatesStatus() {
        Post post = post(10L, 1L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));

        postService.markAsResolved(10L);

        assertThat(post.getStatus()).isEqualTo(1);
        verify(postMapper).updateById(post);
    }

    @Test
    void markAsSold_nonMarket_throws() {
        Post post = post(10L, 1L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));

        assertThatThrownBy(() -> postService.markAsSold(10L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void markAsSold_marketPost_updatesStatus() {
        Post post = post(10L, 1L, 0);
        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_MARKET)));

        postService.markAsSold(10L);

        assertThat(post.getStatus()).isEqualTo(5);
        verify(postMapper).updateById(post);
    }

    @Test
    void getUserBookmarks_filtersDeletedPostAndKeepsFlags() {
        Page<Bookmark> bookmarkPage = new Page<>(1, 10);
        Bookmark b1 = new Bookmark();
        b1.setPostId(10L);
        Bookmark b2 = new Bookmark();
        b2.setPostId(11L);
        bookmarkPage.setRecords(List.of(b1, b2));
        bookmarkPage.setTotal(2);
        when(bookmarkMapper.selectPage(
            org.mockito.ArgumentMatchers.<Page<Bookmark>>any(),
            org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.conditions.Wrapper<Bookmark>>any()
        )).thenReturn(bookmarkPage);

        Post p10 = post(10L, 2L, 0);
        p10.setTitle("alive");
        Post p11 = post(11L, 2L, 2);
        p11.setTitle("deleted");
        when(postMapper.selectBatchIds(List.of(10L, 11L))).thenReturn(List.of(p10, p11));
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));

        User author = new User();
        author.setId(2L);
        author.setUsername("u2");
        author.setNickname("n2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));
        when(sysRoleMapper.selectUserIdsByRoleKey(anyString(), any())).thenReturn(List.of());

        Like like = new Like();
        like.setPostId(10L);
        when(likeMapper.selectList(any())).thenReturn(List.of(like));
        Bookmark existing = new Bookmark();
        existing.setPostId(10L);
        when(bookmarkMapper.selectList(any())).thenReturn(List.of(existing));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        var result = postService.getUserBookmarks(1L, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        PostVO vo = result.getRecords().getFirst();
        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getIsLiked()).isTrue();
        assertThat(vo.getIsBookmarked()).isTrue();
    }

    @Test
    void searchPosts_blankKeyword_returnsEmpty() {
        var result = postService.searchPosts("   ", 1, 10);
        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void searchPosts_withData_buildsVoList() {
        Page<Post> page = new Page<>(1, 10);
        Post p = post(10L, 2L, 0);
        p.setTitle("match");
        page.setRecords(List.of(p));
        page.setTotal(1);
        when(postMapper.fullTextSearch(org.mockito.ArgumentMatchers.<Page<Post>>any(), anyString())).thenReturn(page);
        when(postBoardMapper.selectList(any())).thenReturn(List.of(board(10L, BoardUtil.BOARD_HELP)));
        User author = new User();
        author.setId(2L);
        author.setUsername("u2");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));
        when(sysRoleMapper.selectUserIdsByRoleKey(anyString(), any())).thenReturn(List.of());
        when(likeMapper.selectList(any())).thenReturn(List.of());
        when(bookmarkMapper.selectList(any())).thenReturn(List.of());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        var result = postService.searchPosts("match", 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getTitle()).isEqualTo("match");
    }

    @Test
    void createPost_userMissing_throwsNotFound() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setBoards(List.of(BoardUtil.BOARD_HELP));
        dto.setTitle("title");
        dto.setContent("content");
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> postService.createPost(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createPost_userNotVerified_throwsForbidden() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setBoards(List.of(BoardUtil.BOARD_HELP));
        dto.setTitle("title");
        dto.setContent("content");
        User current = new User();
        current.setId(1L);
        current.setVerifyStatus(1);
        when(userMapper.selectById(1L)).thenReturn(current);

        assertThatThrownBy(() -> postService.createPost(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void createPost_marketCreditDenied_throwsForbidden() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setBoards(List.of(BoardUtil.BOARD_MARKET));
        dto.setTitle("title");
        dto.setContent("content");
        User current = new User();
        current.setId(1L);
        current.setVerifyStatus(2);
        when(userMapper.selectById(1L)).thenReturn(current);
        when(creditService.canPostInMarket(1L)).thenReturn(false);

        assertThatThrownBy(() -> postService.createPost(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void createPost_treeHoleForcesAnonymousAndBindsFiles() {
        PostCreateDTO dto = new PostCreateDTO();
        dto.setBoards(List.of(BoardUtil.BOARD_TREE_HOLE));
        dto.setTitle("title");
        dto.setContent("content");
        dto.setIsAnonymous(false);
        dto.setFileIds(List.of(9L));
        User current = new User();
        current.setId(1L);
        current.setVerifyStatus(2);
        when(userMapper.selectById(1L)).thenReturn(current);
        when(sensitiveWordService.containsSensitiveWord("title")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("content")).thenReturn(false);
        doAnswer(invocation -> {
            Post inserted = invocation.getArgument(0);
            inserted.setId(101L);
            return 1;
        }).when(postMapper).insert(any(Post.class));

        Long postId = postService.createPost(dto);

        assertThat(postId).isEqualTo(101L);
        verify(postMapper).insert(argThat(post ->
            Boolean.TRUE.equals(post.getIsAnonymous()) && BoardUtil.BOARD_TREE_HOLE.equals(post.getBoard())
        ));
        verify(postBoardMapper).insert(any(PostBoard.class));
        verify(fileService).bindFiles(List.of(9L), 101L, "post");
    }

    @Test
    void createPostByAdmin_success() {
        becomeSuperAdmin();
        PostCreateDTO dto = new PostCreateDTO();
        dto.setBoards(List.of(BoardUtil.BOARD_HELP));
        dto.setTitle("admin-title");
        dto.setContent("admin-content");
        when(sensitiveWordService.containsSensitiveWord("admin-title")).thenReturn(false);
        when(sensitiveWordService.containsSensitiveWord("admin-content")).thenReturn(false);
        doAnswer(invocation -> {
            Post inserted = invocation.getArgument(0);
            inserted.setId(102L);
            return 1;
        }).when(postMapper).insert(any(Post.class));

        Long id = postService.createPostByAdmin(dto);

        assertThat(id).isEqualTo(102L);
        verify(postMapper).insert(any(Post.class));
    }

    @Test
    void updatePost_notOwner_throwsForbidden() {
        Post post = post(20L, 2L, 0);
        when(postMapper.selectById(20L)).thenReturn(post);

        assertThatThrownBy(() -> postService.updatePost(20L, new PostUpdateDTO()))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void updatePost_invalidBoards_throwsBadRequest() {
        Post post = post(21L, 1L, 0);
        when(postMapper.selectById(21L)).thenReturn(post);
        PostUpdateDTO dto = new PostUpdateDTO();
        dto.setBoards(List.of("invalid-board"));

        assertThatThrownBy(() -> postService.updatePost(21L, dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void deletePost_notOwnerAndNotAdmin_throwsForbidden() {
        Post post = post(22L, 2L, 0);
        when(postMapper.selectById(22L)).thenReturn(post);

        assertThatThrownBy(() -> postService.deletePost(22L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void restorePostByAdmin_deletedPost_restoresStatus() {
        becomeSuperAdmin();
        Post post = post(30L, 2L, 2);
        when(postMapper.selectById(30L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        postService.restorePostByAdmin(30L, "恢复");

        assertThat(post.getStatus()).isEqualTo(0);
        verify(postMapper).updateById(post);
    }

    @Test
    void purgePostByAdmin_deletedPost_deletesAndClearsLock() {
        becomeSuperAdmin();
        Post post = post(31L, 2L, 2);
        when(postMapper.selectById(31L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        postService.purgePostByAdmin(31L, "彻底删除");

        verify(postMapper).deleteById(31L);
        verify(redisTemplate).delete("post:lock:31");
    }

    @Test
    void offlinePostByAdmin_archived_noop() {
        becomeSuperAdmin();
        Post post = post(32L, 2L, 4);
        when(postMapper.selectById(32L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        postService.offlinePostByAdmin(32L, "下架");

        verify(postMapper, never()).updateById(any(Post.class));
    }

    @Test
    void onlinePostByAdmin_notArchived_throwsBadRequest() {
        becomeSuperAdmin();
        Post post = post(33L, 2L, 0);
        when(postMapper.selectById(33L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> postService.onlinePostByAdmin(33L, "上架"))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode());
    }

    @Test
    void lockPostByAdmin_success_setsRedisKey() {
        becomeSuperAdmin();
        Post post = post(34L, 2L, 0);
        when(postMapper.selectById(34L)).thenReturn(post);
        when(dataScopeService.canAccessUser(1L, 2L)).thenReturn(true);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        postService.lockPostByAdmin(34L, "锁定");

        verify(valueOps).set("post:lock:34", "1");
    }

    @Test
    void unbookmarkPost_deletesBookmarkRecord() {
        postService.unbookmarkPost(35L);
        verify(bookmarkMapper).delete(any());
    }

    private void becomeSuperAdmin() {
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return List.of(SecurityUtil.getSuperAdminRoleKey());
            }
        });
    }

    private Post post(Long id, Long userId, Integer status) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setStatus(status);
        post.setBoard(BoardUtil.BOARD_HELP);
        post.setIsAnonymous(false);
        return post;
    }

    private PostBoard board(Long postId, String board) {
        PostBoard postBoard = new PostBoard();
        postBoard.setPostId(postId);
        postBoard.setBoard(board);
        return postBoard;
    }
}
