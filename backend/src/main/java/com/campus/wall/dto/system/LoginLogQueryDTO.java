package com.campus.wall.dto.system;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginLogQueryDTO extends PageQueryDTO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录IP")
    private String ipaddr;

    @Schema(description = "状态：0成功 1失败")
    private Integer status;

    @Schema(description = "登录时间起")
    private String loginTimeStart;

    @Schema(description = "登录时间止")
    private String loginTimeEnd;
}
