package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户批量分配（按条件）")
public class UserBatchAssignDTO extends UserQueryDTO {

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "角色分配模式: REPLACE/ADD")
    private String roleMode;
}
