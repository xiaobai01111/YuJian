package com.campus.wall.service.market.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.entity.market.MarketOrder;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.market.MarketOrderMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.market.MarketOrderService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.vo.market.MarketOrderVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 市集订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketOrderServiceImpl implements MarketOrderService {

    private final MarketOrderMapper marketOrderMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    // 订单状态：0进行中 1已完成 2已取消
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_CANCELLED = 2;

    // 市集板块
    private static final String BOARD_MARKET = "market";

    @Override
    @Transactional
    public Long createOrder(MarketOrderCreateDTO dto) {
        Long buyerId = StpUtil.getLoginIdAsLong();

        // 检查帖子是否存在且是市集帖子
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!BOARD_MARKET.equals(post.getBoard())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该帖子不是市集商品");
        }

        // 不能购买自己的商品
        if (post.getUserId().equals(buyerId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能购买自己的商品");
        }

        // 检查是否已有进行中的订单
        MarketOrder existing = marketOrderMapper.selectOne(
                new LambdaQueryWrapper<MarketOrder>()
                        .eq(MarketOrder::getPostId, dto.getPostId())
                        .eq(MarketOrder::getStatus, STATUS_PENDING)
        );
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该商品已有进行中的订单");
        }

        // 创建订单（使用帖子价格，防止买家篡改）
        MarketOrder order = new MarketOrder();
        order.setPostId(dto.getPostId());
        order.setSellerId(post.getUserId());
        order.setBuyerId(buyerId);
        order.setPrice(post.getPrice());
        order.setStatus(STATUS_PENDING);
        order.setBuyerConfirmed(false);
        order.setSellerConfirmed(false);

        marketOrderMapper.insert(order);

        log.info("买家 {} 创建订单: postId={}, orderId={}", buyerId, dto.getPostId(), order.getId());
        return order.getId();
    }

    @Override
    @Transactional
    public void buyerConfirm(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketOrder order = getOrderOrThrow(orderId);

        // 权限校验
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许确认");
        }

        order.setBuyerConfirmed(true);
        marketOrderMapper.updateById(order);

        // 检查是否双方都已确认
        checkAndCompleteOrder(order);

        log.info("买家 {} 确认订单: {}", userId, orderId);
    }

    @Override
    @Transactional
    public void sellerConfirm(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketOrder order = getOrderOrThrow(orderId);

        // 权限校验
        if (!order.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许确认");
        }

        order.setSellerConfirmed(true);
        marketOrderMapper.updateById(order);

        // 检查是否双方都已确认
        checkAndCompleteOrder(order);

        log.info("卖家 {} 确认订单: {}", userId, orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketOrder order = getOrderOrThrow(orderId);

        // 权限校验：买家或卖家可取消
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此订单");
        }

        if (order.getStatus() != STATUS_PENDING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许取消");
        }

        order.setStatus(STATUS_CANCELLED);
        marketOrderMapper.updateById(order);

        log.info("用户 {} 取消订单: {}", userId, orderId);
    }

    @Override
    public MarketOrderVO getOrderDetail(Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketOrder order = getOrderOrThrow(orderId);
        boolean isAdmin = StpUtil.hasRole(SecurityUtil.getSuperAdminRoleKey());
        if (!isAdmin && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此订单");
        }
        return toOrderVO(order);
    }

    @Override
    public PageResult<MarketOrderVO> getBuyerOrders(Long userId, int page, int size) {
        Page<MarketOrder> orderPage = new Page<>(page, size);

        Page<MarketOrder> result = marketOrderMapper.selectPage(
                orderPage,
                new LambdaQueryWrapper<MarketOrder>()
                        .eq(MarketOrder::getBuyerId, userId)
                        .orderByDesc(MarketOrder::getCreatedAt)
        );

        List<MarketOrderVO> records = result.getRecords().stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<MarketOrderVO> getSellerOrders(Long userId, int page, int size) {
        Page<MarketOrder> orderPage = new Page<>(page, size);

        Page<MarketOrder> result = marketOrderMapper.selectPage(
                orderPage,
                new LambdaQueryWrapper<MarketOrder>()
                        .eq(MarketOrder::getSellerId, userId)
                        .orderByDesc(MarketOrder::getCreatedAt)
        );

        List<MarketOrderVO> records = result.getRecords().stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private void checkAndCompleteOrder(MarketOrder order) {
        // 重新查询最新状态
        order = marketOrderMapper.selectById(order.getId());

        if (Boolean.TRUE.equals(order.getBuyerConfirmed()) 
                && Boolean.TRUE.equals(order.getSellerConfirmed())) {
            // 双方都确认，订单完成
            order.setStatus(STATUS_COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            marketOrderMapper.updateById(order);

            // 双方都获得信用分 +5
            creditService.rewardForTransaction(order.getBuyerId());
            creditService.rewardForTransaction(order.getSellerId());

            log.info("订单完成: orderId={}, buyerId={}, sellerId={}", 
                    order.getId(), order.getBuyerId(), order.getSellerId());
        }
    }

    private MarketOrder getOrderOrThrow(Long orderId) {
        MarketOrder order = marketOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private MarketOrderVO toOrderVO(MarketOrder order) {
        MarketOrderVO vo = new MarketOrderVO();
        vo.setId(order.getId());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setBuyerConfirmed(order.getBuyerConfirmed());
        vo.setSellerConfirmed(order.getSellerConfirmed());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCompletedAt(order.getCompletedAt());

        // 帖子信息
        Post post = postMapper.selectById(order.getPostId());
        if (post != null) {
            PostVO postVO = new PostVO();
            postVO.setId(post.getId());
            postVO.setTitle(post.getTitle());
            postVO.setPrice(post.getPrice());
            vo.setPost(postVO);
        }

        // 卖家信息
        User seller = userMapper.selectById(order.getSellerId());
        if (seller != null) {
            UserVO sellerVO = new UserVO();
            sellerVO.setId(seller.getId());
            sellerVO.setUsername(seller.getUsername());
            sellerVO.setNickname(seller.getNickname());
            sellerVO.setAvatar(seller.getAvatar());
            vo.setSeller(sellerVO);
        }

        // 买家信息
        User buyer = userMapper.selectById(order.getBuyerId());
        if (buyer != null) {
            UserVO buyerVO = new UserVO();
            buyerVO.setId(buyer.getId());
            buyerVO.setUsername(buyer.getUsername());
            buyerVO.setNickname(buyer.getNickname());
            buyerVO.setAvatar(buyer.getAvatar());
            vo.setBuyer(buyerVO);
        }

        return vo;
    }
}
