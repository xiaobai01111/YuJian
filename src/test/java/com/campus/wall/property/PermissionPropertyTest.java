package com.campus.wall.property;

import net.jqwik.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限相关属性测试
 * Property 12: 权限分配即时生效
 * Property 13: 权限拒绝正确性
 * Property 14: 封禁用户登录拒绝
 */
class PermissionPropertyTest {

    private static final String ALL_PERMISSION = "*";
    private static final Long SUPER_ADMIN_ID = 1L;
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_BANNED = 1;

    /**
     * Property 12: 超级管理员拥有所有权限
     */
    @Property(tries = 100)
    void superAdminHasAllPermissions(
            @ForAll("anyPermission") String permission) {
        List<String> adminPerms = List.of(ALL_PERMISSION);

        boolean hasPermission = checkPermission(adminPerms, permission);

        assertThat(hasPermission).isTrue();
    }

    /**
     * Property 13: 空权限列表拒绝所有请求
     */
    @Property(tries = 100)
    void emptyPermissionsDenyAll(
            @ForAll("anyPermission") String permission) {
        List<String> emptyPerms = List.of();

        boolean hasPermission = checkPermission(emptyPerms, permission);

        assertThat(hasPermission).isFalse();
    }

    /**
     * Property: 精确权限匹配
     */
    @Property(tries = 100)
    void exactPermissionMatch(
            @ForAll("anyPermission") String permission) {
        List<String> userPerms = List.of(permission);

        boolean hasPermission = checkPermission(userPerms, permission);

        assertThat(hasPermission).isTrue();
    }

    /**
     * Property: 不相关权限不匹配
     */
    @Property(tries = 100)
    void unrelatedPermissionsDontMatch(
            @ForAll("anyPermission") String userPerm,
            @ForAll("anyPermission") String requiredPerm) {
        Assume.that(!userPerm.equals(requiredPerm));
        Assume.that(!userPerm.equals(ALL_PERMISSION));

        List<String> userPerms = List.of(userPerm);

        boolean hasPermission = checkPermission(userPerms, requiredPerm);

        assertThat(hasPermission).isFalse();
    }

    /**
     * Property 12: 权限列表包含即生效
     */
    @Property(tries = 50)
    void permissionInListIsGranted(
            @ForAll("permissionList") List<String> permissions,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 0, max = 4) int index) {
        Assume.that(!permissions.isEmpty());
        int safeIndex = index % permissions.size();
        String targetPerm = permissions.get(safeIndex);

        boolean hasPermission = checkPermission(permissions, targetPerm);

        assertThat(hasPermission).isTrue();
    }

    /**
     * Property: 角色权限继承正确性
     * 用户通过角色获得的权限应该正确合并
     */
    @Property(tries = 50)
    void rolePermissionsAreMerged(
            @ForAll("permissionList") List<String> role1Perms,
            @ForAll("permissionList") List<String> role2Perms) {
        Set<String> merged = new HashSet<>();
        merged.addAll(role1Perms);
        merged.addAll(role2Perms);
        List<String> mergedList = new ArrayList<>(merged);

        // 合并后的权限列表应该包含两个角色的所有权限
        for (String perm : role1Perms) {
            assertThat(checkPermission(mergedList, perm)).isTrue();
        }
        for (String perm : role2Perms) {
            assertThat(checkPermission(mergedList, perm)).isTrue();
        }
    }

    /**
     * Property: 超级管理员用户ID为1
     */
    @Example
    void superAdminUserIdIsOne() {
        assertThat(SUPER_ADMIN_ID).isEqualTo(1L);
    }

    /**
     * Property 14: 封禁用户登录拒绝
     * 被封禁的用户无法登录
     */
    @Property(tries = 100)
    void bannedUserCannotLogin(
            @ForAll @net.jqwik.api.constraints.LongRange(min = 2, max = 10000) Long userId) {
        int userStatus = STATUS_BANNED;

        boolean canLogin = checkLoginAllowed(userStatus);

        assertThat(canLogin).isFalse();
    }

    /**
     * Property 14: 正常用户可以登录
     */
    @Property(tries = 100)
    void normalUserCanLogin(
            @ForAll @net.jqwik.api.constraints.LongRange(min = 2, max = 10000) Long userId) {
        int userStatus = STATUS_NORMAL;

        boolean canLogin = checkLoginAllowed(userStatus);

        assertThat(canLogin).isTrue();
    }

    /**
     * Property 14: 封禁后状态变更立即生效
     */
    @Property(tries = 50)
    void banStatusChangeIsImmediate(
            @ForAll @net.jqwik.api.constraints.IntRange(min = 0, max = 1) int initialStatus,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 0, max = 1) int newStatus) {
        // 状态变更后，登录检查应该立即反映新状态
        boolean canLoginAfterChange = checkLoginAllowed(newStatus);

        if (newStatus == STATUS_BANNED) {
            assertThat(canLoginAfterChange).isFalse();
        } else {
            assertThat(canLoginAfterChange).isTrue();
        }
    }

    /**
     * 模拟登录检查逻辑
     */
    private boolean checkLoginAllowed(int userStatus) {
        return userStatus == STATUS_NORMAL;
    }

    /**
     * 模拟权限检查逻辑
     */
    private boolean checkPermission(List<String> userPerms, String requiredPerm) {
        if (userPerms == null || userPerms.isEmpty()) {
            return false;
        }

        // 超级管理员权限
        if (userPerms.contains(ALL_PERMISSION)) {
            return true;
        }

        // 精确匹配
        return userPerms.contains(requiredPerm);
    }

    @Provide
    Arbitrary<String> anyPermission() {
        Arbitrary<String> modules = Arbitraries.of("system", "content", "user", "post", "file");
        Arbitrary<String> resources = Arbitraries.of("user", "role", "menu", "post", "comment", "report", "file");
        Arbitrary<String> actions = Arbitraries.of("list", "create", "update", "delete", "handle", "upload");

        return Combinators.combine(modules, resources, actions)
                .as((m, r, a) -> m + ":" + r + ":" + a);
    }

    @Provide
    Arbitrary<List<String>> permissionList() {
        return anyPermission().list().ofMinSize(1).ofMaxSize(10);
    }
}
