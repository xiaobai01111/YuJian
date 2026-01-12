package com.campus.wall.property;

import com.campus.wall.entity.market.MarketOrder;
import com.campus.wall.entity.post.Comment;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.system.Notification;
import com.campus.wall.entity.system.SysMenu;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 属性测试：序列化往返一致性
 * 
 * **Feature: campus-wall, Property 11: 序列化往返一致性**
 * *对于任意* 系统数据对象，JSON 序列化后反序列化应得到等效对象
 * **Validates: Requirements 9.3, 9.4**
 */
class SerializationPropertyTest {

    private final ObjectMapper objectMapper;

    SerializationPropertyTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // =============================================
    // User Entity
    // =============================================

    @Property(tries = 100)
    void userSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String username,
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String nickname,
            @ForAll @IntRange(min = 0, max = 2) int verifyStatus,
            @ForAll @IntRange(min = 0, max = 1) int status,
            @ForAll @IntRange(min = 0, max = 100) int creditScore
    ) throws Exception {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("hashed_password");
        user.setNickname(nickname);
        user.setVerifyStatus(verifyStatus);
        user.setStatus(status);
        user.setCreditScore(creditScore);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(user);
        User deserialized = objectMapper.readValue(json, User.class);

        assert user.getId().equals(deserialized.getId());
        assert user.getUsername().equals(deserialized.getUsername());
        assert user.getNickname().equals(deserialized.getNickname());
        assert user.getVerifyStatus().equals(deserialized.getVerifyStatus());
        assert user.getStatus().equals(deserialized.getStatus());
        assert user.getCreditScore().equals(deserialized.getCreditScore());
    }

    // =============================================
    // Post Entity
    // =============================================

    @Property(tries = 100)
    void postSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long userId,
            @ForAll("boardProvider") String board,
            @ForAll @StringLength(min = 1, max = 200) @AlphaChars String title,
            @ForAll @StringLength(min = 1, max = 1000) @AlphaChars String content,
            @ForAll boolean isAnonymous,
            @ForAll @IntRange(min = 0, max = 4) int status
    ) throws Exception {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setBoard(board);
        post.setTitle(title);
        post.setContent(content);
        post.setIsAnonymous(isAnonymous);
        post.setStatus(status);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(post);
        Post deserialized = objectMapper.readValue(json, Post.class);

        assert post.getId().equals(deserialized.getId());
        assert post.getUserId().equals(deserialized.getUserId());
        assert post.getBoard().equals(deserialized.getBoard());
        assert post.getTitle().equals(deserialized.getTitle());
        assert post.getContent().equals(deserialized.getContent());
        assert post.getIsAnonymous().equals(deserialized.getIsAnonymous());
        assert post.getStatus().equals(deserialized.getStatus());
    }

    @Provide
    Arbitrary<String> boardProvider() {
        return Arbitraries.of("confession", "treehole", "help", "market", "lost", "freshman");
    }

    // =============================================
    // Comment Entity
    // =============================================

    @Property(tries = 100)
    void commentSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long postId,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long userId,
            @ForAll @StringLength(min = 1, max = 500) @AlphaChars String content,
            @ForAll boolean isOwner,
            @ForAll @IntRange(min = 0, max = 1) int status
    ) throws Exception {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setIsOwner(isOwner);
        comment.setStatus(status);
        comment.setCreatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(comment);
        Comment deserialized = objectMapper.readValue(json, Comment.class);

        assert comment.getId().equals(deserialized.getId());
        assert comment.getPostId().equals(deserialized.getPostId());
        assert comment.getUserId().equals(deserialized.getUserId());
        assert comment.getContent().equals(deserialized.getContent());
        assert comment.getIsOwner().equals(deserialized.getIsOwner());
        assert comment.getStatus().equals(deserialized.getStatus());
    }

    // =============================================
    // Notification Entity
    // =============================================

    @Property(tries = 100)
    void notificationSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long userId,
            @ForAll("notificationTypeProvider") String type,
            @ForAll @StringLength(min = 1, max = 200) @AlphaChars String title,
            @ForAll boolean isRead
    ) throws Exception {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setIsRead(isRead);
        notification.setCreatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(notification);
        Notification deserialized = objectMapper.readValue(json, Notification.class);

        assert notification.getId().equals(deserialized.getId());
        assert notification.getUserId().equals(deserialized.getUserId());
        assert notification.getType().equals(deserialized.getType());
        assert notification.getTitle().equals(deserialized.getTitle());
        assert notification.getIsRead().equals(deserialized.getIsRead());
    }

    @Provide
    Arbitrary<String> notificationTypeProvider() {
        return Arbitraries.of("like", "comment", "system", "report");
    }

    // =============================================
    // MarketOrder Entity
    // =============================================

    @Property(tries = 100)
    void marketOrderSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long postId,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long sellerId,
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long buyerId,
            @ForAll @DoubleRange(min = 0.01, max = 99999.99) double priceValue,
            @ForAll @IntRange(min = 0, max = 3) int status
    ) throws Exception {
        MarketOrder order = new MarketOrder();
        order.setId(id);
        order.setPostId(postId);
        order.setSellerId(sellerId);
        order.setBuyerId(buyerId);
        order.setPrice(BigDecimal.valueOf(priceValue).setScale(2, java.math.RoundingMode.HALF_UP));
        order.setStatus(status);
        order.setBuyerConfirmed(false);
        order.setSellerConfirmed(false);
        order.setCreatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(order);
        MarketOrder deserialized = objectMapper.readValue(json, MarketOrder.class);

        assert order.getId().equals(deserialized.getId());
        assert order.getPostId().equals(deserialized.getPostId());
        assert order.getSellerId().equals(deserialized.getSellerId());
        assert order.getBuyerId().equals(deserialized.getBuyerId());
        assert order.getPrice().compareTo(deserialized.getPrice()) == 0;
        assert order.getStatus().equals(deserialized.getStatus());
    }

    // =============================================
    // SysRole Entity
    // =============================================

    @Property(tries = 100)
    void sysRoleSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String roleName,
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String roleKey,
            @ForAll @IntRange(min = 0, max = 1) int status
    ) throws Exception {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(status);
        role.setSortOrder(0);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(role);
        SysRole deserialized = objectMapper.readValue(json, SysRole.class);

        assert role.getId().equals(deserialized.getId());
        assert role.getRoleName().equals(deserialized.getRoleName());
        assert role.getRoleKey().equals(deserialized.getRoleKey());
        assert role.getStatus().equals(deserialized.getStatus());
    }

    // =============================================
    // SysMenu Entity
    // =============================================

    @Property(tries = 100)
    void sysMenuSerializationRoundTrip(
            @ForAll @LongRange(min = 1, max = Long.MAX_VALUE) Long id,
            @ForAll @LongRange(min = 0, max = Long.MAX_VALUE) Long parentId,
            @ForAll @StringLength(min = 1, max = 50) @AlphaChars String name,
            @ForAll @IntRange(min = 0, max = 2) int type,
            @ForAll boolean visible
    ) throws Exception {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setType(type);
        menu.setVisible(visible);
        menu.setSortOrder(0);
        menu.setCreatedAt(LocalDateTime.now());
        menu.setUpdatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(menu);
        SysMenu deserialized = objectMapper.readValue(json, SysMenu.class);

        assert menu.getId().equals(deserialized.getId());
        assert menu.getParentId().equals(deserialized.getParentId());
        assert menu.getName().equals(deserialized.getName());
        assert menu.getType().equals(deserialized.getType());
        assert menu.getVisible().equals(deserialized.getVisible());
    }
}
