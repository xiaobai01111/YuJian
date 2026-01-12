package com.campus.wall.annotation;

import java.lang.annotation.*;

/**
 * 动态权限检查注解
 * 从数据库读取URL对应的权限标识进行校验
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    /**
     * 是否跳过权限检查（仅需登录）
     */
    boolean loginOnly() default false;
}
