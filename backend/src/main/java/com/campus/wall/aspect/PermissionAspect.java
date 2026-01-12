package com.campus.wall.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.campus.wall.annotation.RequiresPermission;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.ResultCode;
import com.campus.wall.service.system.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 动态权限检查切面
 * 从数据库读取URL对应的权限标识进行校验
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Around("@annotation(com.campus.wall.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        // 检查是否登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 仅需登录的接口，不检查权限
        if (annotation.loginOnly()) {
            return joinPoint.proceed();
        }

        // 获取当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 超级管理员跳过权限检查
        if (userId == 1L) {
            return joinPoint.proceed();
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();
        String httpMethod = request.getMethod();

        // 从数据库查询该URL需要的权限
        String requiredPerm = permissionService.getPermissionByUrl(uri, httpMethod);

        if (requiredPerm == null || requiredPerm.isEmpty()) {
            // 没有配置权限要求，放行
            return joinPoint.proceed();
        }

        // 检查用户是否有该权限
        if (!permissionService.hasPermission(userId, requiredPerm)) {
            log.warn("用户 {} 访问 {} {} 权限不足，需要权限: {}", userId, httpMethod, uri, requiredPerm);
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        return joinPoint.proceed();
    }
}
