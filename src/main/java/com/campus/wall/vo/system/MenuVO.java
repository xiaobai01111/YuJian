package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "菜单视图（树形结构）")
public class MenuVO {

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private String groupCode;
    private Integer type;
    private Boolean visible;
    private Integer status;
    private Boolean isFrame;
    private Boolean isCache;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    @Schema(description = "子菜单")
    private List<MenuVO> children;

    @Schema(description = "菜单类型: M=目录, C=菜单, F=按钮")
    public String getMenuType() {
        if (type == null) return "M";
        return switch (type) {
            case 0 -> "M";
            case 1 -> "C";
            case 2 -> "F";
            default -> "M";
        };
    }
}
