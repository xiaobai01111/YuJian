# 校园墙后端服务

基于 Spring Boot 3 + Sa-Token + MyBatis-Plus + PostgreSQL 构建的校园社区互动平台后端服务。

## 技术栈

- **Java 21**
- **Spring Boot 3.4.1**
- **Gradle 8.5** - 构建工具
- **Sa-Token 1.38.0** - 权限认证
- **MyBatis-Plus 3.5.5** - ORM
- **PostgreSQL 15+** - 数据库
- **Redis 7+** - 缓存与会话
- **Flyway** - 数据库迁移
- **Knife4j** - API 文档

## 项目结构

```
src/main/java/com/campus/wall/
├── common/          # 通用类
├── config/          # 配置类
├── constant/        # 常量类
├── controller/      # 控制器
├── dto/             # 数据传输对象
│   ├── auth/
│   ├── post/
│   ├── user/
│   └── ...
├── entity/          # 实体类
│   ├── post/
│   ├── system/
│   ├── user/
│   └── ...
├── enums/           # 枚举类
│   ├── post/
│   ├── system/
│   ├── user/
│   └── ...
├── mapper/          # MyBatis Mapper
│   ├── post/
│   ├── system/
│   ├── user/
│   └── ...
├── service/         # 服务层
│   ├── auth/impl/
│   ├── post/
│   ├── system/
│   └── ...
├── util/            # 工具类
└── vo/              # 视图对象
    ├── auth/
    ├── post/
    ├── user/
    └── ...
```

## 快速开始

### 环境要求

- JDK 21+
- Gradle 8.5+
- PostgreSQL 15+
- Redis 7+

### 数据库初始化

```bash
# 创建数据库
createdb campus_wall

# Flyway 会在应用启动时自动执行迁移脚本
```

### 启动服务

```bash
# 开发环境
./gradlew bootRun --args='--spring.profiles.active=dev'

# 或编译后运行
./gradlew bootJar
java -jar build/libs/campus-wall.jar --spring.profiles.active=dev
```

### 访问 API 文档

启动服务后访问: http://localhost:8080/swagger-ui.html

## 管理员账号初始化

- 系统不提供默认管理员口令
- 首次初始化时请设置唯一用户名和高强度密码（至少 8 位，包含大小写字母、数字、特殊字符）
- 严禁在文档、脚本或镜像中写入固定管理员密码

## 配置说明

主要配置文件:
- `application.yml` - 主配置
- `application-dev.yml` - 开发环境配置
- `application-test.yml` - 测试环境配置

环境变量:
- `DB_USERNAME` - 数据库用户名
- `DB_PASSWORD` - 数据库密码
- `REDIS_HOST` - Redis 主机
- `REDIS_PORT` - Redis 端口
- `REDIS_PASSWORD` - Redis 密码

## 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行属性测试
./gradlew test --tests "*PropertyTest"
```

## License

MIT
