-- =============================================
-- 修复菜单路径：从 /system/* 改为 /console/*
-- =============================================

-- 更新顶级菜单路径
UPDATE sys_menus SET path = '/console/dashboard' WHERE id = 1;
UPDATE sys_menus SET path = '/console' WHERE id = 2;  -- 系统管理目录

-- 更新系统管理子菜单路径
UPDATE sys_menus SET path = '/console/user' WHERE id = 4;
UPDATE sys_menus SET path = '/console/role' WHERE id = 5;
UPDATE sys_menus SET path = '/console/menu' WHERE id = 6;
UPDATE sys_menus SET path = '/console/dict' WHERE id = 50;
UPDATE sys_menus SET path = '/console/dept' WHERE id = 51;
UPDATE sys_menus SET path = '/console/post' WHERE id = 52;
UPDATE sys_menus SET path = '/console/login-log' WHERE id = 53;
UPDATE sys_menus SET path = '/console/oper-log' WHERE id = 54;
UPDATE sys_menus SET path = '/console/profile' WHERE id = 55;

-- 内容管理相关
UPDATE sys_menus SET path = '/console/announcement' WHERE id = 7;
UPDATE sys_menus SET path = '/console/sensitive-word' WHERE id = 8;
