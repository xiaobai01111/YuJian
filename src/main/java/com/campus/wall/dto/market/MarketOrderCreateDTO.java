package com.campus.wall.dto.market;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "创建订单请求")
public class MarketOrderCreateDTO {

    @NotNull(message = "商品帖子ID不能为空")
    private Long postId;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;
}
