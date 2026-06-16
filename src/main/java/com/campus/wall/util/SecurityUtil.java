package com.campus.wall.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 安全工具类
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    private static volatile String superAdminRoleKey = "admin";

    public static void setSuperAdminRoleKey(String roleKey) {
        if (roleKey != null && !roleKey.trim().isEmpty()) {
            superAdminRoleKey = roleKey.trim();
        }
    }

    public static String getSuperAdminRoleKey() {
        return superAdminRoleKey;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前登录用户ID（可能为空）
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        try {
            return StpUtil.hasRole(superAdminRoleKey);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        try {
            return StpUtil.hasRole(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前用户是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        try {
            return StpUtil.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }
}
