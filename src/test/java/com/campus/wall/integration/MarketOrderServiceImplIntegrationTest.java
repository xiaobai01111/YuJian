package com.campus.wall.integration;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.market.MarketOrderMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.market.MarketOrderService;
import com.campus.wall.support.IntegrationTestBase;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class MarketOrderServiceImplIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MarketOrderService marketOrderService;

    @Autowired
    private MarketOrderMapper marketOrderMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    private Long sellerId;
    private Long buyerId;
    private Long buyer2Id;
    private Long postId;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        marketOrderMapper.delete(null);
        postMapper.delete(null);
        long suffix = System.nanoTime();
        sellerId = createUser("seller_user_" + suffix);
        buyerId = createUser("buyer_user_" + suffix);
        buyer2Id = createUser("buyer_user2_" + suffix);
        postId = createMarketPost(sellerId);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void createOrder_duplicatePendingRejected() {
        MarketOrderCreateDTO dto = new MarketOrderCreateDTO();
        dto.setPostId(postId);

        StpUtil.login(buyerId);
        marketOrderService.createOrder(dto);

        StpUtil.logout();
        StpUtil.login(buyer2Id);

        assertThatThrownBy(() -> marketOrderService.createOrder(dto))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该商品已有进行中的订单");
    }

    private Long createUser(String username) {
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

    private Long createMarketPost(Long sellerId) {
        Post post = new Post();
        post.setUserId(sellerId);
        post.setBoard("market");
        post.setTitle("item");
        post.setContent("content");
        post.setPrice(new BigDecimal("19.9"));
        post.setStatus(0);
        post.setShowOnHome(true);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postMapper.insert(post);
        return post.getId();
    }
}
