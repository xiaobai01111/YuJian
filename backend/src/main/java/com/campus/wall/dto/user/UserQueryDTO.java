package com.campus.wall.dto.user;

import com.campus.wall.dto.common.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询请求")
public class UserQueryDTO extends PageQueryDTO {

    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    private Integer verifyStatus;
    private Integer userType;
    private Long deptId;
    private Long roleId;
    private String loginDateStart;
    private String loginDateEnd;
    private Long lastId;
    private String lastDeletedAt;
}
