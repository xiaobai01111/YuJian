package com.campus.wall.service.system.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.wall.common.BusinessException;
import com.campus.wall.config.SecurityProperties;
import com.campus.wall.config.StorageProperties;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.dto.system.SetupInitDTO;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysRoleDept;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.entity.system.SysSiteSetting;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.system.SysMenu;
import com.campus.wall.entity.system.SysApiPermission;
import com.campus.wall.entity.user.User;
import com.campus.wall.enums.file.StorageProviderType;
import com.campus.wall.mapper.system.SysApiPermissionMapper;
import com.campus.wall.mapper.system.SysDeptMapper;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleDeptMapper;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.mapper.system.SysSiteSettingMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.service.system.SetupService;
import com.campus.wall.vo.system.SetupStatusVO;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetupServiceImpl implements SetupService {

    private static final long SETUP_LOCK_KEY = 20260126L;

    private final SysSiteSettingMapper siteSettingMapper;
    private final UserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysApiPermissionMapper apiPermissionMapper;
    private final StorageProperties storageProperties;
    private final SecurityProperties securityProperties;
    private final PermissionService permissionService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public SetupStatusVO getStatus() {
        SysSiteSetting setting = siteSettingMapper.selectOne(new LambdaQueryWrapper<SysSiteSetting>()
                .orderByDesc(SysSiteSetting::getId)
                .last("LIMIT 1"));
        SetupStatusVO vo = new SetupStatusVO();
        if (setting != null && Boolean.TRUE.equals(setting.getSetupCompleted())) {
            vo.setSetupCompleted(true);
            vo.setSiteName(setting.getSiteName());
            vo.setStorageProvider(setting.getStorageProvider());
            vo.setLocalPath(setting.getLocalPath());
            vo.setLocalPublicEnabled(setting.getLocalPublicEnabled());
        } else {
            vo.setSetupCompleted(false);
            vo.setStorageProvider(storageProperties.getPrimaryProvider().getCode());
            vo.setLocalPath(storageProperties.getLocalPath());
            vo.setLocalPublicEnabled(storageProperties.isLocalPublicEnabled());
        }
        return vo;
    }

    @Override
    @Transactional
    public void initialize(SetupInitDTO dto) {
        acquireSetupLock();
        if (isSetupCompleted()) {
            throw new BusinessException("系统已完成初始化");
        }
        if (!StringUtils.hasText(dto.getAdminPassword()) || !dto.getAdminPassword().equals(dto.getAdminConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }

        SysRole adminRole = roleMapper.selectByRoleKey(securityProperties.getSuperAdminRoleKey());
        if (adminRole == null) {
            throw new BusinessException("系统角色未初始化，请先执行SQL");
        }
        SysDept systemDept = deptMapper.selectById(SecurityConstants.SYSTEM_DEPT_ID);
        if (systemDept == null) {
            throw new BusinessException("系统部门未初始化，请先执行SQL");
        }

        String username = dto.getAdminUsername().trim();
        User existingByUsername = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)
                .last("LIMIT 1"));
        User adminUser = null;
        if (existingByUsername != null) {
            if (existingByUsername.getUserType() == null || existingByUsername.getUserType() != 1) {
                throw new BusinessException("该用户名已被占用，请更换管理员账号");
            }
            adminUser = existingByUsername;
        } else {
            User existingAdmin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUserType, 1)
                    .eq(User::getDeleted, 0)
                    .orderByAsc(User::getId)
                    .last("LIMIT 1"));
            if (existingAdmin != null) {
                adminUser = existingAdmin;
                adminUser.setUsername(username);
            }
        }

        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUsername(username);
            adminUser.setPassword(BCrypt.hashpw(dto.getAdminPassword()));
            adminUser.setNickname(StringUtils.hasText(dto.getAdminNickname()) ? dto.getAdminNickname().trim() : username);
            adminUser.setEmail(StringUtils.hasText(dto.getAdminEmail()) ? dto.getAdminEmail().trim() : null);
            adminUser.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            adminUser.setUserType(1);
            adminUser.setVerifyStatus(2);
            adminUser.setStatus(0);
            adminUser.setCreditScore(SecurityConstants.MAX_CREDIT_SCORE);
            userMapper.insert(adminUser);
        } else {
            adminUser.setUsername(username);
            adminUser.setPassword(BCrypt.hashpw(dto.getAdminPassword()));
            if (StringUtils.hasText(dto.getAdminNickname())) {
                adminUser.setNickname(dto.getAdminNickname().trim());
            }
            if (StringUtils.hasText(dto.getAdminEmail())) {
                adminUser.setEmail(dto.getAdminEmail().trim());
            }
            if (adminUser.getDeptId() == null) {
                adminUser.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            }
            adminUser.setUserType(1);
            adminUser.setVerifyStatus(2);
            adminUser.setStatus(0);
            userMapper.updateById(adminUser);
        }

        userRoleMapper.deleteByUserId(adminUser.getId());
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(adminUser.getId());
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);

        ensureAdminRoleDepts(adminRole.getId(), systemDept.getId());
        ensureAdminRoleMenus(adminRole.getId());

        SysSiteSetting setting = siteSettingMapper.selectOne(new LambdaQueryWrapper<SysSiteSetting>()
                .orderByDesc(SysSiteSetting::getId)
                .last("LIMIT 1"));
        if (setting == null) {
            setting = new SysSiteSetting();
        }
        setting.setSiteName(dto.getSiteName().trim());
        setting.setLogoUrl(StringUtils.hasText(dto.getLogoUrl()) ? dto.getLogoUrl().trim() : null);
        setting.setFaviconUrl(StringUtils.hasText(dto.getFaviconUrl()) ? dto.getFaviconUrl().trim() : null);
        setting.setTheme(StringUtils.hasText(dto.getTheme()) ? dto.getTheme().trim() : null);
        setting.setStorageProvider(resolveStorageProvider(dto));
        setting.setLocalPath(resolveLocalPath(dto));
        setting.setLocalPublicEnabled(resolveLocalPublicEnabled(dto));
        setting.setAdminUserId(adminUser.getId());
        setting.setSetupCompleted(true);

        if (setting.getId() == null) {
            siteSettingMapper.insert(setting);
        } else {
            siteSettingMapper.updateById(setting);
        }

        lockSetupInitEndpoint();
        applyStorageRuntimeConfig(setting);
    }

    private boolean isSetupCompleted() {
        SysSiteSetting setting = siteSettingMapper.selectOne(new LambdaQueryWrapper<SysSiteSetting>()
                .eq(SysSiteSetting::getSetupCompleted, true)
                .orderByDesc(SysSiteSetting::getId)
                .last("LIMIT 1"));
        return setting != null;
    }

    private String resolveStorageProvider(SetupInitDTO dto) {
        String provider = StringUtils.hasText(dto.getStorageProvider()) ? dto.getStorageProvider().trim() : storageProperties.getPrimaryProvider().getCode();
        StorageProviderType type = StorageProviderType.fromCode(provider);
        if (type != StorageProviderType.LOCAL) {
            throw new BusinessException("当前仅支持本地存储");
        }
        return type.getCode();
    }

    private String resolveLocalPath(SetupInitDTO dto) {
        if (StringUtils.hasText(dto.getLocalPath())) {
            return dto.getLocalPath().trim();
        }
        return storageProperties.getLocalPath();
    }

    private Boolean resolveLocalPublicEnabled(SetupInitDTO dto) {
        if (dto.getLocalPublicEnabled() != null) {
            return dto.getLocalPublicEnabled();
        }
        return storageProperties.isLocalPublicEnabled();
    }

    private void applyStorageRuntimeConfig(SysSiteSetting setting) {
        if (setting == null) {
            return;
        }
        storageProperties.setPrimaryProvider(StorageProviderType.fromCode(setting.getStorageProvider()));
        storageProperties.setFallbackProvider(StorageProviderType.fromCode(setting.getStorageProvider()));
        if (StringUtils.hasText(setting.getLocalPath())) {
            storageProperties.setLocalPath(setting.getLocalPath());
        }
        if (setting.getLocalPublicEnabled() != null) {
            boolean current = storageProperties.isLocalPublicEnabled();
            boolean target = setting.getLocalPublicEnabled();
            if (current != target) {
                log.warn("本地公开访问开关已更新为 {}，需重启应用后生效", target);
            } else {
                storageProperties.setLocalPublicEnabled(target);
            }
        }
    }

    private void acquireSetupLock() {
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + SETUP_LOCK_KEY + ")");
    }

    private void lockSetupInitEndpoint() {
        SysApiPermission permission = apiPermissionMapper.selectOne(new LambdaQueryWrapper<SysApiPermission>()
                .eq(SysApiPermission::getUrl, "/api/v1/setup/init")
                .eq(SysApiPermission::getHttpMethod, "POST")
                .eq(SysApiPermission::getStatus, true)
                .last("LIMIT 1"));
        if (permission == null) {
            return;
        }
        String publicKey = securityProperties.getApiPublicPermissionKey();
        if (publicKey != null && publicKey.equalsIgnoreCase(permission.getPermission())) {
            permission.setPermission("system:setup:init");
            permission.setDescription("setup.init.locked");
            apiPermissionMapper.updateById(permission);
            permissionService.refreshCache();
        }
    }

    private void ensureAdminRoleDepts(Long roleId, Long deptId) {
        if (roleId == null || deptId == null) {
            return;
        }
        List<Long> existing = roleDeptMapper.selectDeptIdsByRoleId(roleId);
        if (existing != null && existing.contains(deptId)) {
            return;
        }
        SysRoleDept roleDept = new SysRoleDept();
        roleDept.setRoleId(roleId);
        roleDept.setDeptId(deptId);
        roleDeptMapper.insert(roleDept);
    }

    private void ensureAdminRoleMenus(Long roleId) {
        if (roleId == null) {
            return;
        }
        List<Long> existingMenuIds = menuMapper.selectMenuIdsByRoleId(roleId);
        Set<Long> existingSet = existingMenuIds == null ? new HashSet<>() : new HashSet<>(existingMenuIds);
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().select(SysMenu::getId)
        );
        for (SysMenu menu : menus) {
            if (menu.getId() == null || existingSet.contains(menu.getId())) {
                continue;
            }
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menu.getId());
            roleMenuMapper.insert(roleMenu);
        }
    }
}
