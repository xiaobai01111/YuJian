package com.campus.wall.integration;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import com.campus.wall.common.BusinessException;
import com.campus.wall.constant.SecurityConstants;
import com.campus.wall.entity.system.SysDept;
import com.campus.wall.service.system.DeptService;
import com.campus.wall.support.IntegrationTestBase;
import com.campus.wall.support.SaTokenTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class DeptServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DeptService deptService;
    private SaTokenContext previousContext;

    @BeforeEach
    void setupContext() {
        previousContext = SaManager.getSaTokenContext();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenTestContext.bind();
    }

    @AfterEach
    void teardownContext() {
        StpUtil.logout(1L);
        SaTokenTestContext.clear();
        SaManager.setSaTokenContext(previousContext);
    }

    @Test
    void createRejectsBlankName() {
        SysDept dept = new SysDept();
        dept.setDeptName("  ");

        assertThrows(BusinessException.class, () -> deptService.create(dept));
    }

    @Test
    void createRejectsDuplicateName() {
        SysDept dept = new SysDept();
        dept.setDeptName("dup_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        Long id = deptService.create(dept);

        SysDept dup = new SysDept();
        dup.setDeptName(deptService.getById(id).getDeptName());
        dup.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dup.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);

        assertThrows(BusinessException.class, () -> deptService.create(dup));
    }

    @Test
    void createRejectsInvalidDataScope() {
        SysDept dept = new SysDept();
        dept.setDeptName("bad_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDataScope(99);

        assertThrows(BusinessException.class, () -> deptService.create(dept));
    }

    @Test
    void createDefaultsParentToSystemDept() {
        SysDept dept = new SysDept();
        dept.setDeptName("root_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        Long id = deptService.create(dept);

        SysDept saved = deptService.getById(id);
        assertEquals(SecurityConstants.SYSTEM_DEPT_ID, saved.getParentId());
    }

    @Test
    void updateSystemDeptParentDenied() {
        StpUtil.login(1L);
        SysDept update = new SysDept();
        update.setParentId(2L);

        assertThrows(BusinessException.class, () -> deptService.update(SecurityConstants.SYSTEM_DEPT_ID, update));
    }

    @Test
    void updateSystemDeptDataScopeDenied() {
        StpUtil.login(1L);
        SysDept update = new SysDept();
        update.setDataScope(SecurityConstants.DATA_SCOPE_SELF);

        assertThrows(BusinessException.class, () -> deptService.update(SecurityConstants.SYSTEM_DEPT_ID, update));
    }

    @Test
    void updateParentZeroMapsToSystemDept() {
        StpUtil.login(1L);
        SysDept dept = new SysDept();
        dept.setDeptName("move_" + UUID.randomUUID().toString().substring(0, 6));
        dept.setParentId(SecurityConstants.SYSTEM_DEPT_ID);
        dept.setDataScope(SecurityConstants.DATA_SCOPE_DEPT);
        Long id = deptService.create(dept);

        SysDept update = new SysDept();
        update.setParentId(0L);
        deptService.update(id, update);

        SysDept updated = deptService.getById(id);
        assertEquals(SecurityConstants.SYSTEM_DEPT_ID, updated.getParentId());
    }
}
