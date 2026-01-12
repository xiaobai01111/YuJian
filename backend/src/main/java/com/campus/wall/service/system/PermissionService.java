package com.campus.wall.service.system;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 根据URL和HTTP方法获取所需权限标识
     */
    String getPermissionByUrl(String url, String httpMethod);

    /**
     * 检查用户是否拥有指定权限
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * 刷新权限缓存
     */
    void refreshCache();
}
