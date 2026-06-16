package com.campus.wall.service.market.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.util.BoardUtil;
import com.campus.wall.util.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.entity.market.MarketOrder;
import com.campus.wall.entity.post.Post;
import com.campus.wall.entity.post.PostBoard;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.market.MarketOrderMapper;
import com.campus.wall.mapper.post.PostBoardMapper;
import com.campus.wall.mapper.post.PostMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.market.MarketOrderService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.vo.market.MarketOrderVO;
import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PostBoardMapper postBoardMapper;
    private final UserMapper userMapper;
    private final CreditService creditService;

    // 订单状态：0进行中 1已完成 2已取消
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_CANCELLED = 2;

    // 市集板块
    private static final String BOARD_MARKET = BoardUtil.BOARD_MARKET;

    @Override
    @Transactional
    public Long createOrder(MarketOrderCreateDTO dto) {
        Long buyerId = StpUtil.getLoginIdAsLong();

        // 检查帖子是否存在且是市集帖子
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!isMarketPost(post)) {
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

        try {
            marketOrderMapper.insert(order);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该商品已有进行中的订单");
        }

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

        int updated = marketOrderMapper.update(
            null,
            new LambdaUpdateWrapper<MarketOrder>()
                .eq(MarketOrder::getId, orderId)
                .eq(MarketOrder::getStatus, STATUS_PENDING)
                .eq(MarketOrder::getBuyerConfirmed, false)
                .set(MarketOrder::getBuyerConfirmed, true)
        );
        if (updated == 0) {
            MarketOrder latest = getOrderOrThrow(orderId);
            if (latest.getStatus() != STATUS_PENDING) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许确认");
            }
        }

        // 检查是否双方都已确认
        checkAndCompleteOrder(orderId);

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

        int updated = marketOrderMapper.update(
            null,
            new LambdaUpdateWrapper<MarketOrder>()
                .eq(MarketOrder::getId, orderId)
                .eq(MarketOrder::getStatus, STATUS_PENDING)
                .eq(MarketOrder::getSellerConfirmed, false)
                .set(MarketOrder::getSellerConfirmed, true)
        );
        if (updated == 0) {
            MarketOrder latest = getOrderOrThrow(orderId);
            if (latest.getStatus() != STATUS_PENDING) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许确认");
            }
        }

        // 检查是否双方都已确认
        checkAndCompleteOrder(orderId);

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

        int updated = marketOrderMapper.update(
            null,
            new LambdaUpdateWrapper<MarketOrder>()
                .eq(MarketOrder::getId, orderId)
                .eq(MarketOrder::getStatus, STATUS_PENDING)
                .set(MarketOrder::getStatus, STATUS_CANCELLED)
        );
        if (updated == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "订单状态不允许取消");
        }

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

        List<MarketOrderVO> records = toOrderVOList(result.getRecords());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
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

        List<MarketOrderVO> records = toOrderVOList(result.getRecords());

        return PageResult.of(records, result.getTotal(), result.getSize(), result.getCurrent());
    }

    private void checkAndCompleteOrder(Long orderId) {
        // 重新查询最新状态
        MarketOrder order = marketOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }

        if (Boolean.TRUE.equals(order.getBuyerConfirmed()) 
                && Boolean.TRUE.equals(order.getSellerConfirmed())) {
            // 双方都确认，订单完成
            int updated = marketOrderMapper.update(
                null,
                new LambdaUpdateWrapper<MarketOrder>()
                    .eq(MarketOrder::getId, orderId)
                    .eq(MarketOrder::getStatus, STATUS_PENDING)
                    .eq(MarketOrder::getBuyerConfirmed, true)
                    .eq(MarketOrder::getSellerConfirmed, true)
                    .set(MarketOrder::getStatus, STATUS_COMPLETED)
                    .set(MarketOrder::getCompletedAt, LocalDateTime.now())
            );
            if (updated == 0) {
                return;
            }

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

    private List<MarketOrderVO> toOrderVOList(List<MarketOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        Set<Long> postIdSet = new HashSet<>();
        Set<Long> userIdSet = new HashSet<>();
        for (MarketOrder order : orders) {
            if (order == null) {
                continue;
            }
            if (order.getPostId() != null) {
                postIdSet.add(order.getPostId());
            }
            if (order.getSellerId() != null) {
                userIdSet.add(order.getSellerId());
            }
            if (order.getBuyerId() != null) {
                userIdSet.add(order.getBuyerId());
            }
        }
        Map<Long, Post> postMap = postIdSet.isEmpty()
            ? Map.of()
            : postMapper.selectBatchIds(new ArrayList<>(postIdSet)).stream()
                .collect(Collectors.toMap(Post::getId, post -> post, (a, b) -> a));
        Map<Long, User> userMap = userIdSet.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(new ArrayList<>(userIdSet)).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
        List<MarketOrderVO> result = new ArrayList<>(orders.size());
        for (MarketOrder order : orders) {
            if (order == null) {
                continue;
            }
            result.add(buildOrderVO(order,
                order.getPostId() == null ? null : postMap.get(order.getPostId()),
                order.getSellerId() == null ? null : userMap.get(order.getSellerId()),
                order.getBuyerId() == null ? null : userMap.get(order.getBuyerId())));
        }
        return result;
    }

    private MarketOrderVO toOrderVO(MarketOrder order) {
        Post post = order.getPostId() == null ? null : postMapper.selectById(order.getPostId());
        User seller = order.getSellerId() == null ? null : userMapper.selectById(order.getSellerId());
        User buyer = order.getBuyerId() == null ? null : userMapper.selectById(order.getBuyerId());
        return buildOrderVO(order, post, seller, buyer);
    }

    private MarketOrderVO buildOrderVO(MarketOrder order, Post post, User seller, User buyer) {
        MarketOrderVO vo = new MarketOrderVO();
        vo.setId(order.getId());
        vo.setPrice(order.getPrice());
        vo.setStatus(order.getStatus());
        vo.setBuyerConfirmed(order.getBuyerConfirmed());
        vo.setSellerConfirmed(order.getSellerConfirmed());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCompletedAt(order.getCompletedAt());

        // 帖子信息
        if (post != null) {
            PostVO postVO = new PostVO();
            postVO.setId(post.getId());
            postVO.setTitle(post.getTitle());
            postVO.setPrice(post.getPrice());
            vo.setPost(postVO);
        }

        // 卖家信息
        if (seller != null) {
            UserVO sellerVO = new UserVO();
            sellerVO.setId(seller.getId());
            sellerVO.setUsername(seller.getUsername());
            sellerVO.setNickname(seller.getNickname());
            sellerVO.setAvatar(seller.getAvatar());
            vo.setSeller(sellerVO);
        }

        // 买家信息
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

    private boolean isMarketPost(Post post) {
        if (post == null) {
            return false;
        }
        String normalizedBoard = BoardUtil.normalizeBoardKey(post.getBoard());
        if (BOARD_MARKET.equals(normalizedBoard)) {
            return true;
        }
        return postBoardMapper.selectCount(
                new LambdaQueryWrapper<PostBoard>()
                        .eq(PostBoard::getPostId, post.getId())
                        .eq(PostBoard::getBoard, BOARD_MARKET)
        ) > 0;
    }
}
