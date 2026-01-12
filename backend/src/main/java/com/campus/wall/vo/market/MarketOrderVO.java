package com.campus.wall.vo.market;

import com.campus.wall.vo.post.PostVO;
import com.campus.wall.vo.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单视图")
public class MarketOrderVO {

    private Long id;
    private BigDecimal price;
    private Integer status;
    private Boolean buyerConfirmed;
    private Boolean sellerConfirmed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private PostVO post;
    private UserVO seller;
    private UserVO buyer;
}
