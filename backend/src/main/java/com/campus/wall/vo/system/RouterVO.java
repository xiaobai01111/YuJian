package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "路由视图")
public class RouterVO {

    @Schema(description = "路由名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "路由元信息")
    private MetaVO meta;

    @Schema(description = "子路由")
    private List<RouterVO> children;

    @Data
    public static class MetaVO {
        @Schema(description = "菜单标题")
        private String title;

        @Schema(description = "菜单图标")
        private String icon;

        @Schema(description = "是否隐藏")
        private Boolean hidden;

        @Schema(description = "权限标识")
        private String perms;

        @Schema(description = "是否缓存")
        private Boolean keepAlive;

        @Schema(description = "菜单分组编码（用于前端分组）")
        private String groupCode;

        @Schema(description = "功能编码（用于前后端功能对齐）")
        private String featureCode;

        @Schema(description = "路由类型：CATALOG/MENU/BUTTON")
        private String routeType;
    }
}
