package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "角色视图")
public class RoleVO {

    private Long id;
    private String roleName;
    private String roleKey;
    private Integer status;
    private Integer sortOrder;
    private List<Long> menuIds;
    private LocalDateTime createdAt;
}
