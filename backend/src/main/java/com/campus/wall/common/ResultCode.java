package com.campus.wall.common;

import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(422, "业务逻辑错误"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证相关
    LOGIN_FAILED(1001, "用户名或密码错误"),
    USER_BANNED(1002, "账号已被封禁"),
    USER_NOT_VERIFIED(1003, "请先完成校园身份验证"),
    STUDENT_ID_EXISTS(1004, "该学号已被使用"),
    TOKEN_EXPIRED(1005, "登录已过期，请重新登录"),
    DEPT_DISABLED(1006, "所在部门已停用，无法登录"),

    // 业务相关
    POST_NOT_FOUND(2001, "帖子不存在"),
    COMMENT_NOT_FOUND(2002, "评论不存在"),
    PERMISSION_DENIED(2003, "无权操作此资源"),
    CONTENT_CONTAINS_SENSITIVE_WORD(2004, "内容包含敏感词，请修改后重试"),
    CREDIT_SCORE_TOO_LOW(2005, "信用分过低，无法发布商品"),
    RATE_LIMIT_EXCEEDED(2006, "操作过于频繁，请稍后再试"),

    // 文件相关
    FILE_TYPE_NOT_ALLOWED(3001, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(3002, "文件大小超过限制"),
    FILE_UPLOAD_FAILED(3003, "文件上传失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
