package com.campus.wall.enums.post;

import lombok.Getter;

/**
 * 板块类型枚举
 */
@Getter
public enum BoardType {

    CONFESSION("confession", "表白墙"),
    TREEHOLE("treehole", "树洞"),
    HELP("help", "求助"),
    MARKET("market", "市集"),
    LOST("lost", "失物招领"),
    FRESHMAN("freshman", "新生咨询");

    private final String code;
    private final String name;

    BoardType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BoardType fromCode(String code) {
        for (BoardType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public boolean isForceAnonymous() {
        return this == TREEHOLE;
    }

    public boolean requiresVerification() {
        return this != FRESHMAN;
    }
}
