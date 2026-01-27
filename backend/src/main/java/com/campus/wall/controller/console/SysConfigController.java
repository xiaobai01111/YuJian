package com.campus.wall.controller.console;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.campus.wall.common.R;
import com.campus.wall.service.system.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统配置", description = "系统配置管理接口")
@RestController
@RequestMapping("/api/v1/console/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取允许的邮箱域名")
    @GetMapping("/email-domains")
    @SaCheckPermission("system:config:list")
    public R<List<String>> getEmailDomains() {
        return R.ok(sysConfigService.getEmailAllowedDomains());
    }

    @Operation(summary = "更新允许的邮箱域名")
    @PutMapping("/email-domains")
    @SaCheckPermission("system:config:edit")
    public R<Void> updateEmailDomains(@RequestBody List<String> domains) {
        sysConfigService.updateEmailAllowedDomains(domains);
        return R.ok();
    }

    @Operation(summary = "获取SMTP配置")
    @GetMapping("/smtp")
    @SaCheckPermission("system:config:list")
    public R<Map<String, Object>> getSmtpConfig() {
        return R.ok(sysConfigService.getSmtpConfig());
    }

    @Operation(summary = "更新SMTP配置")
    @PutMapping("/smtp")
    @SaCheckPermission("system:config:edit")
    public R<Void> updateSmtpConfig(@RequestBody Map<String, Object> config) {
        sysConfigService.updateSmtpConfig(config);
        return R.ok();
    }

    @Operation(summary = "发送测试邮件")
    @PostMapping("/smtp/test")
    @SaCheckPermission("system:config:edit")
    public R<Void> sendTestEmail(@RequestBody Map<String, String> body) {
        sysConfigService.sendTestEmail(body.get("email"));
        return R.ok();
    }

    @Operation(summary = "获取邮件模板")
    @GetMapping("/email-templates")
    @SaCheckPermission("system:config:list")
    public R<Map<String, Object>> getEmailTemplates() {
        return R.ok(sysConfigService.getEmailTemplates());
    }

    @Operation(summary = "更新邮件模板")
    @PutMapping("/email-templates")
    @SaCheckPermission("system:config:edit")
    public R<Void> updateEmailTemplates(@RequestBody Map<String, Object> templates) {
        sysConfigService.updateEmailTemplates(templates);
        return R.ok();
    }
}
