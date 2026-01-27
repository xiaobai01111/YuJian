package com.campus.wall.service.system;

import java.util.List;
import java.util.Map;

public interface SysConfigService {
    String getConfigValue(String key);
    
    List<String> getEmailAllowedDomains();
    
    void updateEmailAllowedDomains(List<String> domains);
    
    boolean isEmailVerificationEnabled();
    
    Map<String, Object> getSmtpConfig();
    
    void updateSmtpConfig(Map<String, Object> config);
    
    void sendTestEmail(String email);
    
    Map<String, Object> getEmailTemplates();
    
    void updateEmailTemplates(Map<String, Object> templates);
    
    /**
     * 使用模板发送邮件
     * @param to 收件人
     * @param templateName 模板名称 (verification, welcome, resetPassword, notification)
     * @param variables 变量映射 (code, username, email, expireMinutes, content 等)
     */
    void sendEmailWithTemplate(String to, String templateName, Map<String, String> variables);
}
