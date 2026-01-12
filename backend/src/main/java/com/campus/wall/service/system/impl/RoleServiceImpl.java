package com.campus.wall.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.PageResult;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
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
    private final SysMenuMapper sysMenuMapper;

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
    public void updateRole(Long roleId, String roleName, List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        if (roleName != null) {
            role.setRoleName(roleName);
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
        // 删除角色菜单关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        // 删除角色
        sysRoleMapper.deleteById(roleId);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
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
