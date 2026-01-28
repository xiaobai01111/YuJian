# Campus Wall 插件开发模板

## 快速开始

```bash
# 安装依赖
pnpm install

# 开发模式（前端热更新）
pnpm run dev

# 构建并打包
pnpm run build
```

构建后的插件包位于 `release/` 目录，格式为 `{plugin-id}-{version}.zip`。

## 目录结构

```
plugin-template/
├── manifest.json              # 插件清单（必须）
├── package.json               # Node.js 项目配置
├── vite.config.ts             # Vite 构建配置
├── src/
│   ├── frontend/              # 前端代码
│   │   ├── index.ts           # 前端入口
│   │   └── views/             # Vue 组件
│   └── backend/               # 后端代码
│       ├── Plugin.groovy      # 插件入口类
│       └── controllers/       # API 控制器
├── migrations/                # 数据库迁移（可选）
│   └── V1__init.sql
└── release/                   # 打包输出
    └── my-plugin-1.0.0.zip
```

## manifest.json 说明

| 字段 | 类型 | 必须 | 说明 |
|------|------|------|------|
| `id` | string | ✅ | 插件唯一标识，建议格式 `kebab-case` |
| `name` | string | ✅ | 插件显示名称 |
| `version` | string | ✅ | 版本号，建议遵循 semver |
| `description` | string | | 插件描述 |
| `author` | string | | 作者 |
| `permissions` | array | | 权限声明 |
| `menus` | array | | 菜单定义 |
| `apis` | array | | API 路由定义 |
| `config` | object | | 配置项 schema |

### 权限列表

- `database:read` - 读取数据库
- `database:write` - 写入数据库
- `api:register` - 注册 API 路由
- `menu:register` - 注册菜单
- `config:read` - 读取配置
- `config:write` - 写入配置
- `http:request` - 发起 HTTP 请求
- `cache:use` - 使用缓存

## 后端开发

### 插件上下文 (ctx)

```groovy
// 数据库操作
ctx.db.query("SELECT * FROM my_table WHERE id = ?", [1])
ctx.db.execute("INSERT INTO my_table (name) VALUES (?)", ["test"])

// HTTP 客户端
ctx.http.get("https://api.example.com/data")
ctx.http.post("https://api.example.com/data", [key: "value"])

// 缓存
ctx.cache.get("key")
ctx.cache.set("key", value, 60)  // 60秒过期
ctx.cache.delete("key")

// 配置
ctx.config.get("apiKey")
ctx.config.get("maxItems", 100)  // 带默认值

// 日志
ctx.log.info("信息")
ctx.log.warn("警告")
ctx.log.error("错误", exception)
```

### 控制器

```groovy
class MyController {
    def ctx
    
    // GET /api/plugins/{plugin-id}/hello
    def hello(request) {
        return [success: true, message: "Hello!"]
    }
    
    // POST /api/plugins/{plugin-id}/process
    def process(request) {
        def data = request.body
        return [success: true, data: data]
    }
}
```

## 前端开发

### 插件导出接口

```typescript
export interface PluginExports {
  // 安装时调用
  install: (ctx: PluginContext) => void
  
  // 卸载时调用（可选）
  uninstall?: () => void
  
  // 路由定义
  routes?: Array<{
    path: string
    component: any
  }>
  
  // 组件导出
  components?: Record<string, any>
}
```

### 插件上下文

```typescript
interface PluginContext {
  app: App                    // Vue App 实例
  router: Router              // Vue Router
  api: {                      // API 客户端
    get, post, put, delete
  }
  config: Record<string, any> // 插件配置
  notify: {                   // 通知
    success, error, info
  }
}
```

## 数据库迁移

在 `migrations/` 目录下创建 SQL 文件：

```sql
-- migrations/V1__init.sql
CREATE TABLE plugin_my_data (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

命名规则：`V{version}__{description}.sql`

## 安装插件

1. 运行 `pnpm run build` 打包
2. 在管理后台 > 插件管理 > 上传插件
3. 启用插件

## 注意事项

- 后端代码运行在沙箱环境，部分危险操作被禁止
- 数据库操作仅限插件专属 schema
- 请勿在代码中硬编码敏感信息，使用配置系统
