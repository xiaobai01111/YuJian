package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
    private Integer type;
    private Boolean visible;
    private Integer sortOrder;

    @Schema(description = "子菜单")
    private List<MenuVO> children;
}
