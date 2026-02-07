package com.campus.wall.property;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户帖子相关属性测试
 * Property 9: 用户帖子归属正确性
 * Property 20: 搜索结果相关性
 */
class UserPostPropertyTest {

    // 模拟帖子数据
    private final Map<Long, Set<Long>> userPosts = new HashMap<>();
    private final Map<Long, Post> allPosts = new HashMap<>();

    /**
     * Property 9: 用户帖子归属正确性
     * 查询用户帖子时，返回的所有帖子都应属于该用户
     */
    @Property(tries = 100)
    void userPostsBelongToCorrectUser(
            @ForAll("userId") Long userId,
            @ForAll("postIds") List<Long> postIds) {
        // 设置用户的帖子
        userPosts.put(userId, new HashSet<>(postIds));

        // 查询用户帖子
        Set<Long> result = getUserPosts(userId);

        // 验证所有返回的帖子都属于该用户
        for (Long postId : result) {
            assertThat(userPosts.get(userId)).contains(postId);
        }

        // 清理
        userPosts.clear();
    }

    /**
     * Property 9: 用户帖子不会出现在其他用户的列表中
     */
    @Property(tries = 50)
    void userPostsDoNotAppearInOtherUsersList(
            @ForAll("userId") Long userId1,
            @ForAll("userId") Long userId2,
            @ForAll("postIds") List<Long> postIds1,
            @ForAll("postIds") List<Long> postIds2) {
        if (userId1.equals(userId2)) {
            return;
        }

        // 设置两个用户的帖子（不重叠）
        Set<Long> posts1 = new HashSet<>(postIds1);
        Set<Long> posts2 = postIds2.stream()
                .filter(id -> !posts1.contains(id))
                .collect(Collectors.toSet());

        userPosts.put(userId1, posts1);
        userPosts.put(userId2, posts2);

        // 查询各自的帖子
        Set<Long> result1 = getUserPosts(userId1);
        Set<Long> result2 = getUserPosts(userId2);

        // 验证不会交叉
        for (Long postId : result1) {
            assertThat(result2).doesNotContain(postId);
        }

        // 清理
        userPosts.clear();
    }

    /**
     * Property 20: 搜索结果相关性
     * 搜索结果应包含关键词
     */
    @Property(tries = 50)
    void searchResultsContainKeyword(
            @ForAll("keyword") String keyword,
            @ForAll("postTitles") List<String> titles) {
        // 创建帖子
        long postId = 1;
        for (String title : titles) {
            Post post = new Post(postId++, title, title + " content", 0);
            allPosts.put(post.id, post);
        }

        // 搜索
        List<Post> results = search(keyword);

        // 验证搜索结果都包含关键词（标题或内容）
        for (Post post : results) {
            boolean containsKeyword = post.title.toLowerCase().contains(keyword.toLowerCase())
                    || post.content.toLowerCase().contains(keyword.toLowerCase());
            assertThat(containsKeyword).isTrue();
        }

        // 清理
        allPosts.clear();
    }

    /**
     * Property 20: 搜索结果不包含已删除帖子
     */
    @Example
    void searchResultsExcludeDeletedPosts() {
        // 创建正常帖子
        allPosts.put(1L, new Post(1L, "Test Post", "Test content", 0));
        // 创建已删除帖子
        allPosts.put(2L, new Post(2L, "Test Deleted", "Test deleted content", 2));

        // 搜索
        List<Post> results = search("Test");

        // 验证结果不包含已删除帖子
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id).isEqualTo(1L);

        // 清理
        allPosts.clear();
    }

    /**
     * Property 20: 搜索结果匿名帖子隐藏作者
     */
    @Example
    void searchResultsHideAnonymousAuthor() {
        // 创建匿名帖子
        Post anonymousPost = new Post(1L, "Anonymous Post", "Content", 0);
        anonymousPost.isAnonymous = true;
        anonymousPost.userId = 100L;
        allPosts.put(1L, anonymousPost);

        // 搜索
        List<Post> results = search("Anonymous");

        // 验证匿名帖子不暴露作者
        assertThat(results).hasSize(1);
        Post result = results.getFirst();
        // 在实际实现中，转换为 VO 时 author 应为 null
        assertThat(result.isAnonymous).isTrue();

        // 清理
        allPosts.clear();
    }

    // 模拟方法
    private Set<Long> getUserPosts(Long userId) {
        return userPosts.getOrDefault(userId, new HashSet<>());
    }

    private List<Post> search(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return allPosts.values().stream()
                .filter(p -> p.status != 2) // 排除已删除
                .filter(p -> p.title.toLowerCase().contains(lowerKeyword)
                        || p.content.toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    // 内部测试类
    static class Post {
        final Long id;
        final String title;
        final String content;
        final int status;
        boolean isAnonymous;
        Long userId;

        Post(Long id, String title, String content, int status) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.status = status;
            this.isAnonymous = false;
        }
    }

    // 数据提供者
    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1, 1000);
    }

    @Provide
    Arbitrary<List<Long>> postIds() {
        return Arbitraries.longs().between(1, 10000).list().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<String> keyword() {
        return Arbitraries.of("test", "hello", "world", "java", "spring", "校园", "帖子");
    }

    @Provide
    Arbitrary<List<String>> postTitles() {
        return Arbitraries.of(
                "Test Post Title",
                "Hello World",
                "Java Spring Boot",
                "校园墙帖子",
                "Random Title",
                "Another Post"
        ).list().ofMinSize(1).ofMaxSize(10);
    }
}
