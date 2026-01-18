package com.campus.wall.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.R;
import com.campus.wall.dto.system.AuthRuleDTO;
import com.campus.wall.service.system.AuthRuleService;
import com.campus.wall.vo.system.AuthRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证规则", description = "认证规则配置接口")
@RestController
@RequestMapping("/api/v1/system/auth-rules")
@RequiredArgsConstructor
public class AuthRuleController {

    private final AuthRuleService authRuleService;

    @Operation(summary = "查询认证规则")
    @SaCheckLogin
    @SaCheckPermission("system:auth-rule:list")
    @GetMapping
    public R<PageResult<AuthRuleVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) String verifyMethod,
            @RequestParam(required = false) Boolean enabled) {
        return R.ok(authRuleService.queryRules(page, size, triggerType, verifyMethod, enabled));
    }

    @Operation(summary = "新增认证规则")
    @SaCheckLogin
    @SaCheckPermission("system:auth-rule:add")
    @PostMapping
    public R<Long> create(@RequestBody @Valid AuthRuleDTO dto) {
        return R.ok(authRuleService.createRule(dto));
    }

    @Operation(summary = "更新认证规则")
    @SaCheckLogin
    @SaCheckPermission("system:auth-rule:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AuthRuleDTO dto) {
        authRuleService.updateRule(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除认证规则")
    @SaCheckLogin
    @SaCheckPermission("system:auth-rule:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        authRuleService.deleteRule(id);
        return R.ok();
    }
}
