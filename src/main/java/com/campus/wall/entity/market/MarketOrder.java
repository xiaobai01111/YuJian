package com.campus.wall.entity.market;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 市集订单实体
 */
@Data
@TableName("market_orders")
public class MarketOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long sellerId;

    private Long buyerId;

    private BigDecimal price;

    private Integer status;

    private Boolean buyerConfirmed;

    private Boolean sellerConfirmed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
