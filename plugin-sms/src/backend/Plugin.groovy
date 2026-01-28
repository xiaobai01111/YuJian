package plugins.sms

import groovy.transform.CompileStatic

/**
 * 短信验证服务插件入口
 */
@CompileStatic
class Plugin {
    
    def ctx
    
    void onLoad() {
        ctx.log.info("短信验证服务插件已加载")
    }
    
    void onUnload() {
        ctx.log.info("短信验证服务插件已卸载")
    }
    
    void onEnable() {
        ctx.log.info("短信验证服务插件已启用")
    }
    
    void onDisable() {
        ctx.log.info("短信验证服务插件已禁用")
    }
}
