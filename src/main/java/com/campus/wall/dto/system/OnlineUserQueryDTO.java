package com.campus.wall.dto.system;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 在线用户查询参数
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OnlineUserQueryDTO extends PageQueryDTO {

    @Schema(description = "关键词（用户名/昵称/用户ID）")
    private String keyword;

    @Schema(description = "登录IP")
    private String ipaddr;
}
