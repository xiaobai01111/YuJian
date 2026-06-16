package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "用户角色分配请求")
public class UserRoleDTO {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
