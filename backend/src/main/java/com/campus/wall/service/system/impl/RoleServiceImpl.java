package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.PageResult;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.service.system.RoleService;
import com.campus.wall.vo.system.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<RoleVO> getAllRoles() {
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .orderByAsc(SysRole::getSortOrder)
        );
        return roles.stream().map(this::toRoleVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<RoleVO> queryRoles(int page, int size) {
        Page<SysRole> pageResult = sysRoleMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder)
        );
        List<RoleVO> records = pageResult.getRecords().stream()
                .map(this::toRoleVO)
                .collect(Collectors.toList());
        return PageResult.of(records, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent());
    }

    @Override
    @Transactional
    public Long createRole(String roleName, String roleKey, List<Long> menuIds) {
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(0);
        role.setSortOrder(0);
        sysRoleMapper.insert(role);

        // 分配菜单
        if (menuIds != null && !menuIds.isEmpty()) {
            assignMenus(role.getId(), menuIds);
        }

        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long roleId, String roleName, Integer status, Integer sortOrder, Integer dataScope, String remark, List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        // 禁止修改管理员角色
        if (isAdminRole(role)) {
            throw new RuntimeException("管理员角色不允许修改");
        }

        if (roleName != null) {
            role.setRoleName(roleName);
        }
        if (status != null) {
            role.setStatus(status);
        }
        if (sortOrder != null) {
            role.setSortOrder(sortOrder);
        }
        if (dataScope != null) {
            role.setDataScope(dataScope);
        }
        if (remark != null) {
            role.setRemark(remark);
        }
        sysRoleMapper.updateById(role);

        // 更新菜单分配
        if (menuIds != null) {
            sysRoleMenuMapper.deleteByRoleId(roleId);
            assignMenus(roleId, menuIds);
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role != null && isAdminRole(role)) {
            throw new RuntimeException("管理员角色不允许删除");
        }
        // 删除角色菜单关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        // 删除角色
        sysRoleMapper.deleteById(roleId);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role != null && isAdminRole(role)) {
            throw new RuntimeException("管理员角色不允许修改权限");
        }
        // 先删除已有的菜单关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        // 再插入新的菜单关联
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
        // 踢出该角色的所有用户，强制重新登录以刷新权限
        kickoutUsersByRoleId(roleId);
    }

    /**
     * 踢出指定角色的所有用户
     */
    private void kickoutUsersByRoleId(Long roleId) {
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(roleId);
        for (Long userId : userIds) {
            // 不踢出超级管理员
            if (userId != 1L) {
                StpUtil.kickout(userId);
            }
        }
    }

    /**
     * 判断是否为管理员角色
     */
    private boolean isAdminRole(SysRole role) {
        return role.getId() == 1L || "admin".equals(role.getRoleKey());
    }

    @Override
    @Transactional
    public void assignDepts(Long roleId, List<Long> deptIds, Integer dataScope) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        
        // 禁止修改管理员角色
        if (isAdminRole(role)) {
            throw new RuntimeException("管理员角色不允许修改");
        }
        
        // 更新数据范围
        if (dataScope != null) {
            role.setDataScope(dataScope);
            sysRoleMapper.updateById(role);
        }
        
        // 更新角色部门关联（仅在自定义数据权限时需要）
        sysRoleDeptMapper.deleteByRoleId(roleId);
        if (dataScope != null && dataScope == 2 && deptIds != null && !deptIds.isEmpty()) {
            for (Long deptId : deptIds) {
                sysRoleDeptMapper.insert(roleId, deptId);
            }
        }
    }

    private RoleVO toRoleVO(SysRole role) {
        Objects.requireNonNull(role, "角色不能为空");
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        // 获取角色菜单ID列表
        List<Long> menuIds = sysMenuMapper.selectMenuIdsByRoleId(role.getId());
        vo.setMenuIds(menuIds);
        return vo;
    }
}
