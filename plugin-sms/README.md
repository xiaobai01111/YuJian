# 短信验证服务插件

Campus Wall 短信验证服务插件，支持阿里云、腾讯云短信服务。

## 功能特性

- ✅ 支持阿里云短信服务
- ✅ 支持腾讯云短信服务
- ✅ 验证码发送与验证
- ✅ 发送频率限制（60秒冷却）
- ✅ 每日发送限额（按手机号/IP）
- ✅ 验证码有效期配置
- ✅ 后台配置管理界面

## 快速开始

```bash
# 安装依赖
pnpm install

# 开发模式
pnpm run dev

# 构建打包
pnpm run build
```

## API 接口

### 发送验证码

```http
POST /api/plugins/sms/send
Content-Type: application/json

{
  "phone": "13800138000",
  "scene": "login"
}
```

**响应**:
```json
{
  "success": true,
  "message": "验证码已发送"
}
```

### 验证验证码

```http
POST /api/plugins/sms/verify
Content-Type: application/json

{
  "phone": "13800138000",
  "code": "123456",
  "scene": "login"
}
```

**响应**:
```json
{
  "success": true,
  "message": "验证成功"
}
```

## 配置说明

### 阿里云配置

1. 登录 [阿里云短信控制台](https://dysms.console.aliyun.com)
2. 创建短信签名（需审核通过）
3. 创建短信模板，模板内容需包含 `${code}` 变量
4. 获取 AccessKey ID 和 AccessKey Secret
5. 在插件配置页面填入相关信息

### 腾讯云配置

1. 登录 [腾讯云短信控制台](https://console.cloud.tencent.com/smsv2)
2. 创建应用，获取 AppId
3. 创建短信签名（需审核通过）
4. 创建短信模板，模板参数为验证码
5. 获取 SecretId 和 SecretKey
6. 在插件配置页面填入相关信息

## 短信模板示例

### 阿里云模板

```
您的验证码为：${code}，5分钟内有效，请勿泄露给他人。
```

### 腾讯云模板

```
您的验证码为：{1}，5分钟内有效，请勿泄露给他人。
```

## 限流规则

| 规则 | 默认值 | 说明 |
|------|--------|------|
| 冷却时间 | 60秒 | 同一手机号两次发送间隔 |
| 每日单号码上限 | 10次 | 同一手机号每日最多发送次数 |
| 每日单IP上限 | 50次 | 同一IP每日最多发送次数 |
| 验证码有效期 | 5分钟 | 验证码过期时间 |
| 最大验证次数 | 5次 | 同一验证码最多验证次数 |

## 安全建议

1. 生产环境务必配置发送限额
2. 定期检查发送日志，防止滥用
3. AccessKey/SecretKey 会加密存储，但建议使用子账号并限制权限
