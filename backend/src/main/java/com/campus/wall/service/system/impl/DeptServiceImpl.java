package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final OperLogService operLogService;

    @Override
    public List<SysDept> listAll() {
        return deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder)
                .orderByAsc(SysDept::getId)
        );
    }

    @Override
    public List<SysDept> getTree() {
        List<SysDept> allDepts = listAll();
        return buildTree(allDepts, 0L);
    }

    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (dept.getParentId().equals(parentId)) {
                result.add(dept);
            }
        }
        return result;
    }

    @Override
    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long create(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        }
        if (dept.getParentId() == 0L) {
            dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        }
        if (dept.getDeptName() != null) {
            dept.setDeptName(dept.getDeptName().trim());
        }
        if (dept.getDeptName() == null || dept.getDeptName().isEmpty()) {
            throw new BusinessException("部门名称不能为空");
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(0);
        }
        if (dept.getDataScope() == null) {
            dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        }
        if (dept.getDataScope() < SecurityConstants.DATA_SCOPE_ALL || dept.getDataScope() > SecurityConstants.DATA_SCOPE_SELF) {
            throw new BusinessException("数据权限范围不合法");
        }
        Long parentId = dept.getParentId();
        Long existing = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId)
                .eq(SysDept::getDeptName, dept.getDeptName())
        );
        if (existing != null && existing > 0) {
            throw new BusinessException("同级部门名称已存在");
        }
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, SysDept dept) {
        SysDept existing = deptMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("部门不存在");
        }

        if (isSystemDept(existing.getId())) {
            if (dept.getParentId() != null && !dept.getParentId().equals(existing.getParentId())) {
                throw new BusinessException("系统部门不允许修改上级");
            }
            if (dept.getDataScope() != null && !dept.getDataScope().equals(existing.getDataScope())) {
                throw new BusinessException("系统部门不允许修改数据范围");
            }
        }

        if (dept.getParentId() != null && dept.getParentId() == 0L && !isSystemDept(existing.getId())) {
            dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        }

        if (dept.getDeptName() != null) {
            dept.setDeptName(dept.getDeptName().trim());
            if (dept.getDeptName().isEmpty()) {
                throw new BusinessException("部门名称不能为空");
            }
        }

        Long targetParentId = dept.getParentId() != null ? dept.getParentId() : existing.getParentId();
        String targetName = dept.getDeptName() != null ? dept.getDeptName() : existing.getDeptName();
        Long sameNameCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, targetParentId)
                .eq(SysDept::getDeptName, targetName)
                .ne(SysDept::getId, id)
        );
        if (sameNameCount != null && sameNameCount > 0) {
            throw new BusinessException("同级部门名称已存在");
        }
        
        // 防止循环上级：不能将自己或子孙部门设为上级
        if (dept.getParentId() != null && !dept.getParentId().equals(existing.getParentId())) {
            if (dept.getParentId().equals(id)) {
                throw new BusinessException("不能将自己设为上级部门");
            }
            if (isDescendant(id, dept.getParentId())) {
                throw new BusinessException("不能将子孙部门设为上级部门");
            }
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        HashMap<String, Object> before = new HashMap<>();
        before.put("deptName", existing.getDeptName());
        before.put("parentId", existing.getParentId());
        before.put("status", existing.getStatus());
        
        existing.setDeptName(dept.getDeptName());
        existing.setParentId(dept.getParentId());
        existing.setSortOrder(dept.getSortOrder());
        existing.setLeader(dept.getLeader());
        existing.setPhone(dept.getPhone());
        existing.setEmail(dept.getEmail());
        if (dept.getDataScope() != null) {
            if (dept.getDataScope() < SecurityConstants.DATA_SCOPE_ALL || dept.getDataScope() > SecurityConstants.DATA_SCOPE_SELF) {
                throw new BusinessException("数据权限范围不合法");
            }
            existing.setDataScope(dept.getDataScope());
        }
        // 状态变更使用专门的 updateStatus 方法
        
        deptMapper.updateById(existing);
        
        HashMap<String, Object> after = new HashMap<>();
        after.put("deptName", existing.getDeptName());
        after.put("parentId", existing.getParentId());
        after.put("status", existing.getStatus());
        operLogService.log(operatorId, null, "dept", id, "update", null, before, after, null);
    }

    /**
     * 检查 targetId 是否是 deptId 的子孙部门
     */
    private boolean isDescendant(Long deptId, Long targetId) {
        if (targetId == null || targetId == 0L) {
            return false;
        }
        List<SysDept> allDepts = listAll();
        return isDescendantRecursive(allDepts, deptId, targetId);
    }

    private boolean isDescendantRecursive(List<SysDept> allDepts, Long parentId, Long targetId) {
        for (SysDept dept : allDepts) {
            if (dept.getParentId().equals(parentId)) {
                if (dept.getId().equals(targetId)) {
                    return true;
                }
                if (isDescendantRecursive(allDepts, dept.getId(), targetId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (isSystemDept(id)) {
            throw new BusinessException("系统部门不允许停用");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        Integer oldStatus = dept.getStatus();
        
        dept.setStatus(status);
        deptMapper.updateById(dept);
        
        // 停用部门时踢出该部门下所有用户
        if (status == 1) {
            List<Long> deptIds = collectDeptIdsForKickout(id);
            kickoutDeptUsers(deptIds);
        }
        
        // 审计日志
        HashMap<String, Object> before = new HashMap<>();
        before.put("status", oldStatus);
        HashMap<String, Object> after = new HashMap<>();
        after.put("status", status);
        operLogService.log(operatorId, null, "dept", id, "status_change", null, before, after, null);
    }

    @Override
    @Transactional
    public void delete(Long id, DeptDeleteDTO dto) {
        if (isSystemDept(id)) {
            throw new BusinessException("系统部门不允许删除");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        
        // 检查是否有子部门
        Long childCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException("存在子部门，无法删除，请先删除或转移子部门");
        }
        
        Long operatorId = StpUtil.getLoginIdAsLong();
        List<User> deptUsers = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getDeptId, id).eq(User::getDeleted, 0)
        );
        
        // 处理部门下的用户
        if (!deptUsers.isEmpty()) {
            DeptDeleteDTO.UserHandleStrategy strategy = dto != null && dto.getUserStrategy() != null
                ? dto.getUserStrategy()
                : DeptDeleteDTO.UserHandleStrategy.UNASSIGN;
            String reason = dto != null ? dto.getReason() : null;

            if (strategy == DeptDeleteDTO.UserHandleStrategy.DELETE && (reason == null || reason.trim().isEmpty())) {
                throw new BusinessException("删除用户必须提供原因");
            }

            switch (strategy) {
                case TRANSFER_PARENT:
                    // 转移到上级部门，校验上级部门存在且启用
                    Long parentId = dept.getParentId();
                    if (parentId != null && parentId != 0L) {
                        SysDept parentDept = deptMapper.selectById(parentId);
                        if (parentDept == null) {
                            throw new BusinessException("上级部门不存在，无法转移用户");
                        }
                        if (parentDept.getStatus() != 0) {
                            throw new BusinessException("上级部门已停用，无法转移用户，请选择其他处理方式");
                        }
                    }
                    // 先踢出当前部门下用户，保证权限立即失效
                    kickoutUsers(deptUsers);
                    for (User user : deptUsers) {
                        user.setDeptId((parentId == null || parentId == 0L) ? null : parentId);
                        userMapper.updateById(user);
                    }
                    break;
                case UNASSIGN:
                    // 先踢出当前部门下用户，保证权限立即失效
                    kickoutUsers(deptUsers);
                    // 转移到未分配
                    for (User user : deptUsers) {
                        user.setDeptId(null);
                        userMapper.updateById(user);
                    }
                    break;
                case DELETE:
                    // 删除用户
                    List<User> deletableUsers = deptUsers.stream()
                        .filter(u -> !isSystemAdmin(u) && !u.getId().equals(operatorId))
                        .collect(Collectors.toList());
                    // 先踢出当前部门下用户（除系统管理员）
                    kickoutUsers(deptUsers);
                    // 删除用户
                    List<Long> userIds = deletableUsers.stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                    if (!userIds.isEmpty()) {
                        userService.deleteUsersWithReason(userIds, operatorId, reason != null ? reason : "部门删除");
                    }
                    // 处理未删除的用户（管理员/操作者），清除其dept_id避免悬挂引用
                    List<User> excludedUsers = deptUsers.stream()
                        .filter(u -> isSystemAdmin(u) || u.getId().equals(operatorId))
                        .collect(Collectors.toList());
                    for (User user : excludedUsers) {
                        user.setDeptId(null);
                        userMapper.updateById(user);
                    }
                    break;
            }
            
        }
        
        // 删除部门
        deptMapper.deleteById(id);
        
        // 审计日志
        HashMap<String, Object> before = new HashMap<>();
        before.put("deptName", dept.getDeptName());
        before.put("parentId", dept.getParentId());
        before.put("status", dept.getStatus());
        String strategyName = dto != null && dto.getUserStrategy() != null ? dto.getUserStrategy().name() : "UNASSIGN";
        operLogService.log(operatorId, null, "dept", id, "delete", 
            "策略:" + strategyName + (dto != null && dto.getReason() != null ? ", 原因:" + dto.getReason() : ""), 
            before, null, null);
    }

    @Override
    public List<UserVO> getDeptUsers(Long deptId) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getDeptId, deptId).eq(User::getDeleted, 0)
        );
        return users.stream().map(this::toUserVO).collect(Collectors.toList());
    }

    @Override
    public Long getDeptUserCount(Long deptId) {
        return userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getDeptId, deptId).eq(User::getDeleted, 0)
        );
    }

    @SuppressWarnings("unused")
    private void kickoutDeptUsers(Long deptId) {
        kickoutDeptUsers(List.of(deptId));
    }

    private void kickoutDeptUsers(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return;
        }
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().in(User::getDeptId, deptIds).eq(User::getDeleted, 0)
        );
        kickoutUsers(users);
    }

    private void kickoutUsers(List<User> users) {
        if (users == null) {
            return;
        }
        for (User user : users) {
            if (!isSystemAdmin(user)) {
                StpUtil.kickout(user.getId());
            }
        }
    }

    private boolean isSystemAdmin(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        List<String> roleKeys = roleMapper.selectRoleKeysByUserId(user.getId());
        return roleKeys != null && roleKeys.contains(SecurityUtil.getSuperAdminRoleKey());
    }

    private List<Long> collectDeptIdsForKickout(Long rootDeptId) {
        if (rootDeptId == null) {
            return List.of();
        }
        List<SysDept> allDepts = listAll();
        List<Long> result = new java.util.ArrayList<>();
        result.add(rootDeptId);
        collectChildren(allDepts, rootDeptId, result);
        return result;
    }

    private void collectChildren(List<SysDept> allDepts, Long parentId, List<Long> result) {
        for (SysDept dept : allDepts) {
            if (dept.getParentId().equals(parentId)) {
                result.add(dept.getId());
                collectChildren(allDepts, dept.getId(), result);
            }
        }
    }

    private boolean isSystemDept(Long deptId) {
        return deptId != null && deptId.equals(SecurityConstants.SYSTEM_DEPT_ID);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
