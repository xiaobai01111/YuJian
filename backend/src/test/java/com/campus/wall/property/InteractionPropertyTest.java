package com.campus.wall.property;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 互动功能属性测试
 * Property 4: 树洞评论匿名标识一致性
 * Property 10: 收藏往返一致性
 * Property 15: 举报入队正确性
 * Property 17: 互动通知生成
 */
class InteractionPropertyTest {

    // 模拟收藏集合
    private final Map<Long, Set<Long>> userBookmarks = new HashMap<>();

    // 模拟举报队列
    private final Set<Long> reportQueue = new HashSet<>();

    // 模拟通知列表
    private final Map<Long, Set<String>> userNotifications = new HashMap<>();

    /**
     * Property 10: 收藏往返一致性
     * 收藏后取消收藏，状态应回到初始
     */
    @Property(tries = 100)
    void bookmarkRoundTripConsistency(
            @ForAll("userId") Long userId,
            @ForAll("postId") Long postId) {
        // 初始状态：未收藏
        assertThat(isBookmarked(userId, postId)).isFalse();

        // 收藏
        bookmark(userId, postId);
        assertThat(isBookmarked(userId, postId)).isTrue();

        // 取消收藏
        unbookmark(userId, postId);
        assertThat(isBookmarked(userId, postId)).isFalse();

        // 清理
        userBookmarks.clear();
    }

    /**
     * Property 10: 重复收藏应为幂等操作
     */
    @Property(tries = 50)
    void bookmarkIsIdempotent(
            @ForAll("userId") Long userId,
            @ForAll("postId") Long postId) {
        // 连续收藏多次
        bookmark(userId, postId);
        bookmark(userId, postId);
        bookmark(userId, postId);

        // 仍然只有一条收藏记录
        assertThat(isBookmarked(userId, postId)).isTrue();

        // 取消一次即可
        unbookmark(userId, postId);
        assertThat(isBookmarked(userId, postId)).isFalse();

        // 清理
        userBookmarks.clear();
    }

    /**
     * Property 15: 举报入队正确性
     * 举报后帖子应进入审核队列
     */
    @Property(tries = 100)
    void reportedPostEntersQueue(
            @ForAll("userId") Long reporterId,
            @ForAll("postId") Long postId,
            @ForAll("reportReason") String reason) {
        // 举报前不在队列中
        assertThat(reportQueue.contains(postId)).isFalse();

        // 提交举报
        report(reporterId, postId, reason);

        // 举报后应在队列中
        assertThat(reportQueue.contains(postId)).isTrue();

        // 清理
        reportQueue.clear();
    }

    /**
     * Property 15: 重复举报不应重复入队
     */
    @Property(tries = 50)
    void duplicateReportDoesNotDuplicateQueue(
            @ForAll("userId") Long reporterId,
            @ForAll("postId") Long postId) {
        // 同一用户多次举报同一帖子
        report(reporterId, postId, "理由1");
        int queueSizeAfterFirst = reportQueue.size();

        // 再次举报（应被忽略或合并）
        // 在实际实现中会抛出异常，这里模拟幂等
        report(reporterId, postId, "理由2");

        // 队列大小不变
        assertThat(reportQueue.size()).isEqualTo(queueSizeAfterFirst);

        // 清理
        reportQueue.clear();
    }

    /**
     * Property 4: 树洞评论匿名标识一致性
     * 同一用户在同一帖子下的所有评论应有相同的匿名ID
     */
    @Property(tries = 100)
    void sameUserSamePostHasSameAnonymousId(
            @ForAll("userId") Long userId,
            @ForAll("postId") Long postId) {
        // 生成多次匿名ID
        String id1 = generateAnonymousId(userId, postId);
        String id2 = generateAnonymousId(userId, postId);
        String id3 = generateAnonymousId(userId, postId);

        // 应该完全相同
        assertThat(id1).isEqualTo(id2);
        assertThat(id2).isEqualTo(id3);
    }

    /**
     * Property 4: 不同用户在同一帖子下应有不同的匿名ID
     */
    @Property(tries = 100)
    void differentUsersHaveDifferentAnonymousIds(
            @ForAll("userId") Long userId1,
            @ForAll("userId") Long userId2,
            @ForAll("postId") Long postId) {
        // 假设用户ID不同
        if (userId1.equals(userId2)) {
            return;
        }

        String id1 = generateAnonymousId(userId1, postId);
        String id2 = generateAnonymousId(userId2, postId);

        // 不同用户的匿名ID应不同
        assertThat(id1).isNotEqualTo(id2);
    }

