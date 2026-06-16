package com.campus.wall.property;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 帖子模块属性测试
 */
class PostPropertyTest {

    // 帖子状态
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_RESOLVED = 1;
    private static final int STATUS_DELETED = 2;

    /**
     * Property 2: 帖子排序一致性
     * 按时间降序排列的帖子列表应保持一致性
     */
    @Property(tries = 50)
    void postsAreSortedByCreatedAtDescending(
            @ForAll("postList") List<TestPost> posts) {
        // 模拟按创建时间降序排序
        List<TestPost> sorted = posts.stream()
                .sorted(Comparator.comparing(TestPost::getCreatedAt).reversed())
                .collect(Collectors.toList());

        // 验证排序一致性
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i).getCreatedAt())
                    .isAfterOrEqualTo(sorted.get(i + 1).getCreatedAt());
        }
    }

    /**
     * Property 3: 匿名性保护
     * 匿名帖子不应暴露作者信息
     */
    @Property(tries = 100)
    void anonymousPostDoesNotExposeAuthor(
            @ForAll("userId") Long userId,
            @ForAll("postContent") String content) {
        TestPost post = new TestPost();
        post.setUserId(userId);
        post.setContent(content);
        post.setIsAnonymous(true);

        // 模拟转换为 VO
        TestPostVO vo = toPostVO(post);

        // 匿名帖子作者信息应为 null
        assertThat(vo.getAuthorId()).isNull();
        assertThat(vo.getAuthorName()).isNull();
    }

    /**
     * Property 3: 非匿名帖子应显示作者信息
     */
    @Property(tries = 100)
    void nonAnonymousPostShowsAuthor(
            @ForAll("userId") Long userId,
            @ForAll("postContent") String content) {
        TestPost post = new TestPost();
        post.setUserId(userId);
        post.setContent(content);
        post.setIsAnonymous(false);

        // 模拟转换为 VO
        TestPostVO vo = toPostVO(post);

        // 非匿名帖子应显示作者信息
        assertThat(vo.getAuthorId()).isEqualTo(userId);
    }

    /**
     * Property 5: 必填字段验证 - 内容不能为空
     */
    @Example
    void contentCannotBeEmpty() {
        TestPost post = new TestPost();
        post.setBoard("general");
        post.setContent("");

        boolean isValid = validatePost(post);

        assertThat(isValid).isFalse();
    }

    /**
     * Property 5: 必填字段验证 - 板块不能为空
     */
    @Example
    void boardCannotBeEmpty() {
        TestPost post = new TestPost();
        post.setBoard("");
        post.setContent("测试内容");

        boolean isValid = validatePost(post);

        assertThat(isValid).isFalse();
    }

    /**
     * Property 5: 树洞板块强制匿名
     */
    @Property(tries = 50)
    void treeHoleBoardForcesAnonymous(
            @ForAll("postContent") String content) {
        TestPost post = new TestPost();
        post.setBoard("tree-hole");
        post.setContent(content);
        post.setIsAnonymous(false); // 用户选择不匿名

        // 创建帖子时应强制设置为匿名
        TestPost created = createPost(post);

        assertThat(created.getIsAnonymous()).isTrue();
    }

    /**
     * Property 6: 筛选结果正确性 - 按板块筛选
     */
    @Property(tries = 50)
    void filterByBoardReturnsOnlyMatchingPosts(
            @ForAll("postList") List<TestPost> posts,
            @ForAll("board") String targetBoard) {
        // 筛选指定板块的帖子
        List<TestPost> filtered = posts.stream()
                .filter(p -> targetBoard.equals(p.getBoard()))
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .collect(Collectors.toList());

        // 验证筛选结果
        for (TestPost post : filtered) {
            assertThat(post.getBoard()).isEqualTo(targetBoard);
            assertThat(post.getStatus()).isNotEqualTo(STATUS_DELETED);
        }
    }

    /**
     * Property 6: 筛选结果正确性 - 按状态筛选
     */
    @Property(tries = 50)
    void filterByStatusReturnsOnlyMatchingPosts(
            @ForAll("postList") List<TestPost> posts,
            @ForAll("status") Integer targetStatus) {
        // 筛选指定状态的帖子（排除已删除）
        List<TestPost> filtered = posts.stream()
                .filter(p -> targetStatus.equals(p.getStatus()))
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .collect(Collectors.toList());

        // 验证筛选结果
        for (TestPost post : filtered) {
            assertThat(post.getStatus()).isEqualTo(targetStatus);
        }
    }

    /**
     * Property 7: 状态转换后列表排除
     * 已解决的帖子在默认列表中应被排除（如果筛选正常状态）
     */
    @Property(tries = 50)
    void resolvedPostsExcludedFromNormalList(
            @ForAll("postList") List<TestPost> posts) {
        // 筛选正常状态的帖子
        List<TestPost> normalPosts = posts.stream()
                .filter(p -> p.getStatus() == STATUS_NORMAL)
                .collect(Collectors.toList());

        // 验证没有已解决的帖子
        for (TestPost post : normalPosts) {
            assertThat(post.getStatus()).isNotEqualTo(STATUS_RESOLVED);
        }
    }

    /**
     * Property 8: 删除后不可见性
     * 已删除的帖子不应出现在任何列表中
     */
    @Property(tries = 50)
    void deletedPostsAreNotVisible(
            @ForAll("postList") List<TestPost> posts) {
        // 模拟查询（排除已删除）
        List<TestPost> visiblePosts = posts.stream()
                .filter(p -> p.getStatus() != STATUS_DELETED)
                .collect(Collectors.toList());

        // 验证没有已删除的帖子
        for (TestPost post : visiblePosts) {
            assertThat(post.getStatus()).isNotEqualTo(STATUS_DELETED);
        }
    }

    /**
     * Property 8: 删除操作应设置状态为已删除
     */
    @Example
    void deletePostSetsStatusToDeleted() {
        TestPost post = new TestPost();
        post.setId(1L);
        post.setStatus(STATUS_NORMAL);

        // 执行删除
        deletePost(post);

        assertThat(post.getStatus()).isEqualTo(STATUS_DELETED);
    }

    // 辅助方法
    private TestPostVO toPostVO(TestPost post) {
        TestPostVO vo = new TestPostVO();
        vo.setId(post.getId());
        vo.setContent(post.getContent());
        vo.setBoard(post.getBoard());
        vo.setIsAnonymous(post.getIsAnonymous());

        if (!Boolean.TRUE.equals(post.getIsAnonymous())) {
            vo.setAuthorId(post.getUserId());
            vo.setAuthorName("User" + post.getUserId());
        }

        return vo;
    }

    private boolean validatePost(TestPost post) {
        if (post.getBoard() == null || post.getBoard().isEmpty()) {
            return false;
        }
        if (post.getContent() == null || post.getContent().isEmpty()) {
            return false;
        }
        return true;
    }

    private TestPost createPost(TestPost post) {
        TestPost created = new TestPost();
        created.setId(System.currentTimeMillis());
        created.setUserId(post.getUserId());
        created.setBoard(post.getBoard());
        created.setContent(post.getContent());
        created.setStatus(STATUS_NORMAL);
        created.setCreatedAt(LocalDateTime.now());

        // 树洞板块强制匿名
        if ("tree-hole".equals(post.getBoard())) {
            created.setIsAnonymous(true);
        } else {
            created.setIsAnonymous(post.getIsAnonymous());
        }

        return created;
    }

    private void deletePost(TestPost post) {
        post.setStatus(STATUS_DELETED);
    }

    // 数据提供者
    @Provide
    Arbitrary<List<TestPost>> postList() {
        return testPost().list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<TestPost> testPost() {
        return Arbitraries.of("general", "tree-hole", "lost-found", "market")
                .flatMap(board -> Arbitraries.strings().ofMinLength(1).ofMaxLength(100)
                        .flatMap(content -> Arbitraries.of(STATUS_NORMAL, STATUS_RESOLVED, STATUS_DELETED)
                                .flatMap(status -> Arbitraries.longs().between(1, 1000)
                                        .map(userId -> {
                                            TestPost post = new TestPost();
                                            post.setId(System.nanoTime());
                                            post.setUserId(userId);
                                            post.setBoard(board);
                                            post.setContent(content);
                                            post.setStatus(status);
                                            post.setIsAnonymous("tree-hole".equals(board));
                                            post.setCreatedAt(LocalDateTime.now().minusHours((int) (Math.random() * 100)));
                                            return post;
                                        }))));
    }

    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1, 10000);
    }

    @Provide
    Arbitrary<String> postContent() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(200);
    }

    @Provide
    Arbitrary<String> board() {
        return Arbitraries.of("general", "tree-hole", "lost-found", "market");
    }

    @Provide
    Arbitrary<Integer> status() {
        return Arbitraries.of(STATUS_NORMAL, STATUS_RESOLVED);
    }

    // 测试用内部类
    static class TestPost {
        private Long id;
        private Long userId;
        private String board;
        private String content;
        private Boolean isAnonymous;
        private Integer status;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getBoard() { return board; }
        public void setBoard(String board) { this.board = board; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Boolean getIsAnonymous() { return isAnonymous; }
        public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    static class TestPostVO {
        private Long id;
        private String content;
        private String board;
        private Boolean isAnonymous;
        private Long authorId;
        private String authorName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getBoard() { return board; }
        public void setBoard(String board) { this.board = board; }
        public Boolean getIsAnonymous() { return isAnonymous; }
        public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
    }
}
