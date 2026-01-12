-- =============================================
-- 校园墙系统 - 初始数据脚本
-- V2: 初始化基础数据
-- =============================================

-- =============================================
-- 1. 初始化角色
-- =============================================
INSERT INTO sys_roles (role_name, role_key, sort_order) VALUES
('超级管理员', 'admin', 1),
('版主', 'moderator', 2),
('普通用户', 'user', 3);

-- =============================================
-- 2. 初始化菜单
-- =============================================

-- 目录级菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(1, 0, 'Dashboard', '/dashboard', 'views/dashboard/index.vue', 1, 'dashboard', 0),
(2, 0, 'System', '/system', 'Layout', 0, 'setting', 1),
(3, 0, 'Content', '/content', 'Layout', 0, 'document', 2);

-- 系统管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(4, 2, 'UserManage', '/system/user', 'views/system/user/index.vue', 1, 'user', 1),
(5, 2, 'RoleManage', '/system/role', 'views/system/role/index.vue', 1, 'peoples', 2),
(6, 2, 'MenuManage', '/system/menu', 'views/system/menu/index.vue', 1, 'tree-table', 3),
(7, 2, 'AnnouncementManage', '/system/announcement', 'views/system/announcement/index.vue', 1, 'notification', 4),
(8, 2, 'SensitiveWordManage', '/system/sensitive-word', 'views/system/sensitive-word/index.vue', 1, 'warning', 5);

-- 内容管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order) VALUES
(9, 3, 'PostManage', '/content/post', 'views/content/post/index.vue', 1, 'edit', 1),
(10, 3, 'CommentManage', '/content/comment', 'views/content/comment/index.vue', 1, 'message', 2),
(11, 3, 'ReportManage', '/content/report', 'views/content/report/index.vue', 1, 'warning', 3),
(12, 3, 'VerificationManage', '/content/verification', 'views/content/verification/index.vue', 1, 'id-card', 4);

-- 用户管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(13, 4, '查询用户', 'system:user:list', 2, 1),
(14, 4, '新增用户', 'system:user:add', 2, 2),
(15, 4, '编辑用户', 'system:user:edit', 2, 3),
(16, 4, '删除用户', 'system:user:delete', 2, 4),
(17, 4, '封禁用户', 'system:user:ban', 2, 5),
(18, 4, '分配角色', 'system:user:role', 2, 6);

-- 角色管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(19, 5, '查询角色', 'system:role:list', 2, 1),
(20, 5, '新增角色', 'system:role:add', 2, 2),
(21, 5, '编辑角色', 'system:role:edit', 2, 3),
(22, 5, '删除角色', 'system:role:delete', 2, 4);

-- 菜单管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(23, 6, '查询菜单', 'system:menu:list', 2, 1),
(24, 6, '新增菜单', 'system:menu:add', 2, 2),
(25, 6, '编辑菜单', 'system:menu:edit', 2, 3),
(26, 6, '删除菜单', 'system:menu:delete', 2, 4);

-- 公告管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(27, 7, '查询公告', 'system:announcement:list', 2, 1),
(28, 7, '发布公告', 'system:announcement:add', 2, 2),
(29, 7, '编辑公告', 'system:announcement:edit', 2, 3),
(30, 7, '删除公告', 'system:announcement:delete', 2, 4);

-- 敏感词管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(31, 8, '查询敏感词', 'system:sensitive:list', 2, 1),
(32, 8, '新增敏感词', 'system:sensitive:add', 2, 2),
(33, 8, '删除敏感词', 'system:sensitive:delete', 2, 3);

-- 帖子管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(34, 9, '查询帖子', 'content:post:list', 2, 1),
(35, 9, '删除帖子', 'content:post:delete', 2, 2),
(36, 9, '审核帖子', 'content:post:audit', 2, 3);

-- 评论管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(37, 10, '查询评论', 'content:comment:list', 2, 1),
(38, 10, '删除评论', 'content:comment:delete', 2, 2);

-- 举报管理按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(39, 11, '查询举报', 'content:report:list', 2, 1),
(40, 11, '处理举报', 'content:report:handle', 2, 2);

-- 身份审核按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order) VALUES
(41, 12, '查询审核', 'content:verification:list', 2, 1),
(42, 12, '处理审核', 'content:verification:handle', 2, 2);

-- 重置序列
SELECT setval('sys_menus_id_seq', 42);

-- =============================================
-- 3. 初始化角色-菜单关联
-- =============================================

-- admin 角色拥有所有菜单权限
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus;

-- moderator 角色拥有内容管理权限
INSERT INTO sys_role_menus (role_id, menu_id) VALUES
(2, 1),   -- Dashboard
(2, 3),   -- Content 目录
(2, 9),   -- 帖子管理
(2, 10),  -- 评论管理
(2, 11),  -- 举报管理
(2, 12),  -- 身份审核
(2, 34),  -- 查询帖子
(2, 35),  -- 删除帖子
(2, 36),  -- 审核帖子
(2, 37),  -- 查询评论
(2, 38),  -- 删除评论
(2, 39),  -- 查询举报
(2, 40),  -- 处理举报
(2, 41),  -- 查询审核
(2, 42);  -- 处理审核

-- user 角色无后台管理权限

-- =============================================
-- 4. 初始化管理员账号
-- =============================================

-- 密码: 123456 (BCrypt 哈希)
INSERT INTO users (id, username, password, nickname, verify_status, status, credit_score) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt2.S8K', '系统管理员', 2, 0, 100);

-- 分配 admin 角色
INSERT INTO sys_user_roles (user_id, role_id) VALUES (1, 1);

-- 重置序列
SELECT setval('users_id_seq', 1);

-- =============================================
-- 5. 初始化敏感词（示例）
-- =============================================
INSERT INTO sensitive_words (word, level) VALUES
('违禁词1', 2),
('违禁词2', 2),
('警告词1', 1);
