# 代码审计报告

**项目**: CampusWall 校园墙  
**审计时间**: 2026-01-26  
**审计范围**: backend 全量代码

---

## 一、总体评估

| 维度 | 评分 | 状态 |
|------|------|------|
| 安全性 | ⭐⭐⭐⭐☆ | 良好 |
| 并发安全 | ⭐⭐⭐⭐☆ | 良好 |
| 性能 | ⭐⭐⭐⭐☆ | 良好 |
| 资源管理 | ⭐⭐⭐⭐⭐ | 优秀 |
| 功能正确性 | ⭐⭐⭐⭐☆ | 良好 |
| 架构设计 | ⭐⭐⭐⭐⭐ | 优秀 |
| 测试覆盖 | ⭐⭐⭐☆☆ | 一般 |

---

## 二、安全性审计 (Security) 🔒

### ✅ 已实现的安全措施

| 安全措施 | 实现情况 | 位置 |
|----------|----------|------|
| SQL 注入防护 | ✅ MyBatis-Plus 参数化查询 | 全部 Mapper |
| XSS 防护 | ✅ Jsoup.clean() 内容过滤 | `NoticeServiceImpl` |
| 路径穿越防护 | ✅ resolveSafePath() 校验 | `LocalStorageProvider:78-84` |
| 密码加密 | ✅ BCrypt | `AuthServiceImpl:123,224` |
| 权限控制 | ✅ 动态 URL-权限映射 | `SaTokenConfig`, `PermissionServiceImpl` |
| 登录限流 | ✅ Redis 限流 | `AuthServiceImpl:107-112` |
| 文件类型校验 | ✅ Magic bytes 验证 | `FileServiceImpl:308-322` |
| CORS 配置 | ✅ 可配置，通配符警告 | `SaTokenConfig:80-107` |
| 敏感词过滤 | ✅ DFA 算法 | `SensitiveWordServiceImpl` |
| 初始化锁定 | ✅ 完成后锁定 setup 接口 | `SetupServiceImpl:248-264` |

### ⚠️ 需关注的问题

#### 1. [中] 开发环境验证码控制台输出
```java
// AuthServiceImpl:258-260
if ("dev".equals(activeProfile)) {
    System.out.println("[DEV] 邮箱验证码: " + code + " -> " + eduEmail);
}
```
**风险**: 生产环境误配置可能泄露验证码  
**建议**: 使用 `log.debug()` 替代，并确保生产环境日志级别正确

#### 2. [低] ServerMonitor 暴露系统信息
```java
// ServerMonitorController
javaInfo.setJavaHome(System.getProperty("java.home"));
javaInfo.setProjectDir(System.getProperty("user.dir"));
javaInfo.setInputArgs(String.join(" ", runtimeBean.getInputArguments()));
```
**风险**: 可能泄露服务器敏感路径信息  
**建议**: 仅对管理员暴露，已通过权限控制保护

#### 3. [低] 无命令注入风险
ServerMonitorController 未使用 `Runtime.exec()` 执行外部命令，仅读取 `/proc/meminfo`，安全。

---

## 三、并发与线程安全 (Concurrency) 🔄

### ✅ 正确实现

| 组件 | 实现方式 | 位置 |
|------|----------|------|
| 敏感词缓存 | `volatile` + `ConcurrentHashMap` | `SensitiveWordServiceImpl:30` |
| 权限缓存 | `ConcurrentHashMap` + Caffeine | `PermissionServiceImpl:37-41` |
| 公告缓存 | `volatile` 双重检查 | `NoticeServiceImpl:47-49` |
| 匿名映射 | `ConcurrentHashMap` | `AnonymousMappingServiceImpl` |
| 线程池 | 可配置参数 + CallerRunsPolicy | `AsyncConfig` |
| 初始化锁 | PostgreSQL 事务锁 | `SetupServiceImpl:244-246` |

### ⚠️ 需关注的问题

#### 1. [中] 公告缓存非原子更新
```java
// NoticeServiceImpl:66-67
publicNoticesCache = result;
publicNoticesCacheTime = now;
```
**风险**: 两行赋值非原子操作，极端情况下可能读到不一致状态  
**建议**: 使用封装类或 `AtomicReference` 原子更新

---

## 四、性能审计 (Performance) ⚡

### ✅ 优化措施

| 优化项 | 实现方式 | 位置 |
|--------|----------|------|
| 权限缓存 | Caffeine 本地缓存 | `PermissionServiceImpl` |
| 公告缓存 | 60秒内存缓存 | `NoticeServiceImpl:58-60` |
| 批量查询 | `selectBatchIds()` | `NoticeServiceImpl:414` |
| 分页查询 | MyBatis-Plus 分页 | 全部列表接口 |
| 敏感词匹配 | DFA O(n) 算法 | `SensitiveWordServiceImpl` |

### ⚠️ 潜在问题

#### 1. [中] 权限模式匹配遍历
```java
// PermissionServiceImpl:74-84
for (SysApiPermission permission : permissions) {
    if (pathMatcher.match(permission.getUrl(), url)) {
        return permission;
    }
}
```
**影响**: 大量模式规则时 O(n) 遍历  
**建议**: 已排序优先匹配，实际规则数量有限，可接受

#### 2. [低] 敏感词替换 toString() 调用
```java
// SensitiveWordServiceImpl:74
int length = checkSensitiveWord(result.toString(), i);
```
**影响**: 每次循环创建新字符串  
**建议**: 直接操作 `StringBuilder` 或 `CharSequence`

---

## 五、资源管理 (Resource Management) 📦

