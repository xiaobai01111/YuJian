package com.campus.wall.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "认证规则请求")
public class AuthRuleDTO {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private Boolean enabled = true;

    @NotBlank(message = "触发类型不能为空")
    private String triggerType;

    private String verifyMethod;

    private String matchType;

    private String matchValue;

    private List<Long> roleIds;

    private Long deptId;

    private Integer priority;

    private String remark;
}
