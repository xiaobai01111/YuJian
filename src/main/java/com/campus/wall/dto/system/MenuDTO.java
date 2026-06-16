package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "菜单创建/更新请求")
public class MenuDTO {

    @Schema(description = "父菜单ID")
    private Long parentId = 0L;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "类型：0目录 1菜单 2按钮")
    private Integer type = 1;

    @Schema(description = "是否可见")
    private Boolean visible = true;

    @Schema(description = "排序")
    private Integer sortOrder = 0;
}
