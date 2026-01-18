package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色创建/更新请求")
public class RoleDTO {

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Schema(description = "角色标识")
    private String roleKey;

    @Schema(description = "状态：0正常 1停用")
    private Integer status = 0;

    @Schema(description = "排序")
    private Integer sortOrder = 0;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

    @Schema(description = "数据权限部门ID列表")
    private List<Long> deptIds;
}
