-- =============================================
-- 校园墙系统 - 初始数据脚本
-- V2: 初始化基础数据
-- =============================================

-- =============================================
-- 1. 初始化部门
-- =============================================
INSERT INTO sys_depts (id, parent_id, dept_name, sort_order, leader, status) VALUES
(1, 0, '校园墙', 1, '管理员', 0),
(2, 1, '技术部', 1, NULL, 0),
(3, 1, '运营部', 2, NULL, 0),
(4, 1, '内容审核部', 3, NULL, 0);

SELECT setval('sys_depts_id_seq', (SELECT MAX(id) FROM sys_depts));

-- =============================================
-- 2. 初始化角色
-- =============================================
INSERT INTO sys_roles (id, role_name, role_key, data_scope, sort_order, remark) VALUES
(1, '超级管理员', 'admin', 1, 1, '拥有所有权限'),
(2, '版主', 'moderator', 3, 2, '内容管理权限'),
(3, '普通用户', 'user', 5, 3, '基础用户权限');

SELECT setval('sys_roles_id_seq', (SELECT MAX(id) FROM sys_roles));

-- =============================================
-- 3. 初始化菜单
-- =============================================

-- 顶级菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(1, 0, '仪表盘', '/console/dashboard', 'views/dashboard/index.vue', 1, 'dashboard', 1),
(2, 0, '系统管理', '/console', 'Layout', 0, 'setting', 2),
(3, 0, '内容管理', '/console', 'Layout', 0, 'document', 3);

-- 系统管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(10, 2, '用户管理', '/console/user', 'views/console/user/index.vue', 1, 'user', 1),
(11, 2, '角色管理', '/console/role', 'views/console/role/index.vue', 1, 'peoples', 2),
(12, 2, '部门管理', '/console/dept', 'views/console/dept/index.vue', 1, 'tree', 3),
(13, 2, '通知公告', '/console/announcement', 'views/console/announcement/index.vue', 1, 'notification', 4),
(14, 2, '敏感词管理', '/console/sensitive-word', 'views/console/sensitive-word/index.vue', 1, 'warning', 5),
(15, 2, '个人中心', '/console/profile', 'views/console/profile/index.vue', 1, 'user', 6);

-- 内容管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(20, 3, '帖子管理', '/console/post', 'views/console/post/index.vue', 1, 'edit', 1),
(21, 3, '评论管理', '/console/comment', 'views/console/comment/index.vue', 1, 'message', 2),
(22, 3, '举报管理', '/console/report', 'views/console/report/index.vue', 1, 'warning', 3),
(23, 3, '身份审核', '/console/verification', 'views/console/verification/index.vue', 1, 'id-card', 4);

-- 用户管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(100, 10, '查询用户', 'system:user:list', 2, 1),
(101, 10, '新增用户', 'system:user:add', 2, 2),
(102, 10, '编辑用户', 'system:user:edit', 2, 3),
(103, 10, '删除用户', 'system:user:delete', 2, 4),
(104, 10, '封禁用户', 'system:user:ban', 2, 5),
(105, 10, '分配角色', 'system:user:role', 2, 6),
(106, 10, '导入用户', 'system:user:import', 2, 7),
(107, 10, '导出用户', 'system:user:export', 2, 8);

-- 角色管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(110, 11, '查询角色', 'system:role:list', 2, 1),
(111, 11, '新增角色', 'system:role:add', 2, 2),
(112, 11, '编辑角色', 'system:role:edit', 2, 3),
(113, 11, '删除角色', 'system:role:delete', 2, 4),
(114, 11, '分配权限', 'system:role:assign', 2, 5),
(115, 11, '导出角色', 'system:role:export', 2, 6);

-- 部门管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(120, 12, '查询部门', 'system:dept:list', 2, 1),
(121, 12, '新增部门', 'system:dept:add', 2, 2),
(122, 12, '修改部门', 'system:dept:edit', 2, 3),
(123, 12, '删除部门', 'system:dept:delete', 2, 4),
(124, 12, '导出部门', 'system:dept:export', 2, 5);

-- 公告管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(130, 13, '查询公告', 'system:announcement:list', 2, 1),
(131, 13, '发布公告', 'system:announcement:add', 2, 2),
(132, 13, '编辑公告', 'system:announcement:edit', 2, 3),
(133, 13, '删除公告', 'system:announcement:delete', 2, 4),
(134, 13, '导出公告', 'system:announcement:export', 2, 5);