### ✅ 正确实现

| 资源类型 | 管理方式 | 位置 |
|----------|----------|------|
| 文件流 | try-with-resources | `LocalStorageProvider:35-37` |
| 数据库连接 | HikariCP 连接池 | Spring Boot 自动管理 |
| Redis 连接 | commons-pool2 | `build.gradle.kts:64` |
| 线程池 | `setWaitForTasksToCompleteOnShutdown(true)` | `AsyncConfig:38` |

### ✅ 无资源泄露风险
所有文件操作均使用 try-with-resources 自动关闭。

---

## 六、功能正确性 (Correctness) ✔️

### ✅ 边界条件处理

| 检查项 | 实现情况 | 位置 |
|--------|----------|------|
| 空值校验 | ✅ 全面 | 各 Service 层 |
| 分页边界 | ✅ `normalizePage()`, `normalizeSize()` | `NoticeServiceImpl:461-468` |
| 文件大小限制 | ✅ 5MB/200MB | `FileServiceImpl:57-58` |
| 密码确认 | ✅ 两次输入一致性校验 | `SetupServiceImpl:92-94` |
| 权限降级 | ✅ 部门停用则无权限 | `StpInterfaceImpl:53-60` |

### ⚠️ 需关注的问题

#### 1. [低] 空权限返回 true
```java
// PermissionServiceImpl:99-101
if (permission == null || permission.isEmpty()) {
    return true;
}
```
**说明**: 空权限标识视为无需权限，符合设计意图

---

## 七、架构设计 (Architecture) 🏗️

### ✅ 优秀实践

| 设计原则 | 实现情况 |
|----------|----------|
| 分层架构 | Controller → Service → Mapper |
| 依赖注入 | 构造器注入 (`@RequiredArgsConstructor`) |
| 接口隔离 | Service 接口与实现分离 |
| 配置外部化 | `application.yml` + `@ConfigurationProperties` |
| 策略模式 | `StorageProvider` 多存储支持 |
| 动态权限 | URL-权限数据库映射 |

### 项目结构
```
com.campus.wall/
├── annotation/     # 自定义注解
├── aspect/         # AOP 切面
├── common/         # 通用类 (R, BusinessException)
├── config/         # 配置类
├── constant/       # 常量定义
├── controller/     # 控制器 (34个)
├── dto/            # 数据传输对象 (55个)
├── entity/         # 实体类 (28个)
├── enums/          # 枚举 (23个)
├── job/            # 定时任务
├── mapper/         # MyBatis Mapper (33个)
├── service/        # 服务层 (64个)
├── util/           # 工具类
└── vo/             # 视图对象 (32个)
```

---

## 八、测试覆盖 (Testing) 🧪

### 当前状态

| 测试类型 | 数量 | 覆盖情况 |
|----------|------|----------|
| 单元测试 | 10+ | 核心逻辑 |
| 集成测试 | 3 | Auth, Dept, Permission |
| 属性测试 | 8 | jqwik 属性测试 |

### ⚠️ 需改进

1. **集成测试覆盖不足** - 仅 3 个集成测试类
2. **缺少 API 端到端测试**
3. **建议添加**:
   - 文件上传/下载测试
   - 权限边界测试
   - 并发场景测试

---

## 九、依赖安全 (Dependencies) 📚

### 依赖版本检查

| 依赖 | 版本 | 状态 |
|------|------|------|
| Spring Boot | 3.4.1 | ✅ 最新稳定版 |
| Sa-Token | 1.38.0 | ✅ 最新版 |
| MyBatis-Plus | 3.5.5 | ✅ 稳定版 |
| PostgreSQL Driver | 最新 | ✅ Spring 管理 |
| Hutool | 5.8.26 | ✅ 最新版 |
| MinIO | 8.5.7 | ✅ 稳定版 |
| Jsoup | 1.18.1 | ✅ 最新版 |
| POI | 5.2.5 | ✅ 稳定版 |
| Caffeine | 3.1.8 | ✅ 稳定版 |

**建议**: 定期使用 `./gradlew dependencyUpdates` 或 Dependabot 检查更新

---

## 十、修复建议汇总

### 高优先级 🔴
*无*

### 中优先级 🟡

| 问题 | 建议 | 文件 |
|------|------|------|
| 公告缓存非原子更新 | 使用 `AtomicReference` | `NoticeServiceImpl` |
| 验证码控制台输出 | 改用 `log.debug()` | `AuthServiceImpl:258-260` |

### 低优先级 🟢

| 问题 | 建议 | 文件 |
|------|------|------|
| 敏感词替换性能 | 优化 StringBuilder 操作 | `SensitiveWordServiceImpl:74` |
| 测试覆盖不足 | 增加集成测试 | `src/test/` |

---

## 十一、合规性检查 (Compliance)

| 检查项 | 状态 |
|--------|------|
| OWASP Top 10 | ✅ 主要风险已防护 |
| 操作日志 | ✅ `OperLogService` 记录 |
| 登录日志 | ✅ `LoginLog` 表记录 |
| 数据脱敏 | ⚠️ 部分实现 (密码哈希) |
| 审计追踪 | ✅ 操作人、时间记录 |

---

## 十二、结论

**整体评价**: 项目代码质量良好，安全防护措施完善，架构设计合理。主要需要加强测试覆盖和部分并发场景的原子性处理。

**推荐工具**:
- SonarQube - 持续代码质量检测
- Dependabot - 依赖漏洞监控
- JaCoCo - 测试覆盖率报告 (已集成)

---

*审计完成*
