package plugins.myplugin.controllers

import groovy.transform.CompileStatic

/**
 * 插件API控制器
 * 
 * 路由前缀: /api/plugins/my-plugin
 */
@CompileStatic
class MainController {
    
    def ctx  // 插件上下文
    
    /**
     * GET /api/plugins/my-plugin/info
     * 获取插件信息
     */
    def getInfo(request) {
        return [
            success: true,
            data: [
                name: "示例插件",
                version: "1.0.0",
                config: ctx.config.getAll()
            ]
        ]
    }
    
    /**
     * POST /api/plugins/my-plugin/process
     * 处理数据
     */
    def process(request) {
        def input = request.body?.input as String
        
        if (!input || input.trim().isEmpty()) {
            return [success: false, message: "输入不能为空"]
        }
        
        // 示例: 使用配置
        def maxItems = ctx.config.get("maxItems", 100) as Integer
        
        // 示例: 记录日志
        ctx.log.info("处理输入: ${input}")
        
        // 示例: 使用缓存
        def cacheKey = "process:${input.hashCode()}"
        def cached = ctx.cache.get(cacheKey)
        if (cached) {
            return [success: true, data: cached, cached: true]
        }
        
        // 处理逻辑
        def result = [
            input: input,
            processed: input.toUpperCase(),
            length: input.length(),
            timestamp: System.currentTimeMillis()
        ]
        
        // 缓存结果 (60秒)
        ctx.cache.set(cacheKey, result, 60)
        
        return [success: true, data: result]
    }
    
    /**
     * GET /api/plugins/my-plugin/data
     * 查询数据示例
     */
    def getData(request) {
        def page = (request.params?.page ?: 1) as Integer
        def size = (request.params?.size ?: 10) as Integer
        
        // 示例: 数据库查询 (仅限插件专属表)
        // def records = ctx.db.query("SELECT * FROM plugin_my_data LIMIT ? OFFSET ?", [size, (page - 1) * size])
        
        return [
            success: true,
            data: [
                records: [],
                total: 0,
                page: page,
                size: size
            ]
        ]
    }
}
