-- =============================================
-- 校园墙系统 - 系统管理菜单扩展
-- V3: 添加完整的系统管理菜单结构
-- =============================================

-- 更新系统管理目录名称
UPDATE sys_menus SET name = '系统管理' WHERE id = 2;
UPDATE sys_menus SET name = '内容管理' WHERE id = 3;
UPDATE sys_menus SET name = '仪表盘' WHERE id = 1;

-- 更新现有子菜单名称
UPDATE sys_menus SET name = '用户管理' WHERE id = 4;
UPDATE sys_menus SET name = '角色管理' WHERE id = 5;
UPDATE sys_menus SET name = '菜单管理' WHERE id = 6;
UPDATE sys_menus SET name = '通知公告' WHERE id = 7;
UPDATE sys_menus SET name = '敏感词管理' WHERE id = 8;

-- =============================================
-- 添加新的系统管理子菜单
-- =============================================

-- 字典管理
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(50, 2, '字典管理', '/system/dict', 'views/system/dict/index.vue', 1, 'dict', 6);

-- 部门管理
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(51, 2, '部门管理', '/system/dept', 'views/system/dept/index.vue', 1, 'tree', 7);

-- 岗位管理
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(52, 2, '岗位管理', '/system/post', 'views/system/post/index.vue', 1, 'post', 8);

-- 登录日志
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(53, 2, '登录日志', '/system/login-log', 'views/system/login-log/index.vue', 1, 'logininfor', 9);

-- 操作日志
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(54, 2, '操作日志', '/system/oper-log', 'views/system/oper-log/index.vue', 1, 'form', 10);

-- 个人中心
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(55, 2, '个人中心', '/system/profile', 'views/system/profile/index.vue', 1, 'user', 11);

-- =============================================
-- 用户管理按钮权限补充
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(100, 4, '导入用户', 'system:user:import', 2, 7),
(101, 4, '导出用户', 'system:user:export', 2, 8);

-- =============================================
-- 角色管理按钮权限补充
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(102, 5, '分配权限', 'system:role:assign', 2, 5),
(103, 5, '导出角色', 'system:role:export', 2, 6);

-- =============================================
-- 菜单管理按钮权限补充
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(104, 6, '导出菜单', 'system:menu:export', 2, 5);

-- =============================================
-- 字典管理按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(110, 50, '查询字典', 'system:dict:list', 2, 1),
(111, 50, '新增字典', 'system:dict:add', 2, 2),
(112, 50, '修改字典', 'system:dict:edit', 2, 3),
(113, 50, '删除字典', 'system:dict:delete', 2, 4),
(114, 50, '导出字典', 'system:dict:export', 2, 5);

-- =============================================
-- 部门管理按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(120, 51, '查询部门', 'system:dept:list', 2, 1),
(121, 51, '新增部门', 'system:dept:add', 2, 2),
(122, 51, '修改部门', 'system:dept:edit', 2, 3),
(123, 51, '删除部门', 'system:dept:delete', 2, 4),
(124, 51, '导出部门', 'system:dept:export', 2, 5);

-- =============================================
-- 岗位管理按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(130, 52, '查询岗位', 'system:post:list', 2, 1),
(131, 52, '新增岗位', 'system:post:add', 2, 2),
(132, 52, '修改岗位', 'system:post:edit', 2, 3),
(133, 52, '删除岗位', 'system:post:delete', 2, 4),
(134, 52, '导出岗位', 'system:post:export', 2, 5);

-- =============================================
-- 登录日志按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(140, 53, '查询登录日志', 'system:loginlog:list', 2, 1),
(141, 53, '删除登录日志', 'system:loginlog:delete', 2, 2),
(142, 53, '清空登录日志', 'system:loginlog:clear', 2, 3),
(143, 53, '导出登录日志', 'system:loginlog:export', 2, 4);

-- =============================================
-- 操作日志按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(150, 54, '查询操作日志', 'system:operlog:list', 2, 1),
(151, 54, '删除操作日志', 'system:operlog:delete', 2, 2),
(152, 54, '清空操作日志', 'system:operlog:clear', 2, 3),
(153, 54, '导出操作日志', 'system:operlog:export', 2, 4);

-- =============================================
-- 通知公告按钮权限补充
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(160, 7, '导出公告', 'system:announcement:export', 2, 5);

-- =============================================
-- 个人中心按钮权限
-- =============================================
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(170, 55, '查看个人信息', 'system:profile:view', 2, 1),
(171, 55, '修改个人信息', 'system:profile:edit', 2, 2),
(172, 55, '修改密码', 'system:profile:password', 2, 3);

-- =============================================
-- 为管理员角色分配新菜单权限
-- =============================================
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus WHERE id >= 50 AND id NOT IN (SELECT menu_id FROM sys_role_menus WHERE role_id = 1);

-- 重置序列
SELECT setval('sys_menus_id_seq', (SELECT MAX(id) FROM sys_menus));
