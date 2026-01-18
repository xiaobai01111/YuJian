package com.campus.wall.config;

import cn.dev33.satoken.stp.StpInterface;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.entity.user.User;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     * (对应 sys_menus 表的 perms 字段，如 "post:delete")
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        // 超级管理员拥有所有权限
        String adminRoleKey = SecurityUtil.getSuperAdminRoleKey();
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(userId);
        if (roleKeys.contains(adminRoleKey)) {
            List<String> allPerms = new ArrayList<>(menuMapper.selectAllPerms());
            allPerms.add(SecurityConstants.ALL_PERMISSION);
            return allPerms;
        }

        // 部门停用的用户无权限
        User user = userMapper.selectById(userId);
        if (user != null && user.getDeptId() != null) {
            var dept = deptMapper.selectById(user.getDeptId());
            if (dept != null && dept.getStatus() != null && dept.getStatus() == 1) {
                return new ArrayList<>();
            }
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
        User user = userMapper.selectById(userId);
        if (user != null && user.getDeptId() != null) {
            var dept = deptMapper.selectById(user.getDeptId());
            if (dept != null && dept.getStatus() != null && dept.getStatus() == 1) {
                return new ArrayList<>();
            }
        }
        return roleMapper.selectRoleKeysByUserId(userId);
    }
}
