-- 校园墙初始化数据（重置版）

-- 1) 系统部门（根）
INSERT INTO sys_depts (id, parent_id, dept_name, sort_order, leader, status, data_scope)
VALUES (1, 0, '系统部门', 0, '系统管理员', 0, 1);

SELECT setval('sys_depts_id_seq', (SELECT MAX(id) FROM sys_depts));

-- 2) 角色
INSERT INTO sys_roles (id, role_name, role_key, status, sort_order, remark)
VALUES
    (1, '系统管理员', 'admin', 0, 1, '系统角色'),
    (2, '版主', 'moderator', 0, 2, '内容管理权限'),
    (3, '普通用户', 'user', 0, 3, '基础用户权限');

SELECT setval('sys_roles_id_seq', (SELECT MAX(id) FROM sys_roles));

-- 3) 管理员账号（默认密码：Admin@123）
INSERT INTO users (id, username, password, nickname, dept_id, user_type, verify_status, status, credit_score)
VALUES (1, 'admin', '$2y$10$tyTtSrcuzDqXLgTlfM0GTuh0TR7/.cFUceo6y720YixN4Anr0HaWy', '系统管理员', 1, 1, 2, 0, 100);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

INSERT INTO sys_user_roles (user_id, role_id) VALUES (1, 1);

-- 4) 菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (1, 0, '仪表盘', '/console/dashboard', 'views/console/dashboard/index.vue', 1, 'dashboard', 1, TRUE, 0),
    (2, 0, '系统管理', '/console', 'Layout', 0, 'setting', 2, TRUE, 0),
    (3, 0, '内容管理', '/console', 'Layout', 0, 'document', 3, TRUE, 0),
    (4, 0, '系统监控', '/console', 'Layout', 0, 'monitor', 4, TRUE, 0);

-- 系统管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (10, 2, '用户管理', '/console/user', 'views/console/user/index.vue', 1, 'user', 1, TRUE, 0),
    (11, 2, '角色管理', '/console/role', 'views/console/role/index.vue', 1, 'role', 2, TRUE, 0),
    (12, 2, '部门管理', '/console/dept', 'views/console/dept/index.vue', 1, 'tree', 3, TRUE, 0),
    (13, 2, '认证规则', '/console/auth-rule', 'views/console/auth-rule/index.vue', 1, 'id-card', 4, TRUE, 0),
    (14, 2, '敏感词管理', '/console/sensitive-word', 'views/console/sensitive-word/index.vue', 1, 'warning', 5, TRUE, 0),
    (15, 2, '公告管理', '/console/notice', 'views/console/notice/index.vue', 1, 'bell', 6, TRUE, 0),
    (16, 2, '登录日志', '/console/login-log', 'views/console/login-log/index.vue', 1, 'logininfor', 7, TRUE, 0),
    (17, 2, '操作日志', '/console/oper-log', 'views/console/oper-log/index.vue', 1, 'form', 8, TRUE, 0),
    (18, 2, '个人中心', '/console/profile', 'views/console/profile/index.vue', 1, 'user', 9, TRUE, 0);

-- 内容管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (20, 3, '帖子管理', '/console/post', 'views/console/post/index.vue', 1, 'post', 1, TRUE, 0),
    (21, 3, '评论管理', '/console/comment', 'views/console/comment/index.vue', 1, 'message', 2, TRUE, 0),
    (22, 3, '举报管理', '/console/report', 'views/console/report/index.vue', 1, 'warning', 3, TRUE, 0),
    (23, 3, '身份审核', '/console/verification', 'views/console/verification/index.vue', 1, 'id-card', 4, TRUE, 0);

-- 系统监控子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (30, 4, '定时任务', '/console/monitor/job', 'views/console/monitor/job/index.vue', 1, 'timer', 1, TRUE, 0),
    (31, 4, '在线用户', '/console/monitor/online', 'views/console/monitor/online/index.vue', 1, 'users', 2, TRUE, 0),
    (32, 4, '服务监控', '/console/monitor/server', 'views/console/monitor/server/index.vue', 1, 'server', 3, TRUE, 0),
    (33, 4, 'Redis监控', '/console/monitor/redis', 'views/console/monitor/redis/index.vue', 1, 'redis', 4, TRUE, 0),
    (34, 4, '数据缓存', '/console/monitor/cache', 'views/console/monitor/cache/index.vue', 1, 'cache', 5, TRUE, 0),
    (35, 4, '阻止名单', '/console/monitor/blocklist', 'views/console/monitor/blocklist/index.vue', 1, 'block', 6, TRUE, 0);

