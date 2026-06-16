package com.campus.wall.service.market;

import com.campus.wall.common.PageResult;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.vo.market.MarketOrderVO;

/**
 * 市集订单服务接口
 */
public interface MarketOrderService {

    /**
     * 创建订单
     */
    Long createOrder(MarketOrderCreateDTO dto);

    /**
     * 买家确认收货
     */
    void buyerConfirm(Long orderId);

    /**
     * 卖家确认发货
     */
    void sellerConfirm(Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);

    /**
     * 获取订单详情
     */
    MarketOrderVO getOrderDetail(Long orderId);

    /**
     * 获取用户订单列表（作为买家）
     */
    PageResult<MarketOrderVO> getBuyerOrders(Long userId, int page, int size);

    /**
     * 获取用户订单列表（作为卖家）
     */
    PageResult<MarketOrderVO> getSellerOrders(Long userId, int page, int size);
}
