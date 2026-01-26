package com.campus.wall.integration;

import com.campus.wall.entity.system.SysMenu;
import com.campus.wall.entity.system.SysRoleMenu;
import com.campus.wall.service.system.PermissionService;
import com.campus.wall.mapper.system.SysMenuMapper;
import com.campus.wall.mapper.system.SysRoleMenuMapper;
import com.campus.wall.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class PermissionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Test
    void resolvesApiPermissionsByUrl() {
        permissionService.refreshCache();

        assertNotNull(permissionService.getApiPermissionByUrl("/api/v1/auth/login", "POST"));
        assertNotNull(permissionService.getApiPermissionByUrl("/api/v1/notices/public/demo", "GET"));
        assertNull(permissionService.getApiPermissionByUrl("/api/v1/unknown", "GET"));
    }

    @Test
    void permissionChecksWork() {
        permissionService.refreshCache();

        assertTrue(permissionService.hasPermission(1L, "system:user:list"));
        assertFalse(permissionService.hasPermission(1L, "perm:missing"));
    }

    @Test
    void wildcardPermissionShortCircuitsChecks() {
        SysMenu menu = new SysMenu();
        menu.setParentId(0L);
        menu.setName("AllPerms");
        menu.setType(2);
        menu.setPerms("*");
        menu.setVisible(true);
        menu.setStatus(0);
        menu.setSortOrder(999);
        menuMapper.insert(menu);

        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setRoleId(1L);
        roleMenu.setMenuId(menu.getId());
        roleMenuMapper.insert(roleMenu);

        permissionService.refreshCache();
        assertTrue(permissionService.hasPermission(1L, "anything:goes"));
    }
}
