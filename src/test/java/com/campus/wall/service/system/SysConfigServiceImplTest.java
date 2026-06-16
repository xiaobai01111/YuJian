package com.campus.wall.service.system;

import com.campus.wall.common.BusinessException;
import com.campus.wall.constant.SysConfigKeys;
import com.campus.wall.entity.system.SysConfig;
import com.campus.wall.mapper.system.SysConfigMapper;
import com.campus.wall.service.system.impl.SysConfigServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;
    @Mock
    private Environment environment;
    @Mock
    private OperLogService operLogService;

    private SysConfigServiceImpl sysConfigService;

    @BeforeEach
    void setUp() {
        sysConfigService = new SysConfigServiceImpl(sysConfigMapper, new ObjectMapper(), environment, operLogService);
    }

    @Test
    void getEmailAllowedDomains_whenMissing_returnsDefaultEduCn() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        assertThat(sysConfigService.getEmailAllowedDomains()).containsExactly("edu.cn");
    }

    @Test
    void getEmailAllowedDomains_whenInvalidJson_returnsDefaultEduCn() {
        when(sysConfigMapper.selectOne(any())).thenReturn(config(SysConfigKeys.EMAIL_ALLOWED_DOMAINS, "not-json"));

        assertThat(sysConfigService.getEmailAllowedDomains()).containsExactly("edu.cn");
    }

    @Test
    void updateEmailAllowedDomains_whenMissing_insertsConfig() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        sysConfigService.updateEmailAllowedDomains(List.of("edu.cn", "school.edu"));

        verify(sysConfigMapper).insert(any(SysConfig.class));
    }

    @Test
    void updateEmailAllowedDomains_whenExists_updatesConfig() {
        SysConfig existing = config(SysConfigKeys.EMAIL_ALLOWED_DOMAINS, "[\"edu.cn\"]");
        existing.setId(1L);
        when(sysConfigMapper.selectOne(any())).thenReturn(existing);

        sysConfigService.updateEmailAllowedDomains(List.of("school.edu"));

        verify(sysConfigMapper).updateById(existing);
        assertThat(existing.getConfigValue()).contains("school.edu");
    }

    @Test
    void getStudentIdWhitelist_whenInvalidJson_returnsEmptyList() {
        when(sysConfigMapper.selectOne(any())).thenReturn(config(SysConfigKeys.STUDENT_ID_WHITELIST, "{x"));

        assertThat(sysConfigService.getStudentIdWhitelist()).isEmpty();
    }

    @Test
    void updateStudentIdWhitelist_whenMissing_insertsConfig() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        sysConfigService.updateStudentIdWhitelist(List.of("20240001", "20240002"));

        verify(sysConfigMapper).insert(any(SysConfig.class));
    }

    @Test
    void isEmailVerificationEnabled_falseConfig_returnsFalse() {
        when(sysConfigMapper.selectOne(any())).thenReturn(config(SysConfigKeys.EMAIL_VERIFICATION_ENABLED, "false"));

        assertThat(sysConfigService.isEmailVerificationEnabled()).isFalse();
    }

    @Test
    void getSmtpConfig_parsesPortAndSslAndMasksPassword() {
        when(sysConfigMapper.selectOne(any())).thenReturn(
            config(SysConfigKeys.SMTP_HOST, "smtp.example.com"),
            config(SysConfigKeys.SMTP_PORT, "587"),
            config(SysConfigKeys.SMTP_USERNAME, "noreply@example.com"),
            config(SysConfigKeys.SMTP_FROM_NAME, "Campus"),
            config(SysConfigKeys.SMTP_SSL, "true")
        );

        Map<String, Object> smtp = sysConfigService.getSmtpConfig();

        assertThat(smtp.get("host")).isEqualTo("smtp.example.com");
        assertThat(smtp.get("port")).isEqualTo(587);
        assertThat(smtp.get("username")).isEqualTo("noreply@example.com");
        assertThat(smtp.get("password")).isEqualTo("");
        assertThat(smtp.get("ssl")).isEqualTo(true);
    }

    @Test
    void updateSmtpConfig_missingPort_throwsBadRequest() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", "smtp.example.com");
        config.put("username", "noreply@example.com");

        assertThatThrownBy(() -> sysConfigService.updateSmtpConfig(config))
            .isInstanceOf(BusinessException.class)
            .hasMessage("SMTP端口必须是有效数字");
    }

    @Test
    void updateSmtpConfig_invalidPortRange_throwsBadRequest() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", "smtp.example.com");
        config.put("username", "noreply@example.com");
        config.put("port", 70000);

        assertThatThrownBy(() -> sysConfigService.updateSmtpConfig(config))
            .isInstanceOf(BusinessException.class)
            .hasMessage("SMTP端口必须在1-65535范围内");
    }

    @Test
    void updateSmtpConfig_success_savesAllKeys() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> config = new HashMap<>();
        config.put("host", "smtp.example.com");
        config.put("port", 465);
        config.put("username", "noreply@example.com");
        config.put("password", "secret");
        config.put("fromName", "Campus Wall");
        config.put("ssl", true);

        sysConfigService.updateSmtpConfig(config);

        verify(sysConfigMapper, atLeast(6)).insert(any(SysConfig.class));
    }

    @Test
    void getEmailTemplates_whenMissing_returnsDefaultTemplates() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> templates = sysConfigService.getEmailTemplates();
        assertThat(templates).containsKey("verification");
        assertThat(templates).containsKey("welcome");
    }

    @Test
    void updateEmailTemplates_empty_throwsBadRequest() {
        assertThatThrownBy(() -> sysConfigService.updateEmailTemplates(Map.of()))
            .isInstanceOf(BusinessException.class)
            .hasMessage("邮件模板不能为空");
    }

    @Test
    void updateEmailTemplates_invalidTemplateFormat_throwsBadRequest() {
        assertThatThrownBy(() -> sysConfigService.updateEmailTemplates(Map.of("verification", "invalid")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("模板格式错误，必须包含subject和body字段");
    }

    @Test
    void updateEmailTemplates_success_persistsConfig() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);
        Map<String, Object> template = new HashMap<>();
        template.put("subject", "S");
        template.put("body", "B");

        sysConfigService.updateEmailTemplates(Map.of("verification", template));

        verify(sysConfigMapper).insert(any(SysConfig.class));
    }

    @Test
    void sendTestEmail_withoutSmtpConfig_throwsBadRequest() {
        when(sysConfigMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> sysConfigService.sendTestEmail("user@edu.cn"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("请先配置SMTP服务器信息");
    }

    @Test
    void sendEmailWithTemplate_templateNotFound_throwsNotFound() {
        when(sysConfigMapper.selectOne(any())).thenReturn(
            config(SysConfigKeys.SMTP_HOST, "smtp.example.com"),
            config(SysConfigKeys.SMTP_PORT, "465"),
            config(SysConfigKeys.SMTP_USERNAME, "noreply@example.com"),
            config(SysConfigKeys.SMTP_PASSWORD, ""),
            config(SysConfigKeys.SMTP_FROM_NAME, "Campus"),
            config(SysConfigKeys.SMTP_SSL, "true"),
            config(SysConfigKeys.EMAIL_TEMPLATES, "{\"other\":{\"subject\":\"x\",\"body\":\"y\"}}")
        );

        assertThatThrownBy(() -> sysConfigService.sendEmailWithTemplate("user@edu.cn", "verification", Map.of("code", "1234")))
            .isInstanceOf(BusinessException.class)
            .hasMessage("邮件模板不存在");
    }

    private SysConfig config(String key, String value) {
        SysConfig config = new SysConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }
}
