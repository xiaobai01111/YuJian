package com.campus.wall.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.dto.system.DeptDeleteDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.service.system.OperLogService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.vo.system.DeptTreeVO;
import com.campus.wall.vo.user.UserVO;
import com.campus.wall.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;
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

    @Override
    public List<DeptTreeVO> getDeptTree() {
        List<SysDept> allDepts = listAll();
        List<DeptTreeVO> tree = buildDeptTreeVO(allDepts, 0L);
        if (tree.isEmpty() && !allDepts.isEmpty()) {
            tree = buildRootDeptTreeVO(allDepts);
        }
        return tree;
    }

    private List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (java.util.Objects.equals(dept.getParentId(), parentId)) {
                result.add(dept);
            }
        }
        return result;
    }

    private List<DeptTreeVO> buildDeptTreeVO(List<SysDept> depts, Long parentId) {
        List<DeptTreeVO> result = new ArrayList<>();
        for (SysDept dept : depts) {
            if (java.util.Objects.equals(dept.getParentId(), parentId)) {
                DeptTreeVO vo = toDeptTreeVO(dept);
                vo.setChildren(buildDeptTreeVO(depts, dept.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    private List<DeptTreeVO> buildRootDeptTreeVO(List<SysDept> depts) {
        List<DeptTreeVO> roots = new ArrayList<>();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (SysDept dept : depts) {
            if (dept.getId() != null) {
                ids.add(dept.getId());
            }
        }
        for (SysDept dept : depts) {
            Long parentId = dept.getParentId();
            boolean isRoot = parentId == null
                || parentId == 0L
                || !ids.contains(parentId)
                || parentId.equals(dept.getId());
            if (isRoot) {
                DeptTreeVO vo = toDeptTreeVO(dept);
                vo.setChildren(buildDeptTreeVO(depts, dept.getId()));
                roots.add(vo);
            }
        }
        return roots;
    }

    private DeptTreeVO toDeptTreeVO(SysDept dept) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setDeptName(dept.getDeptName());
        vo.setSortOrder(dept.getSortOrder());
        vo.setLeader(dept.getLeader());
        vo.setPhone(dept.getPhone());
        vo.setEmail(dept.getEmail());
        vo.setStatus(dept.getStatus());
        Integer dataScope = dept.getDataScope();
        if (dataScope == null) {
            dataScope = dept.getId() != null && dept.getId().equals(SecurityConstants.SYSTEM_DEPT_ID)
                ? SecurityConstants.DATA_SCOPE_ALL
                : SecurityConstants.DATA_SCOPE_DEPT;
        }
        vo.setDataScope(dataScope);
        vo.setCreatedAt(dept.getCreatedAt());
        return vo;
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

        if (dept.getParentId() != null && !dept.getParentId().equals(existing.getParentId())) {
            if (!SecurityUtil.isSuperAdmin() && !SecurityUtil.hasPermission("system:dept:move")) {
                throw new BusinessException("无权限调整部门层级");
            }
        }
        if (dept.getSortOrder() != null) {
            Integer oldSort = existing.getSortOrder();
            if (oldSort == null || !dept.getSortOrder().equals(oldSort)) {
                if (!SecurityUtil.isSuperAdmin() && !SecurityUtil.hasPermission("system:dept:sort")) {
                    throw new BusinessException("无权限调整部门排序");
                }
            }
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

        // 检查是否被角色数据权限引用
        Long roleRefCount = roleDeptMapper.countRolesByDeptId(id);
        if (roleRefCount != null && roleRefCount > 0) {
            throw new BusinessException("该部门已被角色数据权限引用，请先在角色授权中解除关联后再删除");
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
                        userService.deleteUsersWithReason(userIds, operatorId, reason);
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
    @Transactional
    public void move(Long id, Long parentId, Integer sortOrder) {
        if (parentId == null) {
            throw new BusinessException("上级部门不能为空");
        }
        if (parentId == 0L) {
            parentId = SecurityConstants.SYSTEM_DEPT_ID;
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        if (isSystemDept(id)) {
            throw new BusinessException("系统部门不允许调整层级");
        }
        if (parentId.equals(id)) {
            throw new BusinessException("不能将自己设为上级部门");
        }
        if (isDescendant(id, parentId)) {
            throw new BusinessException("不能将子孙部门设为上级部门");
        }
        if (!isSystemDept(parentId)) {
            SysDept parent = deptMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("上级部门不存在");
            }
        }
        Long sameNameCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, parentId)
                .eq(SysDept::getDeptName, dept.getDeptName())
                .ne(SysDept::getId, id)
        );
        if (sameNameCount != null && sameNameCount > 0) {
            throw new BusinessException("同级部门名称已存在");
        }

        Long operatorId = StpUtil.getLoginIdAsLong();
        HashMap<String, Object> before = new HashMap<>();
        before.put("parentId", dept.getParentId());
        before.put("sortOrder", dept.getSortOrder());

        dept.setParentId(parentId);
        if (sortOrder != null) {
            dept.setSortOrder(sortOrder);
        }
        deptMapper.updateById(dept);

        HashMap<String, Object> after = new HashMap<>();
        after.put("parentId", dept.getParentId());
        after.put("sortOrder", dept.getSortOrder());
        operLogService.log(operatorId, null, "dept", id, "move", null, before, after, null);
    }

    @Override
    @Transactional
    public void updateSort(Long id, Integer sortOrder) {
        if (sortOrder == null) {
            throw new BusinessException("排序值不能为空");
        }
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        HashMap<String, Object> before = new HashMap<>();
        before.put("sortOrder", dept.getSortOrder());
        dept.setSortOrder(sortOrder);
        deptMapper.updateById(dept);
        HashMap<String, Object> after = new HashMap<>();
        after.put("sortOrder", dept.getSortOrder());
        operLogService.log(operatorId, null, "dept", id, "sort", null, before, after, null);
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

    @Override
    public void exportDepts(HttpServletResponse response) {
        List<SysDept> depts = listAll();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("id", "部门ID");
        writer.addHeaderAlias("parentId", "上级部门ID");
        writer.addHeaderAlias("deptName", "部门名称");
        writer.addHeaderAlias("sortOrder", "排序");
        writer.addHeaderAlias("leader", "负责人");
        writer.addHeaderAlias("phone", "联系电话");
        writer.addHeaderAlias("email", "邮箱");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("dataScope", "数据范围");
        writer.addHeaderAlias("createdAt", "创建时间");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysDept dept : depts) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", dept.getId());
            row.put("parentId", dept.getParentId());
            row.put("deptName", dept.getDeptName());
            row.put("sortOrder", dept.getSortOrder());
            row.put("leader", dept.getLeader());
            row.put("phone", dept.getPhone());
            row.put("email", dept.getEmail());
            row.put("status", dept.getStatus() != null && dept.getStatus() == 0 ? "正常" : "停用");
            row.put("dataScope", formatDataScope(dept.getDataScope()));
            row.put("createdAt", dept.getCreatedAt());
            rows.add(row);
        }
        writer.write(rows, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            response.setHeader("Content-Disposition", com.campus.wall.util.HttpHeaderUtil.buildContentDisposition("部门列表.xlsx", true));
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    @Override
    @Transactional
    public String importDepts(MultipartFile file, boolean updateExisting) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();
        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            List<Map<String, Object>> rows = reader.readAll();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                try {
                    String deptName = stringValue(row.get("部门名称"));
                    if (!StringUtils.hasText(deptName)) {
                        failCount++;
                        errorMsg.append("第").append(i + 2).append("行：部门名称为空\n");
                        continue;
                    }
                    Long parentId = parseLong(row.get("上级部门ID"));
                    if (parentId == null) {
                        String parentName = stringValue(row.get("上级部门"));
                        if (StringUtils.hasText(parentName)) {
                            SysDept parent = deptMapper.selectOne(
                                new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptName, parentName.trim())
                            );
                            if (parent == null) {
                                failCount++;
                                errorMsg.append("第").append(i + 2).append("行：上级部门不存在\n");
                                continue;
                            }
                            parentId = parent.getId();
                        }
                    }
                    if (parentId == null || parentId == 0L) {
                        parentId = SecurityConstants.SYSTEM_DEPT_ID;
                    }
                    if (!isSystemDept(parentId)) {
                        SysDept parent = deptMapper.selectById(parentId);
                        if (parent == null) {
                            failCount++;
                            errorMsg.append("第").append(i + 2).append("行：上级部门不存在\n");
                            continue;
                        }
                    }
                    Integer sortOrder = parseInteger(row.get("排序"));
                    Integer status = parseStatus(row.get("状态"));
                    Integer dataScope = parseDataScope(row.get("数据范围"));
                    String leader = stringValue(row.get("负责人"));
                    String phone = stringValue(row.get("联系电话"));
                    String email = stringValue(row.get("邮箱"));

                    SysDept existing = deptMapper.selectOne(
                        new LambdaQueryWrapper<SysDept>()
                            .eq(SysDept::getParentId, parentId)
                            .eq(SysDept::getDeptName, deptName.trim())
                    );

                    if (existing != null) {
                        if (!updateExisting) {
                            failCount++;
                            errorMsg.append("第").append(i + 2).append("行：部门已存在\n");
                            continue;
                        }
                        existing.setLeader(StringUtils.hasText(leader) ? leader.trim() : existing.getLeader());
                        existing.setPhone(StringUtils.hasText(phone) ? phone.trim() : existing.getPhone());
                        existing.setEmail(StringUtils.hasText(email) ? email.trim() : existing.getEmail());
                        if (sortOrder != null) existing.setSortOrder(sortOrder);
                        if (status != null) existing.setStatus(status);
                        if (dataScope != null) existing.setDataScope(dataScope);
                        deptMapper.updateById(existing);
                        successCount++;
                    } else {
                        SysDept dept = new SysDept();
                        dept.setParentId(parentId);
                        dept.setDeptName(deptName.trim());
                        dept.setSortOrder(sortOrder != null ? sortOrder : 0);
                        dept.setLeader(StringUtils.hasText(leader) ? leader.trim() : null);
                        dept.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
                        dept.setEmail(StringUtils.hasText(email) ? email.trim() : null);
                        dept.setStatus(status != null ? status : 0);
                        dept.setDataScope(dataScope != null ? dataScope : SecurityConstants.DATA_SCOPE_DEPT);
                        deptMapper.insert(dept);
                        successCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMsg.append("第").append(i + 2).append("行：").append(e.getMessage()).append("\n");
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取文件失败");
        }
        return String.format("导入完成：成功 %d 条，失败 %d 条。%s",
            successCount, failCount, failCount > 0 ? "\n" + errorMsg : "");
    }

    @Override
    public String syncDepts() {
        return "未配置同步源，未执行";
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseStatus(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        switch (str) {
            case "" -> {
                return null;
            }
            case "正常", "启用", "0" -> {
                return 0;
            }
            case "停用", "禁用", "1" -> {
                return 1;
            }
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseDataScope(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty()) {
            return null;
        }
        switch (str) {
            case "全部数据权限":
                return SecurityConstants.DATA_SCOPE_ALL;
            case "自定义数据权限":
                return SecurityConstants.DATA_SCOPE_CUSTOM;
            case "本部门及以下数据权限":
                return SecurityConstants.DATA_SCOPE_DEPT_AND_CHILD;
            case "本部门数据权限":
                return SecurityConstants.DATA_SCOPE_DEPT;
            case "仅本人数据权限":
                return SecurityConstants.DATA_SCOPE_SELF;
            default:
                try {
                    int scope = Integer.parseInt(str);
                    return scope;
                } catch (NumberFormatException e) {
                    return null;
                }
        }
    }

    private String formatDataScope(Integer dataScope) {
        if (dataScope == null) {
            return "-";
        }
        return switch (dataScope) {
            case SecurityConstants.DATA_SCOPE_ALL -> "全部数据权限";
            case SecurityConstants.DATA_SCOPE_CUSTOM -> "自定义数据权限";
            case SecurityConstants.DATA_SCOPE_DEPT -> "本部门数据权限";
            case SecurityConstants.DATA_SCOPE_DEPT_AND_CHILD -> "本部门及以下数据权限";
            case SecurityConstants.DATA_SCOPE_SELF -> "仅本人数据权限";
            default -> String.valueOf(dataScope);
        };
    }
}
