package com.campus.wall.config;

import cn.dev33.satoken.stp.StpInterface;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义权限验证接口扩展
 * 实现 Sa-Token 的 StpInterface 接口，用于获取用户权限和角色
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     * (对应 sys_menus 表的 perms 字段，如 "post:delete")
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        // 超级管理员 (userId == 1) 拥有所有权限
        if (userId == 1L) {
            return List.of("*");
        }

        // 从数据库查询该用户的权限列表
        return menuMapper.selectPermsByUserId(userId);
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     * (对应 sys_roles 表的 role_key 字段，如 "moderator")
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return roleMapper.selectRoleKeysByUserId(userId);
    }
}
