package plugins.sms.services

import groovy.transform.CompileStatic

/**
 * 短信服务核心逻辑
 */
@CompileStatic
class SmsService {
    
    def ctx
    private SmsProvider provider
    
    SmsService(def ctx) {
        this.ctx = ctx
        initProvider()
    }
    
    private void initProvider() {
        def providerType = ctx.config.get("provider", "aliyun") as String
        if (providerType == "tencent") {
            provider = new TencentSmsProvider(ctx)
        } else {
            provider = new AliyunSmsProvider(ctx)
        }
    }
    
    /**
     * 发送验证码
     */
    Map sendCode(String phone, String scene) {
        // 生成验证码
        def codeLength = ctx.config.get("codeLength", 6) as Integer
        def code = generateCode(codeLength)
        
        // 调用短信服务商发送
        def result = provider.send(phone, code)
        
        if (result.success) {
            // 存储验证码到缓存
            def expireMinutes = ctx.config.get("codeExpireMinutes", 5) as Integer
            def cacheKey = "sms:code:${scene}:${phone}"
            ctx.cache.set(cacheKey, [
                code: code,
                attempts: 0,
                createdAt: System.currentTimeMillis()
            ], expireMinutes * 60)
        }
        
        return result
    }
    
    /**
     * 验证验证码
     */
    Map verifyCode(String phone, String code, String scene) {
        def cacheKey = "sms:code:${scene}:${phone}"
        def cached = ctx.cache.get(cacheKey) as Map
        
        if (!cached) {
            return [success: false, code: 400, message: "验证码已过期或不存在"]
        }
        
        def attempts = (cached.attempts ?: 0) as Integer
        if (attempts >= 5) {
            ctx.cache.delete(cacheKey)
            return [success: false, code: 429, message: "验证次数过多，请重新获取"]
        }
        
        if (cached.code != code) {
            // 增加尝试次数
            cached.attempts = attempts + 1
            ctx.cache.set(cacheKey, cached, -1) // 保持原有过期时间
            return [success: false, code: 400, message: "验证码错误"]
        }
        
        // 验证成功，删除缓存
        ctx.cache.delete(cacheKey)
        
        return [success: true, message: "验证成功"]
    }
    
    /**
     * 生成随机验证码
     */
    private String generateCode(int length) {
        def random = new Random()
        def sb = new StringBuilder()
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10))
        }
        return sb.toString()
    }
}

/**
 * 短信服务商接口
 */
interface SmsProvider {
    Map send(String phone, String code)
}
