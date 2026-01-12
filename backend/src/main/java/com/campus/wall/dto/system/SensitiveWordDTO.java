package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "敏感词请求")
public class SensitiveWordDTO {

    @NotBlank(message = "敏感词不能为空")
    private String word;

    private Integer level = 2;
}
