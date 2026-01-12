package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户编辑DTO
 */
@Data
@Schema(description = "用户编辑请求")
public class UserEditDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长50字符")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "用户类型：0=普通用户, 1=管理员")
    private Integer userType;

    @Schema(description = "性别：0=未知, 1=男, 2=女")
    private Integer sex;

    @Schema(description = "状态：0=正常, 1=停用")
    private Integer status;
}
