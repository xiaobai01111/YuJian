package com.campus.wall.support;

import cn.hutool.crypto.digest.BCrypt;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.entity.system.SysRole;
import com.campus.wall.entity.system.SysUserRole;
import com.campus.wall.entity.user.User;
import com.campus.wall.mapper.system.SysRoleMapper;
import com.campus.wall.mapper.system.SysUserRoleMapper;
import com.campus.wall.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 测试环境初始化管理员账号，避免集成测试依赖缺失。
 */
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestAdminInitializer implements ApplicationRunner {

    private static final Long ADMIN_USER_ID = 1L;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private final UserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureAdminUser();
        ensureAdminRoleBinding();
    }

    private void ensureAdminUser() {
        User user = userMapper.selectById(ADMIN_USER_ID);
        if (user == null) {
            user = new User();
            user.setId(ADMIN_USER_ID);
            user.setUsername(ADMIN_USERNAME);
            user.setPassword(BCrypt.hashpw(ADMIN_PASSWORD));
            user.setNickname(ADMIN_USERNAME);
            user.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            user.setUserType(1);
            user.setVerifyStatus(2);
            user.setStatus(0);
            user.setCreditScore(SecurityConstants.MAX_CREDIT_SCORE);
            userMapper.insert(user);
            jdbcTemplate.execute("SELECT setval('users_id_seq', (SELECT MAX(id) FROM users))");
            return;
        }
        if (!ADMIN_USERNAME.equals(user.getUsername())) {
            user.setUsername(ADMIN_USERNAME);
            user.setNickname(ADMIN_USERNAME);
            user.setPassword(BCrypt.hashpw(ADMIN_PASSWORD));
            user.setUserType(1);
            if (user.getDeptId() == null) {
                user.setDeptId(SecurityConstants.SYSTEM_DEPT_ID);
            }
            userMapper.updateById(user);
        }
    }

    private void ensureAdminRoleBinding() {
        SysRole adminRole = roleMapper.selectByRoleKey(com.campus.wall.util.SecurityUtil.getSuperAdminRoleKey());
        if (adminRole == null) {
            return;
        }
        Long count = userRoleMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, ADMIN_USER_ID)
                .eq(SysUserRole::getRoleId, adminRole.getId())
        );
        if (count == null || count == 0) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(ADMIN_USER_ID);
            userRole.setRoleId(adminRole.getId());
            userRoleMapper.insert(userRole);
        }
    }
}
