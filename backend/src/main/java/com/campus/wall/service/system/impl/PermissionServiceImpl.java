package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.config.SecurityProperties;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.mapper.system.SysApiPermissionMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.service.system.PermissionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
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
    private final SecurityProperties securityProperties;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // URL-权限映射缓存 key: method:url, value: api permission
    private final Map<String, SysApiPermission> permissionCache = new ConcurrentHashMap<>();
    // URL-权限规则模式缓存 key: method, value: api permission list
    private final Map<String, List<SysApiPermission>> permissionPatternCache = new ConcurrentHashMap<>();
    // 用户权限缓存 key: userId, value: permissions set
    private Cache<Long, Set<String>> userPermissionCache;

    @PostConstruct
    public void init() {
        userPermissionCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(securityProperties.getPermissionCacheTtlSeconds()))
                .maximumSize(securityProperties.getPermissionCacheMaxSize())
                .build();
        refreshCache();
    }

    @Override
    public SysApiPermission getApiPermissionByUrl(String url, String httpMethod) {
        // 先精确匹配
        String method = httpMethod == null ? "GET" : httpMethod.toUpperCase();
        String key = method + ":" + url;
        SysApiPermission perm = permissionCache.get(key);
        if (perm != null) {
            return perm;
        }
        SysApiPermission wildcardPerm = permissionCache.get("*:" + url);
        if (wildcardPerm != null) {
            return wildcardPerm;
        }

        // 模糊匹配（支持Ant风格路径）
        SysApiPermission matched = matchPattern(permissionPatternCache.get(method), url);
        if (matched != null) {
            return matched;
        }
        return matchPattern(permissionPatternCache.get("*"), url);
    }

    private SysApiPermission matchPattern(List<SysApiPermission> permissions, String url) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }
        for (SysApiPermission permission : permissions) {
            if (pathMatcher.match(permission.getUrl(), url)) {
                return permission;
            }
        }
        return null;
    }

    @Override
    public boolean hasApiPermissions() {
        return !permissionCache.isEmpty();
    }

    @Override
    public String getPermissionByUrl(String url, String httpMethod) {
        SysApiPermission perm = getApiPermissionByUrl(url, httpMethod);
        return perm != null ? perm.getPermission() : null;
    }

    @Override
    public boolean hasPermission(Long userId, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        // 从缓存获取用户权限
        Set<String> userPerms = userPermissionCache.get(userId, this::loadUserPermissions);
        
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
        permissionPatternCache.clear();
        if (userPermissionCache != null) {
            userPermissionCache.invalidateAll();
        }

        // 从数据库加载URL-权限映射
        List<SysApiPermission> apiPerms = apiPermissionMapper.selectList(
            new LambdaQueryWrapper<SysApiPermission>()
                .eq(SysApiPermission::getStatus, true)
        );

        for (SysApiPermission ap : apiPerms) {
            String method = ap.getHttpMethod() != null ? ap.getHttpMethod().toUpperCase() : "*";
            String key = method + ":" + ap.getUrl();
            permissionCache.put(key, ap);
            if (pathMatcher.isPattern(ap.getUrl())) {
                permissionPatternCache.computeIfAbsent(method, ignore -> new ArrayList<>()).add(ap);
            }
        }

        sortPatternCache();
        log.info("权限缓存已刷新，共加载 {} 条URL权限映射", permissionCache.size());
    }

    /**
     * 从数据库加载用户权限
     */
    private Set<String> loadUserPermissions(Long userId) {
        List<String> perms = menuMapper.selectPermsByUserId(userId);
        if (perms == null || perms.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(perms);
    }

    /**
     * 清除指定用户的权限缓存
     */
    @Override
    public void clearUserCache(Long userId) {
        if (userPermissionCache != null) {
            userPermissionCache.invalidate(userId);
        }
    }

    private void sortPatternCache() {
        for (List<SysApiPermission> list : permissionPatternCache.values()) {
            list.sort(this::comparePatternSpecificity);
        }
    }

    private int comparePatternSpecificity(SysApiPermission left, SysApiPermission right) {
        String leftPattern = left != null ? left.getUrl() : null;
        String rightPattern = right != null ? right.getUrl() : null;
        int leftWildcards = countWildcards(leftPattern);
        int rightWildcards = countWildcards(rightPattern);
        if (leftWildcards != rightWildcards) {
            return Integer.compare(leftWildcards, rightWildcards);
        }
        int leftLength = leftPattern != null ? leftPattern.length() : 0;
        int rightLength = rightPattern != null ? rightPattern.length() : 0;
        return Integer.compare(rightLength, leftLength);
    }

    private int countWildcards(String pattern) {
        if (pattern == null) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*' || c == '?') {
                count++;
            }
        }
        return count;
    }
}
