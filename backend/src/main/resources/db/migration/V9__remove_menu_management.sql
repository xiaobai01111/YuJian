-- 删除菜单管理相关的菜单项
DELETE FROM sys_role_menus WHERE menu_id IN (
    SELECT id FROM sys_menus WHERE id = 6 OR parent_id = 6
);

DELETE FROM sys_menus WHERE id = 6 OR parent_id = 6;
