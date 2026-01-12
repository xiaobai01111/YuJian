package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.mapper.system.SysApiPermissionMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.service.system.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限服务实现
 * 支持从数据库动态加载URL-权限映射
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysApiPermissionMapper apiPermissionMapper;
    private final SysMenuMapper menuMapper;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // URL-权限映射缓存 key: method:url, value: permission
    private final Map<String, String> permissionCache = new ConcurrentHashMap<>();
    // 用户权限缓存 key: userId, value: permissions set
    private final Map<Long, Set<String>> userPermissionCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public String getPermissionByUrl(String url, String httpMethod) {
        // 先精确匹配
        String key = httpMethod.toUpperCase() + ":" + url;
        String perm = permissionCache.get(key);
        if (perm != null) {
            return perm;
        }

        // 模糊匹配（支持Ant风格路径）
        for (Map.Entry<String, String> entry : permissionCache.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length == 2) {
                String method = parts[0];
                String pattern = parts[1];
                if (method.equals(httpMethod.toUpperCase()) || method.equals("*")) {
                    if (pathMatcher.match(pattern, url)) {
                        return entry.getValue();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        // 从缓存获取用户权限
        Set<String> userPerms = userPermissionCache.computeIfAbsent(userId, this::loadUserPermissions);
        
        // 检查是否有通配符权限
        if (userPerms.contains("*")) {
            return true;
        }

        return userPerms.contains(permission);
    }

    @Override
    public void refreshCache() {
        log.info("刷新权限缓存...");
        
        // 清空缓存
        permissionCache.clear();
        userPermissionCache.clear();

        // 从数据库加载URL-权限映射
        List<SysApiPermission> apiPerms = apiPermissionMapper.selectList(
            new LambdaQueryWrapper<SysApiPermission>()
                .eq(SysApiPermission::getStatus, true)
        );

        for (SysApiPermission ap : apiPerms) {
            String key = ap.getHttpMethod().toUpperCase() + ":" + ap.getUrl();
            permissionCache.put(key, ap.getPermission());
        }

        log.info("权限缓存已刷新，共加载 {} 条URL权限映射", permissionCache.size());
    }

    /**
     * 从数据库加载用户权限
     */
    private Set<String> loadUserPermissions(Long userId) {
        List<String> perms = menuMapper.selectPermsByUserId(userId);
        return new HashSet<>(perms);
    }

    /**
     * 清除指定用户的权限缓存
     */
    public void clearUserCache(Long userId) {
        userPermissionCache.remove(userId);
    }
}
