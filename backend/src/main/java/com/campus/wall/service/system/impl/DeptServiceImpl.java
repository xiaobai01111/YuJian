package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.user.UserVO;
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
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(0);
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
            kickoutDeptUsers(id);
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
                    for (User user : deptUsers) {
                        user.setDeptId((parentId == null || parentId == 0L) ? null : parentId);
                        userMapper.updateById(user);
                    }
                    break;
                case UNASSIGN:
                    // 转移到未分配
                    for (User user : deptUsers) {
                        user.setDeptId(null);
                        userMapper.updateById(user);
                    }
                    break;
                case DELETE:
                    // 先踢出所有要删除的用户，避免删除后无法踢出
                    List<User> deletableUsers = deptUsers.stream()
                        .filter(u -> !"admin".equals(u.getUsername()) && !u.getId().equals(operatorId))
                        .collect(Collectors.toList());
                    for (User user : deletableUsers) {
                        StpUtil.kickout(user.getId());
                    }
                    // 删除用户
                    List<Long> userIds = deletableUsers.stream()
                        .map(User::getId)
                        .collect(Collectors.toList());
                    if (!userIds.isEmpty()) {
                        userService.deleteUsersWithReason(userIds, operatorId, reason != null ? reason : "部门删除");
                    }
                    // 处理未删除的用户（管理员/操作者），清除其dept_id避免悬挂引用
                    List<User> excludedUsers = deptUsers.stream()
                        .filter(u -> "admin".equals(u.getUsername()) || u.getId().equals(operatorId))
                        .collect(Collectors.toList());
                    for (User user : excludedUsers) {
                        user.setDeptId(null);
                        userMapper.updateById(user);
                    }
                    break;
            }
            
            // 踢出所有相关用户（未被删除的）
            kickoutDeptUsers(id);
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

    private void kickoutDeptUsers(Long deptId) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>().eq(User::getDeptId, deptId).eq(User::getDeleted, 0)
        );
        for (User user : users) {
            if (!"admin".equals(user.getUsername())) {
                StpUtil.kickout(user.getId());
            }
        }
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
