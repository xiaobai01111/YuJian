package com.campus.wall.controller.market;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.market.MarketOrderCreateDTO;
import com.campus.wall.service.market.MarketOrderService;
import com.campus.wall.service.user.CreditService;
import com.campus.wall.vo.market.MarketOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 市集订单控制器
 */
@Tag(name = "市集订单", description = "市集订单管理接口")
@RestController
@RequestMapping("/api/v1/market/orders")
@RequiredArgsConstructor
public class MarketController {

    private final MarketOrderService marketOrderService;
    private final CreditService creditService;

    @Operation(summary = "创建订单")
    @SaCheckLogin
    @PostMapping
    public R<Long> createOrder(@Valid @RequestBody MarketOrderCreateDTO dto) {
        return R.ok(marketOrderService.createOrder(dto));
    }

    @Operation(summary = "获取订单详情")
    @SaCheckLogin
    @GetMapping("/{id}")
    public R<MarketOrderVO> getOrderDetail(@PathVariable Long id) {
        return R.ok(marketOrderService.getOrderDetail(id));
    }

    @Operation(summary = "买家确认收货")
    @SaCheckLogin
    @PutMapping("/{id}/buyer-confirm")
    public R<Void> buyerConfirm(@PathVariable Long id) {
        marketOrderService.buyerConfirm(id);
        return R.ok();
    }

    @Operation(summary = "卖家确认发货")
    @SaCheckLogin
    @PutMapping("/{id}/seller-confirm")
    public R<Void> sellerConfirm(@PathVariable Long id) {
        marketOrderService.sellerConfirm(id);
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @SaCheckLogin
    @PutMapping("/{id}/cancel")
    public R<Void> cancelOrder(@PathVariable Long id) {
        marketOrderService.cancelOrder(id);
        return R.ok();
    }

    @Operation(summary = "获取我的买入订单")
    @SaCheckLogin
    @GetMapping("/buyer")
    public R<PageResult<MarketOrderVO>> getBuyerOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(marketOrderService.getBuyerOrders(userId, page, size));
    }

    @Operation(summary = "获取我的卖出订单")
    @SaCheckLogin
    @GetMapping("/seller")
    public R<PageResult<MarketOrderVO>> getSellerOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(marketOrderService.getSellerOrders(userId, page, size));
    }

    @Operation(summary = "获取我的信用分")
    @SaCheckLogin
    @GetMapping("/credit")
    public R<Integer> getCreditScore() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(creditService.getCreditScore(userId));
    }

    @Operation(summary = "检查是否可以在市集发帖")
    @SaCheckLogin
    @GetMapping("/can-post")
    public R<Boolean> canPostInMarket() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(creditService.canPostInMarket(userId));
    }
}
