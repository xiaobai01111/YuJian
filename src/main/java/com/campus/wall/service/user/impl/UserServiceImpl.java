package com.campus.wall.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.wall.common.BusinessException;
import com.campus.wall.common.PageResult;
import com.campus.wall.common.ResultCode;
import com.campus.wall.dto.user.UserCreateDTO;
import com.campus.wall.dto.user.UserEditDTO;
import com.campus.wall.dto.user.UserQueryDTO;
import com.campus.wall.dto.user.UserUpdateDTO;
import com.campus.wall.dto.user.UserBatchAssignDTO;
import com.campus.wall.dto.user.UserProfileUpdateDTO;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.system.SysRoleWithUserId;
import com.campus.wall.entity.user.User;
import com.campus.wall.entity.user.IdentityVerification;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.mapper.user.IdentityVerificationMapper;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.service.security.DataScopeService;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.service.user.UserService;
import com.campus.wall.util.PasswordPolicyUtil;
import com.campus.wall.util.SensitiveFieldUtil;
import com.campus.wall.util.ExcelSecurityUtil;
import com.campus.wall.vo.user.UserDetailVO;
import com.campus.wall.vo.user.UserVO;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final IdentityVerificationMapper verificationMapper;
    private final com.campus.wall.service.system.OperLogService operLogService;
    private final PermissionService permissionService;
    private final DataScopeService dataScopeService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public PageResult<UserVO> queryUsers(UserQueryDTO query) {
        LambdaQueryWrapper<User> wrapper = buildUserQuery(query);
        Long operatorId = StpUtil.getLoginIdAsLong();
        applyUserDataScope(wrapper, operatorId);

        if (query.getLastId() != null) {
            wrapper.lt(User::getId, query.getLastId());
            wrapper.orderByDesc(User::getId);
        } else {
            wrapper.orderByDesc(User::getCreatedAt);
        }

        Page<User> page = userMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()), wrapper
        );

        List<User> users = page.getRecords();
        if (users == null || users.isEmpty()) {
            return PageResult.of(List.of(), page.getTotal(), page.getSize(), page.getCurrent());
        }
        Map<Long, List<String>> rolesMap = loadRoleLabelsByUserIds(
            users.stream().map(User::getId).collect(Collectors.toList())
        );
        List<UserVO> records = users.stream()
            .map(user -> toUserVO(user, rolesMap.get(user.getId())))
            .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    @Override
    public PageResult<UserVO> queryDeletedUsers(UserQueryDTO query) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        DataScopeService.DataScope scope = dataScopeService.resolveScope(operatorId);
        List<Long> scopedDeptIds = scope.isAllowAll() ? null : scope.getScopedDeptIds();
        boolean allowSelf = !scope.isAllowAll() && scope.isAllowSelf();
        if (!scope.isAllowAll() && (scopedDeptIds == null || scopedDeptIds.isEmpty()) && !allowSelf) {
            return PageResult.of(List.of(), 0L, query.getSize() == null ? 20L : query.getSize(), 1);
        }

        LocalDateTime loginDateStart = parseDateStart(query.getLoginDateStart());
        LocalDateTime loginDateEnd = parseDateEnd(query.getLoginDateEnd());
        long size = query.getSize() == null ? 20L : query.getSize();
        LocalDateTime lastDeletedAt = parseDateTime(query.getLastDeletedAt());
        Long lastId = query.getLastId();
        boolean hasLastDeletedAt = StringUtils.hasText(query.getLastDeletedAt());
        boolean hasLastId = lastId != null;
        if (hasLastDeletedAt != hasLastId) {
            throw new BusinessException("游标参数不完整");
        }
        if (hasLastDeletedAt && lastDeletedAt == null) {
            throw new BusinessException("lastDeletedAt 格式错误");
        }
        String normalizedPhone = SensitiveFieldUtil.normalizePhone(query.getPhone());
        String phoneHash = SensitiveFieldUtil.hashPhone(normalizedPhone);

        List<User> users = userMapper.selectDeletedUsersAfter(
            query.getUsername(),
            query.getNickname(),
            normalizedPhone,
            phoneHash,
            loginDateStart,
            loginDateEnd,
            lastDeletedAt,
            lastId,
            size,
            scopedDeptIds,
            allowSelf,
            operatorId
        );
        long total = userMapper.countDeletedUsers(
            query.getUsername(),
            query.getNickname(),
            normalizedPhone,
            phoneHash,
            loginDateStart,
            loginDateEnd,
            scopedDeptIds,
            allowSelf,
            operatorId
        );
        if (users == null || users.isEmpty()) {
            return PageResult.of(List.of(), total, size, 1);
        }
        Map<Long, List<String>> rolesMap = loadRoleLabelsByUserIds(
            users.stream().map(User::getId).collect(Collectors.toList())
        );
        List<UserVO> records = users.stream()
            .map(user -> toUserVO(user, rolesMap.get(user.getId())))
            .collect(Collectors.toList());
        return PageResult.of(records, total, size, 1);
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        ensureCanAccessUser(operatorId, userId, "无权查看该用户");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        UserDetailVO vo = new UserDetailVO();
        BeanUtils.copyProperties(user, vo);

        // 获取用户角色ID列表
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        vo.setRoleIds(roleIds == null ? List.of() : roleIds);

        if (user.getVerifyStatus() != null && user.getVerifyStatus() == 0) {
            IdentityVerification rejected = verificationMapper.selectOne(
                new LambdaQueryWrapper<IdentityVerification>()
                    .eq(IdentityVerification::getUserId, userId)
                    .eq(IdentityVerification::getStatus, 2)
                    .orderByDesc(IdentityVerification::getReviewedAt)
                    .last("LIMIT 1")
            );
            if (rejected != null) {
                vo.setVerifyRejectReason(rejected.getRejectReason());
            }
        }

        return vo;
    }

    @Override
    public UserVO getUserById(Long userId) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        ensureCanAccessUser(operatorId, userId, "无权查看该用户");
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return toUserVO(user);
    }

    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (dto.getEmail() != null) {
            String email = SensitiveFieldUtil.normalizeEmail(dto.getEmail());
            ensureEmailUnique(userId, email);
            applyEmail(user, email);
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }

        int updated = userMapper.updateById(user);
        if (updated == 0) {
            throw new BusinessException("更新用户失败，请重试");
        }
    }

    @Override
    public void updateProfile(Long userId, UserProfileUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) {
            String email = SensitiveFieldUtil.normalizeEmail(dto.getEmail());
            ensureEmailUnique(userId, email);
            applyEmail(user, email);
        }
        if (dto.getPhone() != null) {
            applyPhone(user, dto.getPhone());
        }
        if (dto.getSex() != null) {
            user.setSex(dto.getSex());
        }

        int updated = userMapper.updateById(user);
        if (updated == 0) {
            throw new BusinessException("更新个人资料失败，请重试");
        }
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        updateUserStatusWithReason(userId, status, null, null);
    }

    @Override
    @Transactional
    public void updateUserStatusWithReason(Long userId, Integer status, String reason, Long operatorId) {
        Long effectiveOperatorId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        ensureCanAccessUser(effectiveOperatorId, userId, "无权操作该用户");
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 禁止操作管理员账号
        if (isSystemAdminUserId(user.getId())) {
            throw new BusinessException("管理员账号不允许操作");
        }
        // 禁止操作自己
        if (effectiveOperatorId.equals(userId)) {
            throw new BusinessException("不能操作自己的账号");
        }
        // 封禁时必须提供理由
        if (status == 1 && (reason == null || reason.trim().isEmpty())) {
            throw new BusinessException("封禁用户必须提供理由");
        }

        Integer oldStatus = user.getStatus();
        int updated = userMapper.updateStatus(userId, status);
        if (updated == 0) {
            throw new BusinessException("更新用户状态失败，请重试");
        }

        // 记录审计日志
        String action = status == 1 ? "ban" : "unban";
        operLogService.log(effectiveOperatorId, null, "user", userId, action, reason,
                java.util.Map.of("status", oldStatus), java.util.Map.of("status", status), null);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        ensureCanAccessUser(operatorId, userId, "无权操作该用户");
        ensureAssignableRoles(roleIds);
        doAssignRoles(userId, roleIds);
    }

    @Override
    @Transactional
    public void batchAssignRoles(List<Long> userIds, List<Long> roleIds) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        ensureAssignableRoles(roleIds);
        for (Long userId : userIds) {
            ensureCanAccessUser(operatorId, userId, "无权操作该用户");
            doAssignRoles(userId, roleIds);
        }
    }

    private void doAssignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.deleteByUserId(userId);
        // 添加新角色
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
        permissionService.clearUserCache(userId);
        // 记录审计日志
        operLogService.log("user", userId, "role_assign", null);
    }

    @Override
    public int batchAssignByQuery(UserBatchAssignDTO dto, Long operatorId) {
        Long effectiveOperatorId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        if (dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            throw new BusinessException("请选择要分配的角色");
        }

        String roleMode = dto.getRoleMode() == null ? "REPLACE" : dto.getRoleMode().toUpperCase();
        int affected = 0;
        int batchSize = 200;
        Long lastId = null;
        List<Long> roleIds = dto.getRoleIds().stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            throw new BusinessException("请选择要分配的角色");
        }
        ensureAssignableRoles(roleIds);

        while (true) {
            LambdaQueryWrapper<User> wrapper = buildUserQuery(dto);
            applyUserDataScope(wrapper, effectiveOperatorId);
            if (lastId != null) {
                wrapper.lt(User::getId, lastId);
            }
            wrapper.orderByDesc(User::getId);
            wrapper.select(User::getId, User::getDeptId);

            Page<User> page = userMapper.selectPage(new Page<>(1, batchSize), wrapper);
            List<User> users = page.getRecords();
            if (users == null || users.isEmpty()) {
                break;
            }

            List<Long> batchUserIds = users.stream()
                .map(User::getId)
                .filter(userId -> !isSystemAdminUserId(userId))
                .collect(Collectors.toList());
            if (!batchUserIds.isEmpty()) {
                if ("ADD".equals(roleMode)) {
                    List<SysUserRole> existing = userRoleMapper.selectByUserIds(batchUserIds);
                    Set<String> existingPairs = new java.util.HashSet<>();
                    if (existing != null) {
                        for (SysUserRole userRole : existing) {
                            existingPairs.add(userRole.getUserId() + ":" + userRole.getRoleId());
                        }
                    }
                    List<SysUserRole> insertList = new java.util.ArrayList<>();
                    for (Long userId : batchUserIds) {
                        for (Long roleId : roleIds) {
                            String key = userId + ":" + roleId;
                            if (existingPairs.contains(key)) {
                                continue;
                            }
                            SysUserRole userRole = new SysUserRole();
                            userRole.setUserId(userId);
                            userRole.setRoleId(roleId);
                            insertList.add(userRole);
                        }
                    }
                    if (!insertList.isEmpty()) {
                        userRoleMapper.batchInsert(insertList);
                    }
                } else {
                    userRoleMapper.deleteByUserIds(batchUserIds);
                    List<SysUserRole> insertList = new java.util.ArrayList<>();
                    for (Long userId : batchUserIds) {
                        for (Long roleId : roleIds) {
                            SysUserRole userRole = new SysUserRole();
                            userRole.setUserId(userId);
                            userRole.setRoleId(roleId);
                            insertList.add(userRole);
                        }
                    }
                    if (!insertList.isEmpty()) {
                        userRoleMapper.batchInsert(insertList);
                    }
                }

                for (Long userId : batchUserIds) {
                    permissionService.clearUserCache(userId);
                }
                affected += batchUserIds.size();
            }

            lastId = users.getLast().getId();
        }

        operLogService.log(effectiveOperatorId, null, "user", null, "batch_assign",
                "批量分配用户，影响 " + affected + " 人", null, null, null);
        return affected;
    }

    private LambdaQueryWrapper<User> buildUserQuery(UserQueryDTO query) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return wrapper;
        }

        String username = trimToNull(query.getUsername());
        String nickname = trimToNull(query.getNickname());
        String phone = trimToNull(query.getPhone());
        String phoneHash = SensitiveFieldUtil.hashPhone(phone);
        Long roleId = query.getRoleId();

        wrapper.like(StringUtils.hasText(username), User::getUsername, username)
            .like(StringUtils.hasText(nickname), User::getNickname, nickname)
            .and(StringUtils.hasText(phone), w -> w.eq(User::getPhoneHash, phoneHash).or().eq(User::getPhone, phone))
            .eq(query.getStatus() != null, User::getStatus, query.getStatus())
            .eq(query.getVerifyStatus() != null, User::getVerifyStatus, query.getVerifyStatus())
            .eq(query.getUserType() != null, User::getUserType, query.getUserType())
            .eq(query.getDeptId() != null, User::getDeptId, query.getDeptId());

        if (roleId != null) {
            wrapper.exists(
                "select 1 from sys_user_roles ur where ur.user_id = users.id and ur.role_id = {0}",
                roleId
            );
        }

        applyLoginDateRange(wrapper, query.getLoginDateStart(), query.getLoginDateEnd());
        return wrapper;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyLoginDateRange(LambdaQueryWrapper<User> wrapper, String start, String end) {
        LocalDateTime startTime = parseDateStart(start);
        LocalDateTime endTime = parseDateEnd(end);
        if (startTime != null) {
            wrapper.ge(User::getLoginDate, startTime);
        }
        if (endTime != null) {
            wrapper.le(User::getLoginDate, endTime);
        }
    }

    private LocalDateTime parseDateStart(String start) {
        if (!StringUtils.hasText(start)) {
            return null;
        }
        try {
            LocalDate startDate = LocalDate.parse(start);
            return startDate.atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateEnd(String end) {
        if (!StringUtils.hasText(end)) {
            return null;
        }
        try {
            LocalDate endDate = LocalDate.parse(end);
            return endDate.plusDays(1).atStartOfDay().minusNanos(1);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void updateCreditScore(Long userId, int delta, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        int newScore = Math.max(0, Math.min(100, user.getCreditScore() + delta));
        userMapper.updateCreditScore(userId, newScore);
    }

    @Override
    @Transactional
    public Long createUser(UserCreateDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        PasswordPolicyUtil.validateOrThrow(dto.getPassword());
        Long operatorId = StpUtil.getLoginIdAsLong();
        boolean operatorIsSuperAdmin = isSystemAdminUserId(operatorId);
        int targetUserType = resolveUserType(dto.getUserType());
        if (targetUserType == 1 && !operatorIsSuperAdmin) {
            throw new BusinessException("仅超级管理员可创建管理员账号");
        }

        Long targetRoleId = dto.getRoleId();
        SysRole targetRole = null;
        if (targetRoleId != null) {
            ensureAssignableRoles(List.of(targetRoleId));
            targetRole = sysRoleMapper.selectById(targetRoleId);
            if (targetRole == null) {
                throw new BusinessException("角色不存在");
            }
            validateUserTypeRoleConsistency(targetUserType, targetRole);
        } else if (targetUserType == 1) {
            throw new BusinessException("管理员账号必须分配超级管理员角色");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname());
        String email = SensitiveFieldUtil.normalizeEmail(dto.getEmail());
        ensureEmailUnique(null, email);
        applyEmail(user, email);
        applyPhone(user, dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setUserType(targetUserType);
        user.setSex(dto.getSex() != null ? dto.getSex() : 0);
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        user.setRemark(dto.getRemark());
        user.setVerifyStatus(0);
        user.setCreditScore(100);

        userMapper.insert(user);

        // 创建时分配角色
        if (targetRoleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(targetRoleId);
            userRoleMapper.insert(userRole);
        }

        // 记录审计日志
        operLogService.log("user", user.getId(), "create", null);

        return user.getId();
    }

    @Override
    public void editUser(Long userId, UserEditDTO dto) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        ensureCanAccessUser(operatorId, userId, "无权操作该用户");
        boolean operatorIsSuperAdmin = isSystemAdminUserId(operatorId);
        boolean targetIsSuperAdmin = isSystemAdminUserId(userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (targetIsSuperAdmin) {
            if (dto.getDeptId() != null) {
                throw new BusinessException("管理员账号不允许调整部门或用户类型");
            }
        }

        user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) {
            String email = SensitiveFieldUtil.normalizeEmail(dto.getEmail());
            ensureEmailUnique(userId, email);
            applyEmail(user, email);
        }
        if (dto.getPhone() != null) {
            applyPhone(user, dto.getPhone());
        }
        if (!targetIsSuperAdmin && dto.getDeptId() != null) {
            user.setDeptId(dto.getDeptId());
        }
        if (dto.getUserType() != null) {
            if (!operatorIsSuperAdmin) {
                throw new BusinessException("仅超级管理员可修改用户类型");
            }
            int targetUserType = resolveUserType(dto.getUserType());
            if (targetUserType == 1 && !targetIsSuperAdmin) {
                throw new BusinessException("提升为管理员前请先分配超级管理员角色");
            }
            if (targetUserType == 0 && targetIsSuperAdmin) {
                throw new BusinessException("超级管理员账号不能降级为普通用户");
            }
            user.setUserType(targetUserType);
        }
        if (dto.getSex() != null) user.setSex(dto.getSex());
        if (dto.getRemark() != null) user.setRemark(dto.getRemark());

        userMapper.updateById(user);

        // 记录审计日志
        operLogService.log("user", userId, "edit", null);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> userIds) {
        deleteUsersWithReason(userIds, null, null);
    }

    @Override
    @Transactional
    public void deleteUsersWithReason(List<Long> userIds, Long operatorId, String reason) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Long effectiveOperatorId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();

        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user == null) continue;
            ensureCanAccessUser(effectiveOperatorId, userId, "无权删除该用户");

            // 禁止删除系统管理员
            if (isSystemAdminUserId(user.getId())) {
                throw new BusinessException("不能删除管理员账号");
            }
            // 禁止删除自己
            if (effectiveOperatorId.equals(userId)) {
                throw new BusinessException("不能删除自己的账号");
            }

            // 软删除：使用原生SQL绕过@TableLogic
            userMapper.softDeleteById(userId, java.time.LocalDateTime.now(), effectiveOperatorId, reason);

            // 记录审计日志
            operLogService.log(effectiveOperatorId, null, "user", userId, "delete", reason,
                    java.util.Map.of("username", user.getUsername()), null, null);
        }
    }

    @Override
    @Transactional
    public void restoreUser(Long userId, Long operatorId) {
        restoreUser(userId, operatorId, null);
    }

    @Override
    @Transactional
    public void restoreUser(Long userId, Long operatorId, String reason) {
        Long effectiveOperatorId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        // 使用原生SQL查询已删除的用户（绕过@TableLogic）
        User user = userMapper.selectDeletedById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或未被删除");
        }
        ensureCanAccessUser(effectiveOperatorId, userId, "无权恢复该用户");

        // 恢复用户
        user.setDeleted(0);
        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setDeletedReason(null);
        int restored = userMapper.restoreById(userId);
        if (restored == 0) {
            throw new BusinessException("恢复用户失败，请重试");
        }

        // 记录审计日志
        operLogService.log(effectiveOperatorId, null, "user", userId, "restore", reason,
                java.util.Map.of("deleted", 1), java.util.Map.of("deleted", 0), null);
    }

    @Override
    @Transactional
    public void purgeUser(Long userId, Long operatorId, String reason) {
        Long effectiveOperatorId = operatorId != null ? operatorId : StpUtil.getLoginIdAsLong();
        User user = userMapper.selectDeletedById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在或未被删除");
        }
        ensureCanAccessUser(effectiveOperatorId, userId, "无权彻底删除该用户");
        if (isSystemAdminUserId(userId)) {
            throw new BusinessException("管理员账号不允许彻底删除");
        }

        int deleted = userMapper.hardDeleteById(userId);
        if (deleted == 0) {
            throw new BusinessException("彻底删除失败，请重试");
        }
        permissionService.clearUserCache(userId);
        operLogService.log(effectiveOperatorId, null, "user", userId, "purge", reason,
                java.util.Map.of("username", user.getUsername()), null, null);
    }

    @Override
    public void exportUsers(UserQueryDTO query, HttpServletResponse response) {
        // 查询所有用户（不分页）
        Long operatorId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<User> wrapper = buildUserQuery(query);
        applyUserDataScope(wrapper, operatorId);
        wrapper.orderByDesc(User::getCreatedAt);
        List<User> users = userMapper.selectList(wrapper);

        // 创建Excel
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("username", "用户名");
        writer.addHeaderAlias("nickname", "昵称");
        writer.addHeaderAlias("email", "邮箱");
        writer.addHeaderAlias("phone", "手机号");
        writer.addHeaderAlias("deptName", "部门");
        writer.addHeaderAlias("userType", "用户类型");
        writer.addHeaderAlias("sex", "性别");
        writer.addHeaderAlias("status", "状态");
        writer.addHeaderAlias("createdAt", "创建时间");

        // 转换数据
        List<Map<String, Object>> rows = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("username", ExcelSecurityUtil.escapeFormula(user.getUsername()));
            row.put("nickname", ExcelSecurityUtil.escapeFormula(user.getNickname()));
            row.put("email", ExcelSecurityUtil.escapeFormula(user.getEmail()));
            row.put("phone", ExcelSecurityUtil.escapeFormula(user.getPhone()));
            row.put("userType", user.getUserType() == 1 ? "管理员" : "普通用户");
            row.put("sex", user.getSex() == 1 ? "男" : (user.getSex() == 2 ? "女" : "未知"));
            row.put("status", user.getStatus() == 0 ? "正常" : "停用");
            row.put("createdAt", user.getCreatedAt());
            rows.add(row);
        }
        writer.write(rows, true);

        // 输出响应
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            response.setHeader("Content-Disposition", com.campus.wall.util.HttpHeaderUtil.buildContentDisposition("用户列表.xlsx", true));
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    @Override
    public String importUsers(MultipartFile file, boolean updateExisting) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();
        int batchSize = 200;
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
            List<Map<String, Object>> rows = reader.readAll();

            for (int start = 0; start < rows.size(); start += batchSize) {
                int end = Math.min(rows.size(), start + batchSize);
                List<ImportUserRow> batchRows = new ArrayList<>();

                for (int i = start; i < end; i++) {
                    Map<String, Object> row = rows.get(i);
                    try {
                        String username = normalizeCell(row.get("用户名"));
                        String nickname = normalizeCell(row.get("昵称"));
                        String password = normalizeCell(row.get("密码"));
                        String email = SensitiveFieldUtil.normalizeEmail(normalizeCell(row.get("邮箱")));
                        String phone = SensitiveFieldUtil.normalizePhone(normalizeCell(row.get("手机号")));

                        if (!StringUtils.hasText(username) || !StringUtils.hasText(nickname)) {
                            failCount++;
                            errorMsg.append("第").append(i + 2).append("行：用户名或昵称为空\n");
                            continue;
                        }
                        batchRows.add(new ImportUserRow(i + 2, username, nickname, password, email, phone));
                    } catch (Exception e) {
                        failCount++;
                        errorMsg.append("第").append(i + 2).append("行：").append(e.getMessage()).append("\n");
                    }
                }

                if (batchRows.isEmpty()) {
                    continue;
                }

                BatchImportResult batchResult = txTemplate.execute(status -> {
                    int batchSuccess = 0;
                    int batchFail = 0;
                    StringBuilder batchErrors = new StringBuilder();

                    List<String> usernames = batchRows.stream()
                        .map(ImportUserRow::username)
                        .distinct()
                        .collect(Collectors.toList());
                    List<User> existingUsers = userMapper.selectList(
                        new LambdaQueryWrapper<User>().in(User::getUsername, usernames)
                    );
                    Map<String, User> existingMap = existingUsers.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getUsername, user -> user, (a, b) -> a));

                    Map<String, ImportUserInsert> pendingInserts = new LinkedHashMap<>();
                    List<User> updateList = new ArrayList<>();

                    for (ImportUserRow row : batchRows) {
                        User existing = existingMap.get(row.username());
                        if (existing != null) {
                            if (updateExisting) {
                                applyUserImportUpdate(existing, row);
                                updateList.add(existing);
                                batchSuccess++;
                            } else {
                                batchFail++;
                                batchErrors.append("第").append(row.rowIndex()).append("行：用户名已存在\n");
                            }
                            continue;
                        }

                        ImportUserInsert pending = pendingInserts.get(row.username());
                        if (pending != null) {
                            if (updateExisting) {
                                applyUserImportUpdate(pending.user(), row);
                                pending.rowIndices().add(row.rowIndex());
                            } else {
                                batchFail++;
                                batchErrors.append("第").append(row.rowIndex()).append("行：用户名已存在\n");
                            }
                            continue;
                        }

                        if (!StringUtils.hasText(row.password())) {
                            batchFail++;
                            batchErrors.append("第").append(row.rowIndex()).append("行：新增用户密码不能为空\n");
                            continue;
                        }
                        if (!PasswordPolicyUtil.isValid(row.password())) {
                            batchFail++;
                            batchErrors.append("第").append(row.rowIndex())
                                .append("行：密码需为8-32位，且包含大小写字母、数字和特殊字符\n");
                            continue;
                        }

                        User user = new User();
                        user.setUsername(row.username());
                        user.setNickname(row.nickname());
                        user.setPassword(BCrypt.hashpw(row.password()));
                        applyEmail(user, row.email());
                        applyPhone(user, row.phone());
                        user.setVerifyStatus(0);
                        user.setStatus(0);
                        user.setCreditScore(100);
                        pendingInserts.put(row.username(), new ImportUserInsert(user, new ArrayList<>(List.of(row.rowIndex()))));
                    }

                    for (User updateUser : updateList) {
                        userMapper.updateById(updateUser);
                    }

                    if (!pendingInserts.isEmpty()) {
                        List<ImportUserInsert> insertCandidates = new ArrayList<>(pendingInserts.values());
                        List<User> insertUsers = insertCandidates.stream().map(ImportUserInsert::user).collect(Collectors.toList());
                        try {
                            userMapper.batchInsert(insertUsers);
                            batchSuccess += insertCandidates.stream().mapToInt(candidate -> candidate.rowIndices().size()).sum();
                        } catch (Exception ex) {
                            for (ImportUserInsert candidate : insertCandidates) {
                                try {
                                    userMapper.insert(candidate.user());
                                    batchSuccess += candidate.rowIndices().size();
                                } catch (Exception e) {
                                    batchFail += candidate.rowIndices().size();
                                    for (Integer rowIndex : candidate.rowIndices()) {
                                        batchErrors.append("第").append(rowIndex).append("行：").append(e.getMessage()).append("\n");
                                    }
                                }
                            }
                        }
                    }

                    return new BatchImportResult(batchSuccess, batchFail, batchErrors.toString());
                });

                if (batchResult != null) {
                    successCount += batchResult.successCount();
                    failCount += batchResult.failCount();
                    if (StringUtils.hasText(batchResult.errorMsg())) {
                        errorMsg.append(batchResult.errorMsg());
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException("读取文件失败");
        }

        return String.format("导入完成：成功 %d 条，失败 %d 条。%s", 
            successCount, failCount, failCount > 0 ? "\n" + errorMsg : "");
    }

    private void applyUserImportUpdate(User user, ImportUserRow row) {
        user.setNickname(row.nickname());
        if (row.email() != null) {
            applyEmail(user, row.email());
        }
        if (row.phone() != null) {
            applyPhone(user, row.phone());
        }
    }

    private String normalizeCell(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private record ImportUserRow(int rowIndex, String username, String nickname, String password, String email, String phone) {
    }

    private record ImportUserInsert(User user, List<Integer> rowIndices) {
    }

    private record BatchImportResult(int successCount, int failCount, String errorMsg) {
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeHeadRow(CollUtil.newArrayList("用户名", "昵称", "密码", "邮箱", "手机号"));
        // 写入示例数据
        writer.writeRow(CollUtil.newArrayList("user01", "张三", "Aa@2026xy", "user01@example.com", "13800138000"));

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        try {
            response.setHeader("Content-Disposition", com.campus.wall.util.HttpHeaderUtil.buildContentDisposition("用户导入模板.xlsx", true));
            writer.flush(response.getOutputStream(), true);
            writer.close();
        } catch (IOException e) {
            throw new BusinessException("下载模板失败");
        }
    }

    private UserVO toUserVO(User user) {
        Objects.requireNonNull(user, "用户不能为空");
        List<SysRole> roles = sysRoleMapper.selectAllRolesByUserId(user.getId());
        List<String> roleLabels = roles == null ? List.of() : roles.stream()
            .map(role -> formatRoleLabel(role.getRoleName(), role.getStatus()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return toUserVO(user, roleLabels);
    }

    private UserVO toUserVO(User user, List<String> roleLabels) {
        Objects.requireNonNull(user, "用户不能为空");
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (roleLabels != null && !roleLabels.isEmpty()) {
            vo.setRoles(roleLabels);
        }
        return vo;
    }

    private Map<Long, List<String>> loadRoleLabelsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<SysRoleWithUserId> rows = sysRoleMapper.selectRolesByUserIds(userIds);
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return rows.stream()
            .collect(Collectors.groupingBy(
                SysRoleWithUserId::getUserId,
                Collectors.mapping(
                    row -> formatRoleLabel(row.getRoleName(), row.getStatus()),
                    Collectors.filtering(Objects::nonNull, Collectors.toList())
                )
            ));
    }

    private String formatRoleLabel(String roleName, Integer status) {
        if (roleName == null) {
            return null;
        }
        if (status != null && status == 1) {
            return roleName + "(已禁用)";
        }
        return roleName;
    }

    private void ensureCanAccessUser(Long operatorId, Long targetUserId, String message) {
        if (operatorId == null || targetUserId == null) {
            throw new BusinessException(ResultCode.FORBIDDEN, message);
        }
        if (!dataScopeService.canAccessUser(operatorId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, message);
        }
    }

    private void applyUserDataScope(LambdaQueryWrapper<User> wrapper, Long operatorId) {
        DataScopeService.DataScope scope = dataScopeService.resolveScope(operatorId);
        if (scope.isAllowAll()) {
            return;
        }
        List<Long> scopedDeptIds = scope.getScopedDeptIds();
        boolean hasDeptScope = scopedDeptIds != null && !scopedDeptIds.isEmpty();
        boolean allowSelf = scope.isAllowSelf() && operatorId != null;
        if (hasDeptScope && allowSelf) {
            wrapper.and(w -> w.in(User::getDeptId, scopedDeptIds).or().eq(User::getId, operatorId));
            return;
        }
        if (hasDeptScope) {
            wrapper.in(User::getDeptId, scopedDeptIds);
            return;
        }
        if (allowSelf) {
            wrapper.eq(User::getId, operatorId);
            return;
        }
        wrapper.eq(User::getId, -1L);
    }

    private boolean isSystemAdminUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleKeys = sysRoleMapper.selectRoleKeysByUserId(userId);
        return roleKeys != null && roleKeys.contains(com.campus.wall.util.SecurityUtil.getSuperAdminRoleKey());
    }

    private int resolveUserType(Integer userType) {
        int resolved = userType == null ? 0 : userType;
        if (resolved != 0 && resolved != 1) {
            throw new BusinessException("用户类型非法");
        }
        return resolved;
    }

    private void validateUserTypeRoleConsistency(int userType, SysRole role) {
        String superAdminKey = com.campus.wall.util.SecurityUtil.getSuperAdminRoleKey();
        boolean isSuperAdminRole = role != null && superAdminKey.equals(role.getRoleKey());
        if (userType == 1 && !isSuperAdminRole) {
            throw new BusinessException("管理员账号必须绑定超级管理员角色");
        }
        if (userType != 1 && isSuperAdminRole) {
            throw new BusinessException("普通账号不能绑定超级管理员角色");
        }
    }

    private void applyEmail(User user, String email) {
        String normalized = SensitiveFieldUtil.normalizeEmail(email);
        user.setEmail(normalized);
        user.setEmailHash(SensitiveFieldUtil.hashEmail(normalized));
    }

    private void applyPhone(User user, String phone) {
        String normalized = SensitiveFieldUtil.normalizePhone(phone);
        user.setPhone(normalized);
        user.setPhoneHash(SensitiveFieldUtil.hashPhone(normalized));
    }

    private void ensureEmailUnique(Long userId, String email) {
        String normalized = SensitiveFieldUtil.normalizeEmail(email);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        String emailHash = SensitiveFieldUtil.hashEmail(normalized);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getDeleted, 0)
            .and(w -> w.eq(User::getEmailHash, emailHash)
                .or()
                .eq(User::getEduEmailHash, emailHash)
                .or()
                .eq(User::getEmail, normalized)
                .or()
                .eq(User::getEduEmail, normalized));
        if (userId != null) {
            wrapper.ne(User::getId, userId);
        }
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("邮箱已被使用");
        }
    }

    private void ensureAssignableRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (isSystemAdminUserId(operatorId)) {
            return;
        }

        List<SysRole> targetRoles = sysRoleMapper.selectBatchIds(roleIds);
        if (targetRoles == null || targetRoles.isEmpty()) {
            throw new BusinessException("角色不存在");
        }
        String superAdminKey = com.campus.wall.util.SecurityUtil.getSuperAdminRoleKey();
        for (SysRole role : targetRoles) {
            if (role != null && superAdminKey.equals(role.getRoleKey())) {
                throw new BusinessException("不能分配管理员角色");
            }
        }

        List<SysRole> operatorRoles = sysRoleMapper.selectRolesByUserId(operatorId);
        Set<Long> allowedMenuIds = new HashSet<>();
        if (operatorRoles != null) {
            for (SysRole role : operatorRoles) {
                if (role == null || role.getId() == null) {
                    continue;
                }
                List<Long> roleMenuIds = sysMenuMapper.selectMenuIdsByRoleId(role.getId());
                if (roleMenuIds != null) {
                    allowedMenuIds.addAll(roleMenuIds);
                }
            }
        }
        if (allowedMenuIds.isEmpty()) {
            throw new BusinessException("不能分配超出当前账号权限范围的角色");
        }
        for (SysRole role : targetRoles) {
            if (role == null || role.getId() == null) {
                continue;
            }
            List<Long> roleMenuIds = sysMenuMapper.selectMenuIdsByRoleId(role.getId());
            if (roleMenuIds == null) {
                continue;
            }
            boolean overGranted = roleMenuIds.stream().anyMatch(menuId -> menuId != null && !allowedMenuIds.contains(menuId));
            if (overGranted) {
                throw new BusinessException("不能分配超出当前账号权限范围的角色");
            }
        }
    }
}
