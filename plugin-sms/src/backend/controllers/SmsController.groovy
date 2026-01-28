package plugins.sms.controllers

import groovy.transform.CompileStatic
import plugins.sms.services.SmsService
import plugins.sms.services.AliyunSmsProvider
import plugins.sms.services.TencentSmsProvider

/**
 * 短信服务 API 控制器
 */
@CompileStatic
class SmsController {
    
    def ctx
    private SmsService smsService
    
    void init() {
        smsService = new SmsService(ctx)
    }
    
    /**
     * POST /api/plugins/sms/send
     * 发送短信验证码
     */
    def send(request) {
        def phone = request.body?.phone as String
        def scene = request.body?.scene as String ?: 'login'
        def clientIp = request.headers?.get('X-Real-IP') ?: request.remoteAddr
        
        if (!phone || !phone.matches(/^1[3-9]\d{9}$/)) {
            return [success: false, code: 400, message: "手机号格式不正确"]
        }
        
        // 检查发送频率限制
        def cooldownKey = "sms:cooldown:${phone}"
        if (ctx.cache.get(cooldownKey)) {
            return [success: false, code: 429, message: "发送过于频繁，请稍后再试"]
        }
        
        // 检查每日发送限制
        def dailyKey = "sms:daily:${phone}"
        def dailyCount = (ctx.cache.get(dailyKey) ?: 0) as Integer
        def dailyLimit = ctx.config.get("dailyLimitPerPhone", 10) as Integer
        if (dailyCount >= dailyLimit) {
            return [success: false, code: 429, message: "今日发送次数已达上限"]
        }
        
        // 检查IP每日限制
        def ipDailyKey = "sms:ip:daily:${clientIp}"
        def ipDailyCount = (ctx.cache.get(ipDailyKey) ?: 0) as Integer
        def ipDailyLimit = ctx.config.get("ipDailyLimit", 50) as Integer
        if (ipDailyCount >= ipDailyLimit) {
            return [success: false, code: 429, message: "当前网络发送次数已达上限"]
        }
        
        try {
            def result = smsService.sendCode(phone, scene)
            
            if (result.success) {
                // 设置60秒冷却
                ctx.cache.set(cooldownKey, "1", 60)
                // 增加每日计数 (24小时过期)
                ctx.cache.set(dailyKey, dailyCount + 1, 86400)
                ctx.cache.set(ipDailyKey, ipDailyCount + 1, 86400)
                
                // 记录日志
                ctx.log.info("短信发送成功: phone=${maskPhone(phone)}, scene=${scene}")
                
                return [success: true, message: "验证码已发送"]
            } else {
                ctx.log.warn("短信发送失败: phone=${maskPhone(phone)}, error=${result.message}")
                return [success: false, code: 500, message: result.message ?: "发送失败"]
            }
        } catch (Exception e) {
            ctx.log.error("短信发送异常: ${e.message}", e)
            return [success: false, code: 500, message: "服务异常，请稍后重试"]
        }
    }
    
    /**
     * POST /api/plugins/sms/verify
     * 验证短信验证码
     */
    def verify(request) {
        def phone = request.body?.phone as String
        def code = request.body?.code as String
        def scene = request.body?.scene as String ?: 'login'
        
        if (!phone || !code) {
            return [success: false, code: 400, message: "参数不完整"]
        }
        
        try {
            def result = smsService.verifyCode(phone, code, scene)
            return result
        } catch (Exception e) {
            ctx.log.error("验证码校验异常: ${e.message}", e)
            return [success: false, code: 500, message: "服务异常"]
        }
    }
    
    /**
     * GET /api/plugins/sms/config
     * 获取短信配置（脱敏）
     */
    def getConfig(request) {
        def provider = ctx.config.get("provider", "aliyun")
        
        def config = [
            provider: provider,
            codeLength: ctx.config.get("codeLength", 6),
            codeExpireMinutes: ctx.config.get("codeExpireMinutes", 5),
            dailyLimitPerPhone: ctx.config.get("dailyLimitPerPhone", 10),
            ipDailyLimit: ctx.config.get("ipDailyLimit", 50)
        ]
        
        if (provider == "aliyun") {
            config.aliyun = [
                accessKeyId: maskSecret(ctx.config.get("aliyun.accessKeyId", "") as String),
                signName: ctx.config.get("aliyun.signName", ""),
                templateCode: ctx.config.get("aliyun.templateCode", "")
            ]
        } else if (provider == "tencent") {
            config.tencent = [
                secretId: maskSecret(ctx.config.get("tencent.secretId", "") as String),
                appId: ctx.config.get("tencent.appId", ""),
                signName: ctx.config.get("tencent.signName", ""),
                templateId: ctx.config.get("tencent.templateId", "")
            ]
        }
        
        return [success: true, data: config]
    }
    
