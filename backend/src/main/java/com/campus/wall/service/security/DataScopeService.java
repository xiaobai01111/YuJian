package com.campus.wall.service.security;

import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.util.SecurityUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeService {

    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysDeptMapper deptMapper;
    private final UserMapper userMapper;

    public DataScope resolveScope(Long userId) {
        if (userId == null) {
            return DataScope.selfOnly();
        }
        if (SecurityUtil.isSuperAdmin()) {
            return DataScope.all();
        }

        List<Long> deptIds = sysRoleDeptMapper.selectDeptIdsByUserId(userId);
        if (deptIds == null) {
            deptIds = new ArrayList<>();
        }
        if (deptIds.isEmpty()) {
            return DataScope.selfOnly();
        }

        List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
        boolean allowAll = false;
        boolean allowSelf = false;
        boolean includeChildren = false;
        List<Long> allowDeptIds = new ArrayList<>();

        for (SysDept dept : depts) {
            if (dept == null) {
                continue;
            }
            Integer scope = dept.getDataScope() != null ? dept.getDataScope() : SecurityConstants.DATA_SCOPE_DEPT;
            if (scope == SecurityConstants.DATA_SCOPE_ALL) {
                allowAll = true;
                break;
            }
            if (scope == SecurityConstants.DATA_SCOPE_SELF) {
                allowSelf = true;
            } else if (scope == SecurityConstants.DATA_SCOPE_DEPT) {
                allowDeptIds.add(dept.getId());
            } else if (scope == SecurityConstants.DATA_SCOPE_DEPT_AND_CHILD) {
                allowDeptIds.add(dept.getId());
                includeChildren = true;
            } else if (scope == SecurityConstants.DATA_SCOPE_CUSTOM) {
                allowDeptIds.add(dept.getId());
            }
        }

        if (allowAll) {
            return DataScope.all();
        }

        List<Long> scopedDeptIds = includeChildren ? expandDeptIds(allowDeptIds) : allowDeptIds;
        return new DataScope(false, allowSelf, scopedDeptIds);
    }

    public boolean canAccessUser(Long operatorId, Long targetUserId) {
        if (operatorId == null || targetUserId == null) {
            return false;
        }
        DataScope scope = resolveScope(operatorId);
        if (scope.isAllowAll()) {
            return true;
        }
        if (scope.isAllowSelf() && Objects.equals(operatorId, targetUserId)) {
            return true;
        }
        if (scope.getScopedDeptIds().isEmpty()) {
            return false;
        }
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null || targetUser.getDeptId() == null) {
            return false;
        }
        return scope.getScopedDeptIds().contains(targetUser.getDeptId());
    }

    public String buildUserInSql(DataScope scope) {
        if (scope == null || scope.isAllowAll()) {
            return null;
        }
        if (scope.getScopedDeptIds().isEmpty()) {
            return null;
        }
        String deptIdSql = scope.getScopedDeptIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return "SELECT id FROM users WHERE deleted = 0 AND dept_id IN (" + deptIdSql + ")";
    }

    public String buildPostInSql(DataScope scope, Long userId) {
        if (scope == null || scope.isAllowAll()) {
            return null;
        }
        String userInSql = buildUserInSql(scope);
        if (userInSql == null || userInSql.isEmpty()) {
            return "SELECT id FROM posts WHERE user_id = " + userId;
        }
        return "SELECT id FROM posts WHERE user_id IN (" + userInSql + ")";
    }

    private List<Long> expandDeptIds(List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<SysDept> allDepts = deptMapper.selectList(null);
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SysDept dept : allDepts) {
            childrenMap.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        for (Long rootId : deptIds) {
            collectDeptChildren(rootId, childrenMap, result);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private void collectDeptChildren(Long deptId, Map<Long, List<Long>> childrenMap, List<Long> result) {
        if (deptId == null) {
            return;
        }
        result.add(deptId);
        List<Long> children = childrenMap.get(deptId);
        if (children != null) {
            for (Long childId : children) {
                collectDeptChildren(childId, childrenMap, result);
            }
        }
    }

    @Getter
    public static class DataScope {
        private final boolean allowAll;
        private final boolean allowSelf;
        private final List<Long> scopedDeptIds;

        private DataScope(boolean allowAll, boolean allowSelf, List<Long> scopedDeptIds) {
            this.allowAll = allowAll;
            this.allowSelf = allowSelf;
            this.scopedDeptIds = scopedDeptIds != null ? scopedDeptIds : List.of();
        }

        public static DataScope all() {
            return new DataScope(true, false, List.of());
        }

        public static DataScope selfOnly() {
            return new DataScope(false, true, List.of());
        }
    }
}