-- 敏感词管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(140, 14, '查询敏感词', 'system:sensitive:list', 2, 1),
(141, 14, '新增敏感词', 'system:sensitive:add', 2, 2),
(142, 14, '删除敏感词', 'system:sensitive:delete', 2, 3);

-- 个人中心按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(150, 15, '查看个人信息', 'system:profile:view', 2, 1),
(151, 15, '修改个人信息', 'system:profile:edit', 2, 2),
(152, 15, '修改密码', 'system:profile:password', 2, 3);

-- 帖子管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(200, 20, '查询帖子', 'content:post:list', 2, 1),
(201, 20, '删除帖子', 'content:post:delete', 2, 2),
(202, 20, '审核帖子', 'content:post:audit', 2, 3);

-- 评论管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(210, 21, '查询评论', 'content:comment:list', 2, 1),
(211, 21, '删除评论', 'content:comment:delete', 2, 2);

-- 举报管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(220, 22, '查询举报', 'content:report:list', 2, 1),
(221, 22, '处理举报', 'content:report:handle', 2, 2);

-- 身份审核按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(230, 23, '查询审核', 'content:verification:list', 2, 1),
(231, 23, '处理审核', 'content:verification:handle', 2, 2);

SELECT setval('sys_menus_id_seq', (SELECT MAX(id) FROM sys_menus));

-- =============================================
-- 4. 初始化角色-菜单关联
-- =============================================

-- admin 角色拥有所有菜单权限
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus;

-- moderator 角色拥有内容管理权限
INSERT INTO sys_role_menus (role_id, menu_id) VALUES
-- 仪表盘
(2, 1),
-- 内容管理目录及子菜单
(2, 3),
(2, 20), (2, 21), (2, 22), (2, 23),
-- 内容管理按钮权限
(2, 200), (2, 201), (2, 202),
(2, 210), (2, 211),
(2, 220), (2, 221),
(2, 230), (2, 231),
-- 个人中心
(2, 15), (2, 150), (2, 151), (2, 152);

-- =============================================
-- 5. 初始化管理员账号
-- =============================================

-- ⚠️ 安全警告：此为开发环境默认账号，生产环境部署后请立即修改密码！
-- 默认密码: Admin@123456 (BCrypt 哈希) - 符合复杂度要求但仍需首次登录后修改
INSERT INTO users (id, username, password, nickname, dept_id, user_type, sex, verify_status, status, credit_score) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb386fSvJOaGx7KeIEWr7GqMr2C8.4bMZl8clUvmK', '系统管理员', 1, 1, 0, 2, 0, 100);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 分配 admin 角色
INSERT INTO sys_user_roles (user_id, role_id) VALUES (1, 1);

-- =============================================
-- 6. 初始化API权限配置
-- =============================================

-- 用户管理API
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/console/user/list', 'GET', 'system:user:list', '用户列表'),
('/api/v1/console/user', 'POST', 'system:user:add', '新增用户'),
('/api/v1/console/user/*', 'PUT', 'system:user:edit', '编辑用户'),
('/api/v1/console/user/*', 'DELETE', 'system:user:delete', '删除用户'),
('/api/v1/console/user/*/status', 'PUT', 'system:user:edit', '修改用户状态'),
('/api/v1/console/user/export', 'GET', 'system:user:export', '导出用户'),
('/api/v1/console/user/import', 'POST', 'system:user:import', '导入用户');

-- 角色管理API
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/console/role', 'GET', 'system:role:list', '角色列表'),
('/api/v1/console/role', 'POST', 'system:role:add', '新增角色'),
('/api/v1/console/role/*', 'PUT', 'system:role:edit', '编辑角色'),
('/api/v1/console/role/*', 'DELETE', 'system:role:delete', '删除角色'),
('/api/v1/console/role/*/menus', 'PUT', 'system:role:assign', '分配菜单权限'),
('/api/v1/console/role/*/depts', 'PUT', 'system:role:assign', '分配数据权限');

-- 部门管理API
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/console/dept/tree', 'GET', 'system:dept:list', '部门树'),
('/api/v1/console/dept', 'POST', 'system:dept:add', '新增部门'),
('/api/v1/console/dept/*', 'PUT', 'system:dept:edit', '编辑部门'),
('/api/v1/console/dept/*', 'DELETE', 'system:dept:delete', '删除部门');

-- =============================================
-- 7. 初始化敏感词（示例）
-- =============================================
INSERT INTO sensitive_words (word, level) VALUES
('违禁词示例1', 2),
('违禁词示例2', 2),
('警告词示例1', 1);
