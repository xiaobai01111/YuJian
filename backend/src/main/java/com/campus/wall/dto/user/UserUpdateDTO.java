package com.campus.wall.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新用户信息请求")
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称最多50个字符")
    private String nickname;

    private String avatar;

    private String email;
}