    /**
     * PUT /api/plugins/sms/config
     * 更新短信配置
     */
    def updateConfig(request) {
        def body = request.body as Map
        
        if (!body) {
            return [success: false, code: 400, message: "参数不能为空"]
        }
        
        try {
            // 保存通用配置
            if (body.containsKey("provider")) {
                ctx.config.set("provider", body.provider)
            }
            if (body.containsKey("codeLength")) {
                ctx.config.set("codeLength", body.codeLength)
            }
            if (body.containsKey("codeExpireMinutes")) {
                ctx.config.set("codeExpireMinutes", body.codeExpireMinutes)
            }
            if (body.containsKey("dailyLimitPerPhone")) {
                ctx.config.set("dailyLimitPerPhone", body.dailyLimitPerPhone)
            }
            if (body.containsKey("ipDailyLimit")) {
                ctx.config.set("ipDailyLimit", body.ipDailyLimit)
            }
            
            // 保存阿里云配置
            def aliyun = body.aliyun as Map
            if (aliyun) {
                if (aliyun.accessKeyId && !aliyun.accessKeyId.toString().contains("*")) {
                    ctx.config.set("aliyun.accessKeyId", aliyun.accessKeyId, true)
                }
                if (aliyun.accessKeySecret && !aliyun.accessKeySecret.toString().contains("*")) {
                    ctx.config.set("aliyun.accessKeySecret", aliyun.accessKeySecret, true)
                }
                if (aliyun.containsKey("signName")) {
                    ctx.config.set("aliyun.signName", aliyun.signName)
                }
                if (aliyun.containsKey("templateCode")) {
                    ctx.config.set("aliyun.templateCode", aliyun.templateCode)
                }
            }
            
            // 保存腾讯云配置
            def tencent = body.tencent as Map
            if (tencent) {
                if (tencent.secretId && !tencent.secretId.toString().contains("*")) {
                    ctx.config.set("tencent.secretId", tencent.secretId, true)
                }
                if (tencent.secretKey && !tencent.secretKey.toString().contains("*")) {
                    ctx.config.set("tencent.secretKey", tencent.secretKey, true)
                }
                if (tencent.containsKey("appId")) {
                    ctx.config.set("tencent.appId", tencent.appId)
                }
                if (tencent.containsKey("signName")) {
                    ctx.config.set("tencent.signName", tencent.signName)
                }
                if (tencent.containsKey("templateId")) {
                    ctx.config.set("tencent.templateId", tencent.templateId)
                }
            }
            
            ctx.log.info("短信配置已更新")
            return [success: true, message: "配置已保存"]
        } catch (Exception e) {
            ctx.log.error("保存配置失败: ${e.message}", e)
            return [success: false, code: 500, message: "保存失败"]
        }
    }
    
    /**
     * POST /api/plugins/sms/test
     * 测试短信发送
     */
    def test(request) {
        def phone = request.body?.phone as String
        
        if (!phone || !phone.matches(/^1[3-9]\d{9}$/)) {
            return [success: false, code: 400, message: "手机号格式不正确"]
        }
        
        try {
            def result = smsService.sendCode(phone, 'test')
            if (result.success) {
                return [success: true, message: "测试短信已发送"]
            } else {
                return [success: false, code: 500, message: result.message ?: "发送失败"]
            }
        } catch (Exception e) {
            ctx.log.error("测试发送失败: ${e.message}", e)
            return [success: false, code: 500, message: "发送失败: ${e.message}"]
        }
    }
    
    /**
     * GET /api/plugins/sms/logs
     * 获取发送日志
     */
    def getLogs(request) {
        def page = (request.params?.page ?: 1) as Integer
        def size = (request.params?.size ?: 20) as Integer
        
        // 从插件日志表查询
        def logs = ctx.db.query("""
            SELECT * FROM sys_plugin_log 
            WHERE plugin_id = 'sms-verification' 
            ORDER BY created_at DESC 
            LIMIT ? OFFSET ?
        """, [size, (page - 1) * size])
        
        def total = ctx.db.queryOne("""
            SELECT COUNT(*) as count FROM sys_plugin_log 
            WHERE plugin_id = 'sms-verification'
        """)?.count ?: 0
        
        return [
            success: true,
            data: [
                records: logs,
                total: total,
                page: page,
                size: size
            ]
        ]
    }
    
    // 脱敏手机号
    private String maskPhone(String phone) {
        if (!phone || phone.length() < 7) return phone
        return phone.substring(0, 3) + "****" + phone.substring(7)
    }
    
    // 脱敏密钥
    private String maskSecret(String secret) {
        if (!secret || secret.length() < 8) return secret ? "****" : ""
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4)
    }
}
