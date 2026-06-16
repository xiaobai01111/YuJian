package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "部门删除请求")
public class DeptDeleteDTO {

    @Schema(description = "用户处理策略: TRANSFER_PARENT=转移到上级部门, UNASSIGN=转移到未分配, DELETE=删除用户")
    private UserHandleStrategy userStrategy = UserHandleStrategy.UNASSIGN;

    @Schema(description = "删除原因")
    private String reason;

    public enum UserHandleStrategy {
        TRANSFER_PARENT,  // 转移到上级部门
        UNASSIGN,         // 转移到未分配
        DELETE            // 删除用户
    }
}
