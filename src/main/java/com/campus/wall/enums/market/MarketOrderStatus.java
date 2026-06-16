package com.campus.wall.enums.market;

import lombok.Getter;

/**
 * 市集订单状态枚举
 */
@Getter
public enum MarketOrderStatus {

    PENDING(0, "待确认"),
    COMPLETED(1, "已完成"),
    CANCELLED(2, "已取消"),
    DISPUTED(3, "纠纷中");

    private final int code;
    private final String name;

    MarketOrderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MarketOrderStatus fromCode(int code) {
        for (MarketOrderStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
