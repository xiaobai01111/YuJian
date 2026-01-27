package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.entity.system.SysConfig;
import com.campus.wall.mapper.system.SysConfigMapper;
import com.campus.wall.service.system.SysConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.campus.wall.util.CryptoUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;
    
    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:prod}")
    private String activeProfile;
    
    // P2-2: 短TTL缓存，减少DB查询
    private static final long CACHE_TTL_MS = 60_000; // 60秒缓存
    
    private volatile SmtpConfigCache smtpConfigCache;
    private volatile EmailTemplatesCache emailTemplatesCache;
    
    private record SmtpConfigCache(String host, int port, String username, String password, String fromName, boolean ssl, long expireAt) {
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }
    
    private record EmailTemplatesCache(Map<String, Object> templates, long expireAt) {
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }
    
    /**
     * P2-2: 获取缓存的SMTP配置（60秒TTL）
     */
    private SmtpConfigCache getCachedSmtpConfig() {
        SmtpConfigCache cache = smtpConfigCache;
        if (cache != null && !cache.isExpired()) {
            return cache;
        }
        
        // 一次性读取所有SMTP配置
        String host = getConfigValue("smtp.host");
        String portStr = getConfigValue("smtp.port");
        String username = getConfigValue("smtp.username");
        String encryptedPassword = getConfigValue("smtp.password");
        String fromName = getConfigValue("smtp.from_name");
        boolean ssl = "true".equalsIgnoreCase(getConfigValue("smtp.ssl"));
        
        String password = null;
        if (encryptedPassword != null && !encryptedPassword.isBlank()) {
            try {
                password = CryptoUtils.decrypt(encryptedPassword);
            } catch (Exception e) {
                log.warn("Failed to decrypt SMTP password, using raw value", e);
                password = encryptedPassword;
            }
        }
        
        cache = new SmtpConfigCache(
            host, 
            parseIntOrDefault(portStr, 465), 
            username, 
            password, 
            fromName, 
            ssl, 
            System.currentTimeMillis() + CACHE_TTL_MS
        );
        smtpConfigCache = cache;
        return cache;
    }
    
    /**
     * P2-2: 获取缓存的邮件模板（60秒TTL）
     */
    private Map<String, Object> getCachedEmailTemplates() {
        EmailTemplatesCache cache = emailTemplatesCache;
        if (cache != null && !cache.isExpired()) {
            return cache.templates();
        }
        
        Map<String, Object> templates = getEmailTemplates();
        emailTemplatesCache = new EmailTemplatesCache(templates, System.currentTimeMillis() + CACHE_TTL_MS);
        return templates;
    }

    @Override
    public String getConfigValue(String key) {
        SysConfig config = sysConfigMapper.selectOne(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public List<String> getEmailAllowedDomains() {
        String value = getConfigValue("email.allowed_domains");
        if (value == null || value.isBlank()) {
            return List.of("edu.cn");
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse email.allowed_domains: {}", value, e);
            return List.of("edu.cn");
        }
    }

    @Override
    public void updateEmailAllowedDomains(List<String> domains) {
        try {
            String value = objectMapper.writeValueAsString(domains != null ? domains : new ArrayList<>());
            SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, "email.allowed_domains")
            );
            if (config == null) {
                config = new SysConfig();
                config.setConfigKey("email.allowed_domains");
                config.setConfigValue(value);
                config.setConfigType("json");
                config.setRemark("允许的教育邮箱域名后缀");
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                sysConfigMapper.insert(config);
            } else {
                config.setConfigValue(value);
                config.setUpdatedAt(LocalDateTime.now());
                sysConfigMapper.updateById(config);
            }
        } catch (Exception e) {
            log.error("Failed to update email.allowed_domains", e);
            throw new RuntimeException("更新配置失败");
        }
    }

    @Override
    public boolean isEmailVerificationEnabled() {
        String value = getConfigValue("email.verification_enabled");
        return value == null || "true".equalsIgnoreCase(value);
    }

    @Override
    public Map<String, Object> getSmtpConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("host", getConfigValue("smtp.host"));
        config.put("port", parseIntOrDefault(getConfigValue("smtp.port"), 465));
        config.put("username", getConfigValue("smtp.username"));
        config.put("password", ""); // 不返回密码
        config.put("fromName", getConfigValue("smtp.from_name"));
        config.put("ssl", "true".equalsIgnoreCase(getConfigValue("smtp.ssl")));
        return config;
    }

    @Override
    public void updateSmtpConfig(Map<String, Object> config) {
        // P2-1: SMTP配置强校验
        String host = String.valueOf(config.getOrDefault("host", "")).trim();
        String username = String.valueOf(config.getOrDefault("username", "")).trim();
        Object portObj = config.get("port");
        int port;
        try {
            port = portObj instanceof Number ? ((Number) portObj).intValue() : Integer.parseInt(String.valueOf(portObj));
        } catch (Exception e) {
            throw new RuntimeException("SMTP端口必须是有效数字");
        }
        
        if (host.isBlank()) {
            throw new RuntimeException("SMTP服务器地址不能为空");
        }
        if (host.length() > 255) {
            throw new RuntimeException("SMTP服务器地址过长");
        }
        if (username.isBlank()) {
            throw new RuntimeException("SMTP用户名不能为空");
        }
        if (username.length() > 255) {
            throw new RuntimeException("SMTP用户名过长");
        }
        if (port < 1 || port > 65535) {
            throw new RuntimeException("SMTP端口必须在1-65535范围内");
        }
        
        saveConfig("smtp.host", host, "string", "SMTP服务器地址");
        saveConfig("smtp.port", String.valueOf(port), "number", "SMTP端口");
        saveConfig("smtp.username", username, "string", "SMTP用户名");
        String password = String.valueOf(config.getOrDefault("password", ""));
        if (password != null && !password.isBlank()) {
            String encryptedPassword = CryptoUtils.encrypt(password);
            saveConfig("smtp.password", encryptedPassword, "string", "SMTP密码(加密)");
        }
        String fromName = String.valueOf(config.getOrDefault("fromName", "")).trim();
        if (fromName.length() > 100) {
            throw new RuntimeException("发件人名称过长");
        }
        saveConfig("smtp.from_name", fromName, "string", "发件人名称");
        saveConfig("smtp.ssl", String.valueOf(config.getOrDefault("ssl", true)), "boolean", "是否启用SSL");
        
        // 清除SMTP配置缓存
        smtpConfigCache = null;
    }

    @Override
    public void sendTestEmail(String email) {
        log.info("发送测试邮件到: {}", email);
        
        String host = getConfigValue("smtp.host");
        String portStr = getConfigValue("smtp.port");
        String username = getConfigValue("smtp.username");
        String encryptedPassword = getConfigValue("smtp.password");
        String password = null;
        if (encryptedPassword != null && !encryptedPassword.isBlank()) {
            try {
                password = CryptoUtils.decrypt(encryptedPassword);
            } catch (Exception e) {
                log.warn("Failed to decrypt SMTP password, using raw value", e);
                password = encryptedPassword;
            }
        }
        String fromName = getConfigValue("smtp.from_name");
        boolean ssl = "true".equalsIgnoreCase(getConfigValue("smtp.ssl"));
        
        if (host == null || host.isBlank() || username == null || username.isBlank()) {
            throw new RuntimeException("请先配置SMTP服务器信息");
        }
        
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(parseIntOrDefault(portStr, 465));
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            
            int port = parseIntOrDefault(portStr, 465);
            
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", "30000");
            props.put("mail.smtp.connectiontimeout", "30000");
            props.put("mail.smtp.writetimeout", "30000");
            // 仅在开发环境启用debug，避免泄露认证信息
            props.put("mail.debug", "dev".equals(activeProfile) ? "true" : "false");
            
            if (ssl && port == 465) {
                // SSL模式 (端口465)
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.trust", host);
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            } else if (port == 587 || !ssl) {
                // STARTTLS模式 (端口587)
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            } else {
                // 其他SSL端口 - 使用主机名信任，不使用trust=*
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.trust", host);
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName != null && !fromName.isBlank() ? fromName + " <" + username + ">" : username);
            message.setTo(email);
            message.setSubject("校园墙 - 测试邮件");
            message.setText("这是一封测试邮件，用于验证SMTP配置是否正确。\n\n如果您收到此邮件，说明邮箱配置正确。");
            
            mailSender.send(message);
            log.info("测试邮件发送成功: {}", email);
        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }

    private void saveConfig(String key, String value, String type, String remark) {
        SysConfig config = sysConfigMapper.selectOne(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType(type);
            config.setRemark(remark);
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setUpdatedAt(LocalDateTime.now());
            sysConfigMapper.updateById(config);
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Map<String, Object> getEmailTemplates() {
        String value = getConfigValue("email.templates");
        if (value == null || value.isBlank()) {
            return getDefaultTemplates();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse email.templates", e);
            return getDefaultTemplates();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateEmailTemplates(Map<String, Object> templates) {
        // P2-1: 邮件模板强校验
        if (templates == null || templates.isEmpty()) {
            throw new RuntimeException("邮件模板不能为空");
        }
        
        for (Map.Entry<String, Object> entry : templates.entrySet()) {
            String templateName = entry.getKey();
            Object templateObj = entry.getValue();
            
            if (templateObj == null) {
                throw new RuntimeException("模板 [" + templateName + "] 不能为空");
            }
            
            if (!(templateObj instanceof Map)) {
                throw new RuntimeException("模板 [" + templateName + "] 格式错误，必须包含subject和body字段");
            }
            
            Map<String, Object> template = (Map<String, Object>) templateObj;
            Object subjectObj = template.get("subject");
            Object bodyObj = template.get("body");
            
            if (subjectObj == null || !(subjectObj instanceof String) || ((String) subjectObj).isBlank()) {
                throw new RuntimeException("模板 [" + templateName + "] 缺少必填字段: subject");
            }
            if (bodyObj == null || !(bodyObj instanceof String) || ((String) bodyObj).isBlank()) {
                throw new RuntimeException("模板 [" + templateName + "] 缺少必填字段: body");
            }
            
            String subject = (String) subjectObj;
            String body = (String) bodyObj;
            
            if (subject.length() > 200) {
                throw new RuntimeException("模板 [" + templateName + "] 的subject过长（最大200字符）");
            }
            if (body.length() > 10000) {
                throw new RuntimeException("模板 [" + templateName + "] 的body过长（最大10000字符）");
            }
        }
        
        try {
            String value = objectMapper.writeValueAsString(templates);
            saveConfig("email.templates", value, "json", "邮件模板配置");
            // 清除模板缓存
            emailTemplatesCache = null;
        } catch (Exception e) {
            log.error("Failed to update email.templates", e);
            throw new RuntimeException("更新邮件模板失败");
        }
    }

    private Map<String, Object> getDefaultTemplates() {
        Map<String, Object> templates = new HashMap<>();
        
        // 验证码邮件
        Map<String, String> verification = new HashMap<>();
        verification.put("subject", "【校园墙】您的验证码");
        verification.put("body", "您好，\n\n您的验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟，请尽快使用。\n\n如非本人操作，请忽略此邮件。");
        templates.put("verification", verification);
        
        // 注册欢迎邮件
        Map<String, String> welcome = new HashMap<>();
        welcome.put("subject", "【校园墙】欢迎注册");
        welcome.put("body", "亲爱的 {{username}}，\n\n欢迎加入校园墙！\n\n您的账号已注册成功，现在可以开始使用我们的服务了。\n\n祝您使用愉快！");
        templates.put("welcome", welcome);
        
        // 密码重置邮件
        Map<String, String> resetPassword = new HashMap<>();
        resetPassword.put("subject", "【校园墙】密码重置");
        resetPassword.put("body", "您好，\n\n您正在重置密码，验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟。\n\n如非本人操作，请立即修改密码。");
        templates.put("resetPassword", resetPassword);
        
        // 异地登录提醒
        Map<String, String> loginAlert = new HashMap<>();
        loginAlert.put("subject", "【校园墙】异地登录提醒");
        loginAlert.put("body", "亲爱的 {{username}}，\n\n您的账号于 {{loginTime}} 在新设备上登录：\n\nIP地址：{{ip}}\n设备信息：{{device}}\n登录地点：{{location}}\n\n如果这不是您本人的操作，请立即修改密码并检查账号安全。\n\n校园墙安全中心");
        templates.put("loginAlert", loginAlert);
        
        // 密码修改成功通知
        Map<String, String> passwordChanged = new HashMap<>();
        passwordChanged.put("subject", "【校园墙】密码修改成功");
        passwordChanged.put("body", "亲爱的 {{username}}，\n\n您的密码已于 {{changeTime}} 修改成功。\n\n如果这不是您本人的操作，请立即联系客服。\n\n校园墙安全中心");
        templates.put("passwordChanged", passwordChanged);
        
        // 账号安全警告
        Map<String, String> securityAlert = new HashMap<>();
        securityAlert.put("subject", "【校园墙】账号安全警告");
        securityAlert.put("body", "亲爱的 {{username}}，\n\n我们检测到您的账号存在安全风险：\n\n{{alertContent}}\n\n请及时处理以保护您的账号安全。\n\n校园墙安全中心");
        templates.put("securityAlert", securityAlert);
        
        // 邮箱绑定验证
        Map<String, String> bindEmail = new HashMap<>();
        bindEmail.put("subject", "【校园墙】邮箱绑定验证");
        bindEmail.put("body", "您好，\n\n您正在绑定邮箱 {{email}}，验证码是：{{code}}\n\n验证码有效期为 {{expireMinutes}} 分钟。\n\n如非本人操作，请忽略此邮件。");
        templates.put("bindEmail", bindEmail);
        
        // 身份认证通过
        Map<String, String> verifyApproved = new HashMap<>();
        verifyApproved.put("subject", "【校园墙】身份认证已通过");
        verifyApproved.put("body", "亲爱的 {{username}}，\n\n恭喜！您的身份认证已通过审核。\n\n现在您可以享受完整的校园墙服务了。\n\n校园墙团队");
        templates.put("verifyApproved", verifyApproved);
        
        // 身份认证拒绝
        Map<String, String> verifyRejected = new HashMap<>();
        verifyRejected.put("subject", "【校园墙】身份认证未通过");
        verifyRejected.put("body", "亲爱的 {{username}}，\n\n很抱歉，您的身份认证未通过审核。\n\n拒绝原因：{{rejectReason}}\n\n请修改后重新提交认证申请。\n\n校园墙团队");
        templates.put("verifyRejected", verifyRejected);
        
        // 系统通知
        Map<String, String> notification = new HashMap<>();
        notification.put("subject", "【校园墙】系统通知");
        notification.put("body", "亲爱的 {{username}}，\n\n{{content}}\n\n校园墙团队");
        templates.put("notification", notification);
        
        return templates;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sendEmailWithTemplate(String to, String templateName, Map<String, String> variables) {
        log.info("使用模板 {} 发送邮件到: {}", templateName, to);
        
        // P2-2: 使用缓存获取SMTP配置，减少DB查询
        SmtpConfigCache smtp = getCachedSmtpConfig();
        
        if (smtp.host() == null || smtp.host().isBlank() || smtp.username() == null || smtp.username().isBlank()) {
            throw new RuntimeException("请先配置SMTP服务器信息");
        }
        
        // P2-2: 使用缓存获取模板
        Map<String, Object> templates = getCachedEmailTemplates();
        Object templateObj = templates.get(templateName);
        if (templateObj == null) {
            throw new RuntimeException("邮件模板不存在: " + templateName);
        }
        
        Map<String, String> template;
        try {
            template = (Map<String, String>) templateObj;
        } catch (ClassCastException e) {
            throw new RuntimeException("邮件模板格式错误: " + templateName);
        }
        
        String subject = template.get("subject");
        String body = template.get("body");
        
        // P2-1: 运行时模板字段校验
        if (subject == null || subject.isBlank()) {
            throw new RuntimeException("邮件模板 [" + templateName + "] 缺少subject字段");
        }
        if (body == null || body.isBlank()) {
            throw new RuntimeException("邮件模板 [" + templateName + "] 缺少body字段");
        }
        
        // 变量替换
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                subject = subject.replace(placeholder, value);
                body = body.replace(placeholder, value);
            }
        }
        
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(smtp.host());
            mailSender.setPort(smtp.port());
            mailSender.setUsername(smtp.username());
            mailSender.setPassword(smtp.password());
            mailSender.setDefaultEncoding("UTF-8");

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");
            
            if (smtp.ssl()) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", String.valueOf(smtp.port()));
                props.put("mail.smtp.ssl.trust", smtp.host());
                props.put("mail.smtp.ssl.checkserveridentity", "true");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            String fromName = smtp.fromName();
            message.setFrom(fromName != null && !fromName.isBlank() ? fromName + " <" + smtp.username() + ">" : smtp.username());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("邮件发送成功: {} -> {}", templateName, to);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage());
        }
    }
}