-- 按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order, visible, status) VALUES
    (100, 10, '查询用户', 'system:user:list', 2, 1, TRUE, 0),
    (101, 10, '新增用户', 'system:user:add', 2, 2, TRUE, 0),
    (102, 10, '编辑用户', 'system:user:edit', 2, 3, TRUE, 0),
    (103, 10, '删除用户', 'system:user:delete', 2, 4, TRUE, 0),
    (104, 10, '封禁用户', 'system:user:ban', 2, 5, TRUE, 0),
    (105, 10, '分配角色', 'system:user:role', 2, 6, TRUE, 0),
    (106, 10, '导入用户', 'system:user:import', 2, 7, TRUE, 0),
    (107, 10, '导出用户', 'system:user:export', 2, 8, TRUE, 0),

    (110, 11, '查询角色', 'system:role:list', 2, 1, TRUE, 0),
    (111, 11, '新增角色', 'system:role:add', 2, 2, TRUE, 0),
    (112, 11, '编辑角色', 'system:role:edit', 2, 3, TRUE, 0),
    (113, 11, '删除角色', 'system:role:delete', 2, 4, TRUE, 0),
    (114, 11, '分配权限', 'system:role:assign', 2, 5, TRUE, 0),

    (120, 12, '查询部门', 'system:dept:list', 2, 1, TRUE, 0),
    (121, 12, '新增部门', 'system:dept:add', 2, 2, TRUE, 0),
    (122, 12, '编辑部门', 'system:dept:edit', 2, 3, TRUE, 0),
    (123, 12, '删除部门', 'system:dept:delete', 2, 4, TRUE, 0),

    (130, 13, '查询认证规则', 'system:auth-rule:list', 2, 1, TRUE, 0),
    (131, 13, '新增认证规则', 'system:auth-rule:add', 2, 2, TRUE, 0),
    (132, 13, '编辑认证规则', 'system:auth-rule:edit', 2, 3, TRUE, 0),
    (133, 13, '删除认证规则', 'system:auth-rule:delete', 2, 4, TRUE, 0),

    (140, 14, '查询敏感词', 'system:sensitive-word:list', 2, 1, TRUE, 0),
    (141, 14, '新增敏感词', 'system:sensitive-word:add', 2, 2, TRUE, 0),
    (142, 14, '删除敏感词', 'system:sensitive-word:delete', 2, 3, TRUE, 0),
    (143, 14, '修改级别', 'system:sensitive-word:edit', 2, 4, TRUE, 0),

    (150, 15, '查询公告', 'system:notice:list', 2, 1, TRUE, 0),
    (151, 15, '新增公告', 'system:notice:add', 2, 2, TRUE, 0),
    (152, 15, '编辑公告', 'system:notice:edit', 2, 3, TRUE, 0),
    (153, 15, '发布公告', 'system:notice:publish', 2, 4, TRUE, 0),
    (154, 15, '下线公告', 'system:notice:offline', 2, 5, TRUE, 0),
    (155, 15, '删除公告', 'system:notice:delete', 2, 6, TRUE, 0),

    (160, 17, '查询操作日志', 'system:operlog:list', 2, 1, TRUE, 0),
    (161, 17, '删除操作日志', 'system:operlog:delete', 2, 2, TRUE, 0),
    (162, 17, '清空操作日志', 'system:operlog:clear', 2, 3, TRUE, 0),
    (163, 17, '导出操作日志', 'system:operlog:export', 2, 4, TRUE, 0),

    (170, 16, '查询登录日志', 'system:loginlog:list', 2, 1, TRUE, 0),
    (171, 16, '删除登录日志', 'system:loginlog:delete', 2, 2, TRUE, 0),
    (172, 16, '清空登录日志', 'system:loginlog:clear', 2, 3, TRUE, 0),
    (173, 16, '导出登录日志', 'system:loginlog:export', 2, 4, TRUE, 0),

    (240, 31, '查询在线用户', 'system:online:list', 2, 1, TRUE, 0),
    (241, 31, '强制下线', 'system:online:kickout', 2, 2, TRUE, 0),
    (250, 32, '查看服务监控', 'system:monitor:server', 2, 1, TRUE, 0),
    (260, 33, '查看Redis监控', 'system:monitor:redis', 2, 1, TRUE, 0),

    (180, 18, '编辑资料', 'system:profile:edit', 2, 1, TRUE, 0),
    (181, 18, '修改密码', 'system:profile:password', 2, 2, TRUE, 0),

    (190, 1, '仪表盘-用户', 'system:dashboard:user', 2, 1, TRUE, 0),
    (191, 1, '仪表盘-帖子', 'system:dashboard:post', 2, 2, TRUE, 0),
    (192, 1, '仪表盘-公告', 'system:dashboard:notice', 2, 3, TRUE, 0),
    (193, 1, '仪表盘-运维', 'system:dashboard:ops', 2, 4, TRUE, 0),
    (194, 1, '仪表盘-登录', 'system:dashboard:login', 2, 5, TRUE, 0),
    (195, 1, '仪表盘-操作日志', 'system:dashboard:operlog', 2, 6, TRUE, 0),
    (196, 1, '仪表盘-举报', 'system:dashboard:report', 2, 7, TRUE, 0),
    (197, 1, '仪表盘-审核', 'system:dashboard:verify', 2, 8, TRUE, 0),

    (200, 20, '查询帖子', 'content:post:list', 2, 1, TRUE, 0),
    (201, 20, '新增帖子', 'content:post:add', 2, 2, TRUE, 0),
    (202, 20, '编辑帖子', 'content:post:edit', 2, 3, TRUE, 0),
    (203, 20, '删除帖子', 'content:post:delete', 2, 4, TRUE, 0),
    (204, 20, '标记已解决', 'content:post:resolve', 2, 5, TRUE, 0),

    (210, 21, '查询评论', 'content:comment:list', 2, 1, TRUE, 0),
    (211, 21, '删除评论', 'content:comment:delete', 2, 2, TRUE, 0),

    (220, 22, '查询举报', 'content:report:list', 2, 1, TRUE, 0),
    (221, 22, '处理举报', 'content:report:handle', 2, 2, TRUE, 0),

    (230, 23, '查询审核', 'content:verification:list', 2, 1, TRUE, 0),
    (231, 23, '处理审核', 'content:verification:handle', 2, 2, TRUE, 0);

SELECT setval('sys_menus_id_seq', (SELECT MAX(id) FROM sys_menus));

-- 5) 角色-菜单关系
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus;

-- 版主角色：内容管理 + 个人中心
INSERT INTO sys_role_menus (role_id, menu_id) VALUES
    (2, 1),
    (2, 3), (2, 20), (2, 21), (2, 22), (2, 23),
    (2, 200), (2, 210), (2, 211), (2, 220), (2, 221), (2, 230), (2, 231),
    (2, 18);
