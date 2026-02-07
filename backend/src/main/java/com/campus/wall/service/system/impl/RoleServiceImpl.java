package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleDept;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.util.SecurityUtil;
import com.campus.wall.service.system.RoleService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.vo.system.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final DataScopeService dataScopeService;
    private final OperLogService operLogService;
    private final PermissionService permissionService;

    private static final String PERM_ROLE_ASSIGN = "system:role:assign";
    private static final String PERM_ROLE_ENABLE = "system:role:enable";
    private static final String PERM_ROLE_DISABLE = "system:role:disable";

    @Override
    public List<RoleVO> getAllRoles() {
        List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .orderByAsc(SysRole::getSortOrder)
        );
        return toRoleVOList(roles);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return sysMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public RoleVO getRoleById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return toRoleVO(role, true);
    }

    @Override
    public PageResult<RoleVO> queryRoles(int page, int size, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getSortOrder);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String likeKeyword = "%" + keyword.trim() + "%";
            wrapper.and(q -> q
                    .like(SysRole::getRoleName, likeKeyword)
                    .or()
                    .like(SysRole::getRoleKey, likeKeyword)
                    .or()
                    .like(SysRole::getRemark, likeKeyword)
            );
        }
        Page<SysRole> pageResult = sysRoleMapper.selectPage(
                new Page<>(page, size),
                wrapper
        );
        List<RoleVO> records = toRoleVOList(pageResult.getRecords());
        return PageResult.of(records, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent());
    }

    @Override
    @Transactional
    public RoleVO createRole(String roleName, String roleKey, Integer status, Integer sortOrder, String remark, List<Long> menuIds) {
        Long existing = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleKey, roleKey));
        if (existing != null && existing > 0) {
            throw new BusinessException("角色标识已存在");
        }
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setStatus(status != null ? status : 0);
        role.setSortOrder(sortOrder != null ? sortOrder : 0);
        role.setRemark(remark);
        sysRoleMapper.insert(role);

        // 分配菜单
        if (menuIds != null && !menuIds.isEmpty()) {
            requireCurrentUserPermission(PERM_ROLE_ASSIGN, "缺少角色授权权限");
            assignMenus(role.getId(), menuIds);
        }

        HashMap<String, Object> after = new HashMap<>();
        after.put("roleName", role.getRoleName());
        after.put("roleKey", role.getRoleKey());
        after.put("status", role.getStatus());
        after.put("sortOrder", role.getSortOrder());
        after.put("remark", role.getRemark());
        after.put("menuIds", menuIds);
        operLogService.log(StpUtil.getLoginIdAsLong(), null, "role", role.getId(), "create", null, null, after, null);
        return toRoleVO(role, false);
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long roleId, String roleName, String roleKey, Integer status, Integer sortOrder, String remark, List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        boolean isAdmin = isAdminRole(role);

        if (roleKey != null && !roleKey.equals(role.getRoleKey())) {
            throw new BusinessException("角色标识不允许修改");
        }

        // 管理员角色只允许修改备注和排序
        if (isAdmin) {
            if (roleName != null && !roleName.equals(role.getRoleName())) {
                throw new BusinessException("管理员角色名称不允许修改");
            }
            if (status != null && !status.equals(role.getStatus())) {
                throw new BusinessException("管理员角色状态不允许修改");
            }
            if (menuIds != null) {
                throw new BusinessException("管理员角色权限不允许修改");
            }
        }

        HashMap<String, Object> before = new HashMap<>();
        before.put("roleName", role.getRoleName());
        before.put("roleKey", role.getRoleKey());
        before.put("status", role.getStatus());
        before.put("sortOrder", role.getSortOrder());
        before.put("remark", role.getRemark());
        Integer oldStatus = role.getStatus();

        if (!isAdmin && status != null && !Objects.equals(oldStatus, status)) {
            if (status == 0) {
                requireCurrentUserPermission(PERM_ROLE_ENABLE, "缺少启用角色权限");
            } else if (status == 1) {
                requireCurrentUserPermission(PERM_ROLE_DISABLE, "缺少停用角色权限");
            }
        }
        if (menuIds != null) {
            requireCurrentUserPermission(PERM_ROLE_ASSIGN, "缺少角色授权权限");
        }

        if (roleName != null && !isAdmin) {
            role.setRoleName(roleName);
        }
        if (status != null && !isAdmin) {
            role.setStatus(status);
        }
        if (sortOrder != null) {
            role.setSortOrder(sortOrder);
        }
        if (remark != null) {
            role.setRemark(remark);
        }
        sysRoleMapper.updateById(role);

        // 更新菜单分配（管理员角色不允许修改菜单）
        if (menuIds != null) {
            assignMenus(roleId, menuIds);
        }

        if (!isAdmin && status != null && !Objects.equals(oldStatus, status) && status == 1) {
            kickoutUsersByRoleId(roleId);
        }

        HashMap<String, Object> after = new HashMap<>();
        after.put("roleName", role.getRoleName());
        after.put("roleKey", role.getRoleKey());
        after.put("status", role.getStatus());
        after.put("sortOrder", role.getSortOrder());
        after.put("remark", role.getRemark());
        operLogService.log(StpUtil.getLoginIdAsLong(), null, "role", roleId, "update", null, before, after, null);
        return toRoleVO(role, false);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId, boolean deleteUsers, String reason) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (isAdminRole(role)) {
            throw new BusinessException("管理员角色不允许删除");
        }
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(roleId);
        Long operatorId = StpUtil.getLoginIdAsLong();
        
        if (deleteUsers && userIds != null && !userIds.isEmpty()) {
            // 过滤掉 admin 用户，admin 不能删除
            List<Long> deletableUserIds = userIds.stream()
                .filter(userId -> !isSystemAdminUserId(userId) && !userId.equals(operatorId))
                .collect(Collectors.toList());
            
            if (!deletableUserIds.isEmpty()) {
                userService.deleteUsersWithReason(deletableUserIds, operatorId, reason != null ? reason : "角色删除");
            }
        }
        
        // 先踢出所有相关用户（在删除关联之前）
        kickoutUsersByIds(userIds);
        
        // 删除角色菜单关联
        sysRoleMenuMapper.deleteByRoleId(roleId);
        // 删除用户-角色关联，角色删除后用户变为未分配
        sysUserRoleMapper.deleteByRoleId(roleId);
        // 删除角色
        sysRoleMapper.deleteById(roleId);

        HashMap<String, Object> before = new HashMap<>();
        if (role != null) {
            before.put("roleName", role.getRoleName());
            before.put("roleKey", role.getRoleKey());
            before.put("status", role.getStatus());
            before.put("sortOrder", role.getSortOrder());
            before.put("remark", role.getRemark());
        }
        operLogService.log(operatorId, null, "role", roleId, "delete", reason, before, null, null);
    }

    @Override
    @Transactional
    public RoleVO assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (isAdminRole(role)) {
            throw new BusinessException("管理员角色不允许修改权限");
        }
        requireCurrentUserPermission(PERM_ROLE_ASSIGN, "缺少角色授权权限");
        if (menuIds == null) {
            menuIds = List.of();
        }
        ensureAssignableMenuScope(menuIds);
        List<Long> beforeMenuIds = sysMenuMapper.selectMenuIdsByRoleId(roleId);
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

        HashMap<String, Object> before = new HashMap<>();
        before.put("menuIds", beforeMenuIds);
        HashMap<String, Object> after = new HashMap<>();
        after.put("menuIds", menuIds);
        operLogService.log(StpUtil.getLoginIdAsLong(), null, "role", roleId, "menu_assign", null, before, after, null);
        RoleVO vo = toRoleVO(role, false);
        vo.setMenuIds(menuIds);
        return vo;
    }

    @Override
    @Transactional
    public RoleVO updateRoleStatus(Long roleId, Integer status) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (isAdminRole(role)) {
            throw new BusinessException("管理员角色状态不允许修改");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("角色状态参数非法");
        }

        Integer oldStatus = role.getStatus();
        if (Objects.equals(oldStatus, status)) {
            return toRoleVO(role, false);
        }
        if (status == 0) {
            requireCurrentUserPermission(PERM_ROLE_ENABLE, "缺少启用角色权限");
        } else {
            requireCurrentUserPermission(PERM_ROLE_DISABLE, "缺少停用角色权限");
        }

        role.setStatus(status);
        sysRoleMapper.updateById(role);
        if (status == 1) {
            kickoutUsersByRoleId(roleId);
        }

        HashMap<String, Object> before = new HashMap<>();
        before.put("status", oldStatus);
        HashMap<String, Object> after = new HashMap<>();
        after.put("status", status);
        String action = status == 0 ? "enable" : "disable";
        operLogService.log(StpUtil.getLoginIdAsLong(), null, "role", roleId, action, null, before, after, null);
        return toRoleVO(role, false);
    }

    @Override
    public List<Long> getRoleDeptIds(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return sysRoleDeptMapper.selectDeptIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public RoleVO assignDepts(Long roleId, List<Long> deptIds) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (isAdminRole(role)) {
            throw new BusinessException("管理员角色不允许修改数据权限");
        }
        requireCurrentUserPermission(PERM_ROLE_ASSIGN, "缺少角色授权权限");

        List<Long> normalizedDeptIds = deptIds == null ? List.of() : deptIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        ensureAssignableDeptScope(normalizedDeptIds);

        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));

        for (Long deptId : normalizedDeptIds) {
            SysRoleDept roleDept = new SysRoleDept();
            roleDept.setRoleId(roleId);
            roleDept.setDeptId(deptId);
            sysRoleDeptMapper.insert(roleDept);
        }

        HashMap<String, Object> after = new HashMap<>();
        after.put("deptIds", normalizedDeptIds);
        operLogService.log(StpUtil.getLoginIdAsLong(), null, "role", roleId, "dept_assign", null, null, after, null);

        RoleVO vo = toRoleVO(role, false);
        vo.setDeptIds(normalizedDeptIds);
        return vo;
    }

    @Override
    public List<UserVO> getRoleUsers(Long roleId) {
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
            .filter(Objects::nonNull)
            .map(user -> {
                UserVO vo = new UserVO();
                BeanUtils.copyProperties(user, vo);
                return vo;
            })
            .collect(Collectors.toList());
    }

    /**
     * 踢出指定角色的所有用户
     */
    private void kickoutUsersByRoleId(Long roleId) {
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(roleId);
        kickoutUsersByIds(userIds);
    }

    private void kickoutUsersByIds(List<Long> userIds) {
        if (userIds == null) {
            return;
        }
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
        return role != null && SecurityUtil.getSuperAdminRoleKey().equals(role.getRoleKey());
    }

    private boolean isSystemAdminUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleKeys = sysRoleMapper.selectRoleKeysByUserId(userId);
        return roleKeys != null && roleKeys.contains(SecurityUtil.getSuperAdminRoleKey());
    }

    private void requireCurrentUserPermission(String permission, String message) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (isSystemAdminUserId(operatorId)) {
            return;
        }
        if (!permissionService.hasPermission(operatorId, permission)) {
            throw new BusinessException(message);
        }
    }

    private void ensureAssignableMenuScope(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (isSystemAdminUserId(operatorId)) {
            return;
        }

        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(operatorId);
        Set<Long> allowedMenuIds = new HashSet<>();
        if (roles != null) {
            for (SysRole role : roles) {
                if (role == null || role.getId() == null) {
                    continue;
                }
                List<Long> roleMenuIds = sysMenuMapper.selectMenuIdsByRoleId(role.getId());
                if (roleMenuIds != null) {
                    allowedMenuIds.addAll(roleMenuIds);
                }
            }
        }

        List<Long> overGranted = menuIds.stream()
            .filter(Objects::nonNull)
            .filter(menuId -> !allowedMenuIds.contains(menuId))
            .distinct()
            .collect(Collectors.toList());
        if (!overGranted.isEmpty()) {
            throw new BusinessException("不能授予超出当前账号权限上限的菜单");
        }
    }

    private void ensureAssignableDeptScope(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (isSystemAdminUserId(operatorId)) {
            return;
        }
        DataScopeService.DataScope scope = dataScopeService.resolveScope(operatorId);
        if (scope.isAllowAll()) {
            return;
        }
        List<Long> allowedDeptIds = scope.getScopedDeptIds();
        if (allowedDeptIds == null || allowedDeptIds.isEmpty()) {
            throw new BusinessException("不能分配超出当前账号数据权限范围的部门");
        }
        List<Long> overGranted = deptIds.stream()
            .filter(Objects::nonNull)
            .filter(deptId -> !allowedDeptIds.contains(deptId))
            .distinct()
            .collect(Collectors.toList());
        if (!overGranted.isEmpty()) {
            throw new BusinessException("不能分配超出当前账号数据权限范围的部门");
        }
    }

    private RoleVO toRoleVO(SysRole role, boolean withMenuIds) {
        return toRoleVO(role, withMenuIds, null);
    }

    private RoleVO toRoleVO(SysRole role, boolean withMenuIds, List<Long> deptIds) {
        Objects.requireNonNull(role, "角色不能为空");
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        if (withMenuIds) {
            List<Long> menuIds = sysMenuMapper.selectMenuIdsByRoleId(role.getId());
            vo.setMenuIds(menuIds);
        }
        if (deptIds == null) {
            deptIds = sysRoleDeptMapper.selectDeptIdsByRoleId(role.getId());
        }
        vo.setDeptIds(deptIds == null ? List.of() : deptIds);
        return vo;
    }

    private List<RoleVO> toRoleVOList(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = roles.stream()
            .filter(Objects::nonNull)
            .map(SysRole::getId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, List<Long>> deptMap = loadDeptIdsByRoleIds(roleIds);
        return roles.stream()
            .filter(Objects::nonNull)
            .map(role -> toRoleVO(role, false, deptMap.get(role.getId())))
            .collect(Collectors.toList());
    }

    private Map<Long, List<Long>> loadDeptIdsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        List<SysRoleDept> roleDepts = sysRoleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, roleIds)
        );
        if (roleDepts == null || roleDepts.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Long>> result = new HashMap<>();
        for (SysRoleDept roleDept : roleDepts) {
            if (roleDept == null || roleDept.getRoleId() == null || roleDept.getDeptId() == null) {
                continue;
            }
            result.computeIfAbsent(roleDept.getRoleId(), ignore -> new ArrayList<>()).add(roleDept.getDeptId());
        }
        return result;
    }
}
