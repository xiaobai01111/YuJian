package com.campus.wall.service.market;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.entity.market.MarketOrder;
import com.campus.wall.entity.post.Post;
import com.campus.wall.mapper.market.MarketOrderMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.market.impl.MarketOrderServiceImpl;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketOrderServiceImplTest {

    @Mock
    private MarketOrderMapper marketOrderMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostBoardMapper postBoardMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CreditService creditService;

    @InjectMocks
    private MarketOrderServiceImpl marketOrderService;

    @BeforeEach
    void setUp() {
        SaTokenTestContext.bind();
        StpUtil.login(1L);
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
        SaTokenTestContext.clear();
    }

    @Test
    void createOrder_nonMarketPost_throws() {
        Post post = new Post();
        post.setId(10L);
        post.setUserId(2L);
        post.setBoard("help");
        post.setPrice(new BigDecimal("9.9"));

        when(postMapper.selectById(10L)).thenReturn(post);
        when(postBoardMapper.selectCount(any())).thenReturn(0L);

        MarketOrderCreateDTO dto = new MarketOrderCreateDTO();
        dto.setPostId(10L);

        assertThatThrownBy(() -> marketOrderService.createOrder(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("该帖子不是市集商品");
    }

    @Test
    void createOrder_existingPending_throws() {
        Post post = new Post();
        post.setId(11L);
        post.setUserId(2L);
        post.setBoard("market");
        post.setPrice(new BigDecimal("19.9"));

        when(postMapper.selectById(11L)).thenReturn(post);
        when(marketOrderMapper.selectOne(any())).thenReturn(new MarketOrder());

        MarketOrderCreateDTO dto = new MarketOrderCreateDTO();
        dto.setPostId(11L);

        assertThatThrownBy(() -> marketOrderService.createOrder(dto))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("该商品已有进行中的订单");
    }

    @Test
    void buyerConfirm_notBuyer_forbidden() {
        MarketOrder order = new MarketOrder();
        order.setId(1L);
        order.setBuyerId(2L);
        order.setSellerId(3L);
        order.setStatus(0);

        when(marketOrderMapper.selectById(1L)).thenReturn(order);

        assertThatThrownBy(() -> marketOrderService.buyerConfirm(1L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.FORBIDDEN.getCode())
            .hasMessage("无权操作此订单");
    }

    @Test
    void sellerConfirm_statusNotPending_throws() {
        MarketOrder order = new MarketOrder();
        order.setId(2L);
        order.setBuyerId(1L);
        order.setSellerId(1L);
        order.setStatus(1);

        when(marketOrderMapper.selectById(2L)).thenReturn(order);

        assertThatThrownBy(() -> marketOrderService.sellerConfirm(2L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("订单状态不允许确认");
    }

    @Test
    void buyerConfirm_updateZeroAndStatusChanged_throws() {
        MarketOrder first = new MarketOrder();
        first.setId(3L);
        first.setBuyerId(1L);
        first.setSellerId(2L);
        first.setStatus(0);

        MarketOrder latest = new MarketOrder();
        latest.setId(3L);
        latest.setBuyerId(1L);
        latest.setSellerId(2L);
        latest.setStatus(1);

        when(marketOrderMapper.selectById(3L)).thenReturn(first, latest);
        when(marketOrderMapper.update(eq(null), any())).thenReturn(0);

        assertThatThrownBy(() -> marketOrderService.buyerConfirm(3L))
            .isInstanceOf(BusinessException.class)
            .matches(e -> ((BusinessException) e).getCode() == ResultCode.BAD_REQUEST.getCode())
            .hasMessage("订单状态不允许确认");
    }

    @Test
    void sellerConfirm_completesOrder_rewardsBoth() {
        MarketOrder first = new MarketOrder();
        first.setId(4L);
        first.setBuyerId(10L);
        first.setSellerId(11L);
        first.setStatus(0);
        first.setSellerConfirmed(false);

        MarketOrder latest = new MarketOrder();
        latest.setId(4L);
        latest.setBuyerId(10L);
        latest.setSellerId(11L);
        latest.setStatus(0);
        latest.setBuyerConfirmed(true);
        latest.setSellerConfirmed(true);

        StpUtil.login(11L);
        when(marketOrderMapper.selectById(4L)).thenReturn(first, latest);
        when(marketOrderMapper.update(eq(null), any())).thenReturn(1, 1);

        marketOrderService.sellerConfirm(4L);

        verify(creditService).rewardForTransaction(10L);
        verify(creditService).rewardForTransaction(11L);
    }
}
