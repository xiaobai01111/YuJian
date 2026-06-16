package com.campus.wall.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "认证规则视图")
public class AuthRuleVO {

    private Long id;

    private String name;

    private Boolean enabled;

    private String triggerType;

    private String verifyMethod;

    private String matchType;

    private String matchValue;

    private List<Long> roleIds;

    private List<String> roleNames;

    private Integer priority;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
