package plugins.myplugin

import groovy.transform.CompileStatic

/**
 * 插件入口类
 * 
 * 可用的上下文对象:
 * - ctx.db        数据库操作 (仅限插件schema)
 * - ctx.http      HTTP客户端
 * - ctx.cache     缓存操作
 * - ctx.config    插件配置
 * - ctx.log       日志记录
 */
@CompileStatic
class Plugin {
    
    def ctx  // 插件上下文，由主程序注入
    
    /**
     * 插件加载时调用
     */
    void onLoad() {
        ctx.log.info("示例插件已加载")
    }
    
    /**
     * 插件卸载时调用
     */
    void onUnload() {
        ctx.log.info("示例插件已卸载")
    }
    
    /**
     * 插件启用时调用
     */
    void onEnable() {
        ctx.log.info("示例插件已启用")
    }
    
    /**
     * 插件禁用时调用
     */
    void onDisable() {
        ctx.log.info("示例插件已禁用")
    }
}