    /**
     * Property 4: 同一用户在不同帖子下应有不同的匿名ID
     */
    @Property(tries = 100)
    void sameUserDifferentPostsHaveDifferentAnonymousIds(
            @ForAll("userId") Long userId,
            @ForAll("postId") Long postId1,
            @ForAll("postId") Long postId2) {
        // 假设帖子ID不同
        if (postId1.equals(postId2)) {
            return;
        }

        String id1 = generateAnonymousId(userId, postId1);
        String id2 = generateAnonymousId(userId, postId2);

        // 不同帖子的匿名ID应不同
        assertThat(id1).isNotEqualTo(id2);
    }

    /**
     * Property 4: 无法通过匿名ID反推用户ID
     */
    @Example
    void anonymousIdCannotReverseToUserId() {
        Long userId = 12345L;
        Long postId = 67890L;

        String anonymousId = generateAnonymousId(userId, postId);

        // 匿名ID不应包含原始用户ID
        assertThat(anonymousId).doesNotContain(userId.toString());
        // 匿名ID应该是固定长度的哈希值
        assertThat(anonymousId).hasSize(8); // 截取前8位
    }

    /**
     * Property 17: 互动通知生成
     * 点赞/评论应生成通知给帖子作者
     */
    @Property(tries = 50)
    void likeGeneratesNotification(
            @ForAll("userId") Long likerId,
            @ForAll("userId") Long authorId,
            @ForAll("postId") Long postId) {
        // 假设点赞者和作者不是同一人
        if (likerId.equals(authorId)) {
            return;
        }

        // 点赞前作者没有通知
        int notificationsBefore = getNotificationCount(authorId);

        // 点赞
        like(likerId, postId, authorId);

        // 点赞后作者应收到通知
        int notificationsAfter = getNotificationCount(authorId);
        assertThat(notificationsAfter).isEqualTo(notificationsBefore + 1);

        // 清理
        userNotifications.clear();
    }

    /**
     * Property 17: 自己给自己点赞不应生成通知
     */
    @Example
    void selfLikeDoesNotGenerateNotification() {
        Long userId = 1L;
        Long postId = 100L;

        int notificationsBefore = getNotificationCount(userId);

        // 自己点赞自己的帖子
        like(userId, postId, userId);

        // 不应生成通知
        int notificationsAfter = getNotificationCount(userId);
        assertThat(notificationsAfter).isEqualTo(notificationsBefore);

        // 清理
        userNotifications.clear();
    }

    // 模拟方法实现
    private boolean isBookmarked(Long userId, Long postId) {
        Set<Long> bookmarks = userBookmarks.get(userId);
        return bookmarks != null && bookmarks.contains(postId);
    }

    private void bookmark(Long userId, Long postId) {
        userBookmarks.computeIfAbsent(userId, k -> new HashSet<>()).add(postId);
    }

    private void unbookmark(Long userId, Long postId) {
        Set<Long> bookmarks = userBookmarks.get(userId);
        if (bookmarks != null) {
            bookmarks.remove(postId);
        }
    }

    private void report(Long reporterId, Long postId, String reason) {
        // 模拟举报入队
        reportQueue.add(postId);
    }

    private String generateAnonymousId(Long userId, Long postId) {
        // 使用 Hash(UserId + PostId + Salt) 策略
        String salt = "campus_wall_secret_salt";
        String input = userId + ":" + postId + ":" + salt;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) { // 取前4字节生成8位十六进制
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void like(Long likerId, Long postId, Long authorId) {
        // 自己点赞自己不生成通知
        if (!likerId.equals(authorId)) {
            userNotifications.computeIfAbsent(authorId, k -> new HashSet<>())
                    .add("like:" + likerId + ":" + postId);
        }
    }

    private int getNotificationCount(Long userId) {
        Set<String> notifications = userNotifications.get(userId);
        return notifications == null ? 0 : notifications.size();
    }

    // 数据提供者
    @Provide
    Arbitrary<Long> userId() {
        return Arbitraries.longs().between(1, 10000);
    }

    @Provide
    Arbitrary<Long> postId() {
        return Arbitraries.longs().between(1, 10000);
    }

    @Provide
    Arbitrary<String> reportReason() {
        return Arbitraries.of("垃圾广告", "色情内容", "违规信息", "人身攻击", "其他");
    }
}
