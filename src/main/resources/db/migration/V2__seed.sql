-- noinspection SpellCheckingInspection
-- 校园墙初始化数据（重置版）

-- 审计日志增强字段（兼容旧结构）
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS request_body_digest VARCHAR(64);
COMMENT ON COLUMN sys_oper_log.user_agent IS '请求UA';
COMMENT ON COLUMN sys_oper_log.request_body_digest IS '请求体SHA-256摘要';

-- 1) 系统部门（根）
INSERT INTO sys_depts (id, parent_id, dept_name, sort_order, leader, status, data_scope)
VALUES (1, 0, '系统部门', 0, '系统管理员', 0, 1);

SELECT setval('sys_depts_id_seq', (SELECT MAX(id) FROM sys_depts));

-- 2) 角色
INSERT INTO sys_roles (id, role_name, role_key, status, sort_order, remark)
VALUES
    (1, '系统管理员', 'console', 0, 1, '系统角色'),
    (2, '版主', 'moderator', 0, 2, '内容管理权限'),
    (3, '普通用户', 'user', 0, 3, '基础用户权限');

SELECT setval('sys_roles_id_seq', (SELECT MAX(id) FROM sys_roles));

-- 3) 管理员账号由初始化向导创建

-- 4) 菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (1, 0, '仪表盘', '/console/dashboard', 'views/console/dashboard/index.vue', 1, 'dashboard', 1, TRUE, 0),
    (2, 0, '系统管理', '/console', 'Layout', 0, 'setting', 2, TRUE, 0),
    (3, 0, '内容管理', '/console', 'Layout', 0, 'document', 3, TRUE, 0),
    (5, 0, '资产管理', '/console', 'Layout', 0, 'image', 4, TRUE, 0),
    (4, 0, '系统监控', '/console', 'Layout', 0, 'monitor', 5, TRUE, 0),
    (6, 0, '校园管理', '/console', 'Layout', 0, 'peoples', 6, TRUE, 0),
    (8, 0, '审计日志', '/console', 'Layout', 0, 'form', 7, TRUE, 0);

-- 系统管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (10, 2, '用户管理', '/console/system/user', 'views/console/user/index.vue', 1, 'user', 1, TRUE, 0),
    (11, 2, '角色管理', '/console/system/role', 'views/console/role/index.vue', 1, 'role', 2, TRUE, 0),
    (12, 2, '部门管理', '/console/system/dept', 'views/console/dept/index.vue', 1, 'tree', 3, TRUE, 0),
    (23, 2, '身份审核', '/console/system/verification', 'views/console/verification/index.vue', 1, 'id-card', 4, TRUE, 0),
    (13, 2, '认证规则', '/console/system/auth-rule', 'views/console/auth-rule/index.vue', 1, 'id-card', 5, TRUE, 0),
    (14, 2, '敏感词管理', '/console/system/sensitive-word', 'views/console/sensitive-word/index.vue', 1, 'warning', 6, TRUE, 0),
    (19, 2, '邮件服务配置', '/console/system/email-config', 'views/console/email-config/index.vue', 1, 'email', 7, TRUE, 0),
    (18, 2, '个人中心', '/console/profile', 'views/console/profile/index.vue', 1, 'user', 99, TRUE, 0);

-- 内容管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (20, 3, '帖子管理', '/console/content/post', 'views/console/post/index.vue', 1, 'post', 1, TRUE, 0),
    (21, 3, '评论管理', '/console/content/comment', 'views/console/comment/index.vue', 1, 'message', 2, TRUE, 0),
    (15, 3, '公告管理', '/console/content/notice', 'views/console/notice/index.vue', 1, 'bell', 3, TRUE, 0),
    (22, 3, '举报管理', '/console/content/report', 'views/console/report/index.vue', 1, 'warning', 4, TRUE, 0),
    (28, 3, '审核中心', '/console/content/verify', 'views/console/content/verify/index.vue', 1, 'id-card', 5, TRUE, 0),
    (24, 3, '回收站', '/console/content/recycle', 'views/console/recycle/index.vue', 1, 'recycle', 6, TRUE, 0);

-- 系统监控子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (31, 4, '在线用户', '/console/monitor/online', 'views/console/monitor/online/index.vue', 1, 'users', 1, TRUE, 0),
    (32, 4, '服务监控', '/console/monitor/server', 'views/console/monitor/server/index.vue', 1, 'server', 2, TRUE, 0),
    (33, 4, 'Redis监控', '/console/monitor/redis', 'views/console/monitor/redis/index.vue', 1, 'redis', 3, TRUE, 0),
    (35, 4, '阻止名单', '/console/monitor/blocklist', 'views/console/monitor/blocklist/index.vue', 1, 'block', 4, TRUE, 0);

-- 资产管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (40, 5, '公共媒体库', '/console/asset/media', 'views/console/tool/gallery/index.vue', 1, 'image', 1, TRUE, 0),
    (41, 5, '业务附件库', '/console/asset/attachment', 'views/console/tool/file/index.vue', 1, 'file', 2, TRUE, 0),
    (42, 5, '系统资源库', '/console/asset/resource', 'views/console/asset/resource/index.vue', 1, 'image', 3, TRUE, 0),
    (900, 5, '上传策略', '/console/asset/upload-policy', 'views/console/asset/upload-policy/index.vue', 1, 'setting', 4, TRUE, 0);

-- 校园管理子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (60, 6, '校园展示位', '/console/campus/showcase', 'views/console/campus/hero/index.vue', 1, 'image', 1, TRUE, 0),
    (61, 6, '校园信息', '/console/campus/school', 'views/console/campus/school/index.vue', 1, 'tree', 2, TRUE, 0);

-- 审计日志子菜单
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (16, 8, '登录日志', '/console/audit/login-log', 'views/console/login-log/index.vue', 1, 'logininfor', 1, TRUE, 0),
    (17, 8, '操作日志', '/console/audit/oper-log', 'views/console/oper-log/index.vue', 1, 'form', 2, TRUE, 0);

-- 按钮权限
INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order, visible, status) VALUES
    (100, 10, '查询用户', 'system:user:list', 2, 1, TRUE, 0),
    (101, 10, '新增用户', 'system:user:add', 2, 2, TRUE, 0),
    (102, 10, '编辑用户', 'system:user:edit', 2, 3, TRUE, 0),
    (103, 10, '删除用户', 'system:user:delete', 2, 4, TRUE, 0),
    (104, 10, '封禁用户', 'system:user:ban', 2, 5, TRUE, 0),
    (108, 10, '解封用户', 'system:user:unban', 2, 6, TRUE, 0),
    (109, 10, '恢复用户', 'system:user:restore', 2, 7, TRUE, 0),
    (116, 10, '彻底删除用户', 'system:user:purge', 2, 8, TRUE, 0),
    (105, 10, '分配角色', 'system:user:role', 2, 9, TRUE, 0),
    (106, 10, '导入用户', 'system:user:import', 2, 10, TRUE, 0),
    (107, 10, '导出用户', 'system:user:export', 2, 11, TRUE, 0),

    (110, 11, '查询角色', 'system:role:list', 2, 1, TRUE, 0),
    (115, 11, '查看角色', 'system:role:view', 2, 2, TRUE, 0),
    (111, 11, '新增角色', 'system:role:add', 2, 3, TRUE, 0),
    (112, 11, '编辑角色', 'system:role:edit', 2, 4, TRUE, 0),
    (117, 11, '启用角色', 'system:role:enable', 2, 5, TRUE, 0),
    (118, 11, '停用角色', 'system:role:disable', 2, 6, TRUE, 0),
    (113, 11, '删除角色', 'system:role:delete', 2, 7, TRUE, 0),
    (114, 11, '分配权限', 'system:role:assign', 2, 8, TRUE, 0),

    (120, 12, '查询部门', 'system:dept:list', 2, 1, TRUE, 0),
    (121, 12, '新增部门', 'system:dept:add', 2, 2, TRUE, 0),
    (122, 12, '编辑部门', 'system:dept:edit', 2, 3, TRUE, 0),
    (123, 12, '删除部门', 'system:dept:delete', 2, 4, TRUE, 0),
    (124, 12, '查看部门', 'system:dept:view', 2, 5, TRUE, 0),
    (125, 12, '调整层级', 'system:dept:move', 2, 6, TRUE, 0),
    (126, 12, '调整排序', 'system:dept:sort', 2, 7, TRUE, 0),
    (127, 12, '导入部门', 'system:dept:import', 2, 8, TRUE, 0),
    (128, 12, '导出部门', 'system:dept:export', 2, 9, TRUE, 0),
    (129, 12, '同步部门', 'system:dept:sync', 2, 10, TRUE, 0),

    (130, 13, '查询认证规则', 'system:auth-rule:list', 2, 1, TRUE, 0),
    (131, 13, '新增认证规则', 'system:auth-rule:add', 2, 2, TRUE, 0),
    (132, 13, '编辑认证规则', 'system:auth-rule:edit', 2, 3, TRUE, 0),
    (133, 13, '删除认证规则', 'system:auth-rule:delete', 2, 4, TRUE, 0),
    (134, 13, '查看认证规则', 'system:auth-rule:view', 2, 5, TRUE, 0),
    (135, 13, '启用/停用认证规则', 'system:auth-rule:status', 2, 6, TRUE, 0),
    (136, 13, '调整认证规则优先级', 'system:auth-rule:sort', 2, 7, TRUE, 0),
    (137, 13, '复制认证规则', 'system:auth-rule:clone', 2, 8, TRUE, 0),

    (140, 14, '查询敏感词', 'system:sensitive-word:list', 2, 1, TRUE, 0),
    (141, 14, '新增敏感词', 'system:sensitive-word:add', 2, 2, TRUE, 0),
    (142, 14, '删除敏感词', 'system:sensitive-word:delete', 2, 3, TRUE, 0),
    (143, 14, '修改级别', 'system:sensitive-word:edit', 2, 4, TRUE, 0),
    (144, 14, '批量导入敏感词', 'system:sensitive-word:batch:add', 2, 5, TRUE, 0),
    (145, 14, '批量删除敏感词', 'system:sensitive-word:batch:delete', 2, 6, TRUE, 0),

    (146, 19, '查看邮箱域名', 'system:config:email-domains:view', 2, 1, TRUE, 0),
    (147, 19, '编辑邮箱域名', 'system:config:email-domains:edit', 2, 2, TRUE, 0),
    (148, 19, '查看SMTP配置', 'system:config:smtp:view', 2, 3, TRUE, 0),
    (149, 19, '编辑SMTP配置', 'system:config:smtp:edit', 2, 4, TRUE, 0),
    (156, 19, '测试SMTP配置', 'system:config:smtp:test', 2, 5, TRUE, 0),
    (157, 19, '查看邮件模板', 'system:config:email-templates:view', 2, 6, TRUE, 0),
    (158, 19, '编辑邮件模板', 'system:config:email-templates:edit', 2, 7, TRUE, 0),

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

    (600, 60, '查询Hero', 'campus:hero:list', 2, 1, TRUE, 0),
    (601, 60, '新增Hero', 'campus:hero:add', 2, 2, TRUE, 0),
    (602, 60, '编辑Hero', 'campus:hero:edit', 2, 3, TRUE, 0),
    (603, 60, '删除Hero', 'campus:hero:delete', 2, 4, TRUE, 0),

    (240, 31, '查询在线用户', 'system:online:list', 2, 1, TRUE, 0),
    (241, 31, '强制下线', 'system:online:kickout', 2, 2, TRUE, 0),
    (250, 32, '查看服务监控', 'system:monitor:server', 2, 1, TRUE, 0),
    (260, 33, '查看Redis监控', 'system:monitor:redis', 2, 1, TRUE, 0),
    (270, 35, '查询阻止名单', 'system:blocklist:list', 2, 1, TRUE, 0),
    (271, 35, '新增阻止名单', 'system:blocklist:add', 2, 2, TRUE, 0),
    (272, 35, '编辑阻止名单', 'system:blocklist:edit', 2, 3, TRUE, 0),
    (273, 35, '删除阻止名单', 'system:blocklist:delete', 2, 4, TRUE, 0),
    (280, 41, '查询文件', 'system:file:list', 2, 1, TRUE, 0),
    (281, 40, '查询图库', 'system:gallery:list', 2, 1, TRUE, 0),
    (282, 41, '上传文件', 'system:file:upload', 2, 2, TRUE, 0),
    (283, 41, '删除文件', 'system:file:delete', 2, 3, TRUE, 0),
    (284, 40, '上传图库', 'system:gallery:upload', 2, 2, TRUE, 0),
    (285, 40, '删除图库', 'system:gallery:delete', 2, 3, TRUE, 0),
    (286, 41, '设置文件权限', 'system:file:permission', 2, 4, TRUE, 0),
    (287, 40, '设置图库权限', 'system:gallery:permission', 2, 4, TRUE, 0),
    (289, 42, '查询资源', 'system:resource:list', 2, 1, TRUE, 0),
    (290, 42, '上传资源', 'system:resource:upload', 2, 2, TRUE, 0),
    (291, 42, '删除资源', 'system:resource:delete', 2, 3, TRUE, 0),
    (292, 42, '设置资源权限', 'system:resource:permission', 2, 4, TRUE, 0),
    (288, 41, '清理孤儿文件', 'system:file:cleanup', 2, 5, TRUE, 0),
    (901, 900, '查询上传策略', 'system:upload-policy:list', 2, 1, TRUE, 0),
    (902, 900, '编辑上传策略', 'system:upload-policy:edit', 2, 2, TRUE, 0),

    (180, 18, '编辑资料', 'system:profile:edit', 2, 1, TRUE, 0),
    (181, 18, '修改密码', 'system:profile:password', 2, 2, TRUE, 0),

    (190, 1, '仪表盘-用户', 'system:dashboard:user', 2, 1, TRUE, 0),
    (191, 1, '仪表盘-帖子', 'system:dashboard:post', 2, 2, TRUE, 0),
    (192, 1, '仪表盘-公告-最新', 'system:dashboard:notice:list', 2, 3, TRUE, 0),
    (198, 1, '仪表盘-公告-概览', 'system:dashboard:notice:overview', 2, 4, TRUE, 0),
    (193, 1, '仪表盘-运维', 'system:dashboard:ops', 2, 5, TRUE, 0),
    (194, 1, '仪表盘-登录', 'system:dashboard:login', 2, 6, TRUE, 0),
    (195, 1, '仪表盘-操作日志', 'system:dashboard:operlog', 2, 7, TRUE, 0),
    (196, 1, '仪表盘-举报', 'system:dashboard:report', 2, 8, TRUE, 0),
    (197, 1, '仪表盘-审核', 'system:dashboard:verify', 2, 9, TRUE, 0),

    (200, 20, '查询帖子', 'content:post:list', 2, 1, TRUE, 0),
    (201, 20, '新增帖子', 'content:post:add', 2, 2, TRUE, 0),
    (202, 20, '编辑帖子', 'content:post:edit', 2, 3, TRUE, 0),
    (203, 20, '删除帖子', 'content:post:delete', 2, 4, TRUE, 0),
    (204, 20, '标记已解决', 'content:post:resolve', 2, 5, TRUE, 0),
    (205, 20, '驳回帖子', 'content:post:reject', 2, 6, TRUE, 0),
    (206, 20, '置顶帖子', 'content:post:pin', 2, 7, TRUE, 0),
    (207, 20, '下架帖子', 'content:post:offline', 2, 8, TRUE, 0),
    (208, 20, '上架帖子', 'content:post:online', 2, 9, TRUE, 0),
    (209, 20, '锁定帖子', 'content:post:lock', 2, 10, TRUE, 0),
    (214, 20, '解锁帖子', 'content:post:unlock', 2, 11, TRUE, 0),
    (215, 20, '标记已售出', 'content:post:sold', 2, 12, TRUE, 0),

    (210, 21, '查询评论', 'content:comment:list', 2, 1, TRUE, 0),
    (211, 21, '删除评论', 'content:comment:delete', 2, 2, TRUE, 0),
    (212, 21, '编辑评论', 'content:comment:edit', 2, 3, TRUE, 0),
    (213, 21, '批量删除评论', 'content:comment:batch-delete', 2, 4, TRUE, 0),

    (220, 22, '查询举报', 'content:report:list', 2, 1, TRUE, 0),
    (221, 22, '处理举报', 'content:report:handle', 2, 2, TRUE, 0),
    (222, 22, '批量处理举报', 'content:report:batch-handle', 2, 3, TRUE, 0),
    (223, 22, '删除举报', 'content:report:delete', 2, 4, TRUE, 0),

    (230, 23, '查询审核', 'content:verification:list', 2, 1, TRUE, 0),
    (232, 23, '查看审核', 'content:verification:view', 2, 2, TRUE, 0),
    (231, 23, '处理审核', 'content:verification:handle', 2, 3, TRUE, 0),
    (233, 23, '查看学号白名单', 'content:verification:whitelist:list', 2, 4, TRUE, 0),
    (234, 23, '编辑学号白名单', 'content:verification:whitelist:edit', 2, 5, TRUE, 0),

    (340, 20, '频道-表白墙-查看', 'content:channel:confessions:view', 2, 20, TRUE, 0),
    (341, 20, '频道-表白墙-管理', 'content:channel:confessions:manage', 2, 21, TRUE, 0),
    (342, 20, '频道-树洞-查看', 'content:channel:treehole:view', 2, 22, TRUE, 0),
    (343, 20, '频道-树洞-管理', 'content:channel:treehole:manage', 2, 23, TRUE, 0),
    (344, 20, '频道-求助-查看', 'content:channel:help:view', 2, 24, TRUE, 0),
    (345, 20, '频道-求助-管理', 'content:channel:help:manage', 2, 25, TRUE, 0),
    (346, 20, '频道-市集-查看', 'content:channel:market:view', 2, 26, TRUE, 0),
    (347, 20, '频道-市集-管理', 'content:channel:market:manage', 2, 27, TRUE, 0),
    (348, 20, '频道-失物-查看', 'content:channel:lost-found:view', 2, 28, TRUE, 0),
    (349, 20, '频道-失物-管理', 'content:channel:lost-found:manage', 2, 29, TRUE, 0),

    (300, 24, '查看帖子回收站', 'content:recycle:post:list', 2, 1, TRUE, 0),
    (301, 24, '恢复帖子', 'content:recycle:post:restore', 2, 2, TRUE, 0),
    (302, 24, '彻底删除帖子', 'content:recycle:post:purge', 2, 3, TRUE, 0),
    (310, 24, '查看评论回收站', 'content:recycle:comment:list', 2, 4, TRUE, 0),
    (311, 24, '恢复评论', 'content:recycle:comment:restore', 2, 5, TRUE, 0),
    (312, 24, '彻底删除评论', 'content:recycle:comment:purge', 2, 6, TRUE, 0),
    (320, 24, '查看举报回收站', 'content:recycle:report:list', 2, 7, TRUE, 0),
    (321, 24, '恢复举报', 'content:recycle:report:restore', 2, 8, TRUE, 0),
    (322, 24, '彻底删除举报', 'content:recycle:report:purge', 2, 9, TRUE, 0);

-- 菜单分组编码（用于侧边栏分组）
UPDATE sys_menus
SET group_code = CASE
    WHEN id = 1 THEN 'WORKBENCH'
    WHEN id = 2 THEN 'SYSTEM'
    WHEN id = 3 THEN 'CONTENT'
    WHEN id = 5 THEN 'ASSET'
    WHEN id = 4 THEN 'MONITOR'
    WHEN id = 6 THEN 'CAMPUS'
    WHEN id = 8 THEN 'AUDIT'
    ELSE 'GENERAL'
END
WHERE parent_id = 0;

WITH RECURSIVE menu_tree AS (
    SELECT id, parent_id, group_code
    FROM sys_menus
    WHERE parent_id = 0
      AND group_code IS NOT NULL
      AND group_code <> ''
    UNION ALL
    SELECT c.id, c.parent_id, mt.group_code
    FROM sys_menus c
    INNER JOIN menu_tree mt ON c.parent_id = mt.id
)
UPDATE sys_menus m
SET group_code = mt.group_code
FROM menu_tree mt
WHERE m.id = mt.id;

SELECT setval('sys_menus_id_seq', (SELECT MAX(id) FROM sys_menus));

-- 5) 角色-菜单关系
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus;

-- 版主角色：内容管理 + 个人中心
INSERT INTO sys_role_menus (role_id, menu_id) VALUES
    (2, 1),
    (2, 3), (2, 20), (2, 21), (2, 15), (2, 22), (2, 28), (2, 24),
    (2, 150), (2, 151), (2, 152), (2, 153), (2, 154), (2, 155),
    (2, 200), (2, 201), (2, 202), (2, 203), (2, 204), (2, 205), (2, 206), (2, 207), (2, 208), (2, 209), (2, 214), (2, 215),
    (2, 340), (2, 341), (2, 342), (2, 343), (2, 344), (2, 345), (2, 346), (2, 347), (2, 348), (2, 349),
    (2, 210), (2, 211), (2, 212), (2, 213),
    (2, 220), (2, 221), (2, 222), (2, 223), (2, 230), (2, 232), (2, 231),
    (2, 300), (2, 301), (2, 302), (2, 310), (2, 311), (2, 312), (2, 320), (2, 321), (2, 322),
    (2, 5), (2, 40), (2, 41),
    (2, 280), (2, 281), (2, 282), (2, 283), (2, 284), (2, 285), (2, 286), (2, 287), (2, 288),
    (2, 18);

-- 6) 上传策略
INSERT INTO sys_upload_policies (scene_code, scene_name, asset_type, visibility) VALUES
    ('post', '帖子图片', 'gallery', NULL),
    ('comment', '评论图片', 'gallery', NULL),
    ('avatar', '头像', 'gallery', NULL),
    ('id_card', '身份材料', 'resource', 'PRIVATE'),
    ('public', '公共上传（前台）', 'gallery', NULL),
    ('package', '安装包', 'resource', 'PUBLIC'),
    ('file', '业务附件库', 'file', NULL),
    ('gallery', '公共媒体库', 'gallery', NULL),
    ('resource', '系统资源库', 'resource', NULL),
    ('campus.hero', '校园展示位', 'resource', NULL),
    ('campus.school', '校园信息', 'resource', NULL);

-- 7) URL 权限映射（动态）
DELETE FROM sys_api_permissions;

-- Public
INSERT INTO sys_api_permissions (url, http_method, permission, description, status) VALUES
    ('/api/v1/auth/login', 'POST', 'public', 'auth.login', TRUE),
    ('/api/v1/auth/login-captcha', 'GET', 'public', 'auth.login.captcha', TRUE),
    ('/api/v1/auth/register', 'POST', 'public', 'auth.register', TRUE),
    ('/api/v1/auth/register-email-code', 'POST', 'public', 'auth.register.email.code', TRUE),
    ('/api/v1/auth/admin-contact', 'GET', 'public', 'auth.admin.contact', TRUE),
    ('/api/v1/auth/refresh', 'POST', 'public', 'auth.refresh', TRUE),
    ('/api/v1/setup/status', 'GET', 'public', 'setup.status', TRUE),
    ('/api/v1/setup/init', 'POST', 'public', 'setup.init', TRUE),
    ('/api/v1/notices/public', 'GET', 'public', 'notice.public.list', TRUE),
    ('/api/v1/notices/public/**', 'GET', 'public', 'notice.public', TRUE),
    ('/api/v1/posts', 'GET', 'public', 'post.list.public', TRUE),
    ('/api/v1/comments/post/*', 'GET', 'public', 'comment.list.public', TRUE),
    ('/api/v1/comments/post/*/page', 'GET', 'public', 'comment.list.page.public', TRUE),
    ('/api/v1/search/posts', 'GET', 'public', 'search.posts.public', TRUE),
    ('/api/v1/users/*/posts', 'GET', 'public', 'user.posts.public', TRUE),
    ('/api/v1/campus/heroes/*', 'GET', 'public', 'campus.hero.public', TRUE),
    ('/api/v1/files/preview/*', 'GET', 'public', 'file.preview.public', TRUE);

-- Login-only (no menu permission)
INSERT INTO sys_api_permissions (url, http_method, permission, description, status) VALUES
    ('/api/v1/auth/logout', 'POST', 'login', 'auth.logout', TRUE),
    ('/api/v1/auth/info', 'GET', 'login', 'auth.info', TRUE),
    ('/api/v1/auth/password', 'PUT', 'login', 'auth.password', TRUE),
    ('/api/v1/auth/verify-email', 'POST', 'login', 'auth.verify.email', TRUE),
    ('/api/v1/auth/confirm-email', 'POST', 'login', 'auth.confirm.email', TRUE),
    ('/api/v1/auth/submit-id-card', 'POST', 'login', 'auth.submit.idcard', TRUE),
    ('/api/v1/auth/submit-student-id', 'POST', 'login', 'auth.submit.student.id', TRUE),
    ('/api/v1/auth/verification/cancel', 'POST', 'login', 'auth.verify.cancel', TRUE),
    ('/api/v1/users/*', 'GET', 'login', 'user.detail', TRUE),
    ('/api/v1/users/bookmarks', 'GET', 'login', 'user.bookmarks', TRUE),
    ('/api/v1/users/credit', 'GET', 'login', 'user.credit', TRUE),
    ('/api/v1/users/me', 'GET', 'login', 'user.me', TRUE),
    ('/api/v1/users/me', 'PUT', 'login', 'user.me.update', TRUE),
    ('/api/v1/users/me', 'POST', 'login', 'user.me.update', TRUE),
    ('/api/v1/posts/*', 'GET', 'login', 'post.detail', TRUE),
    ('/api/v1/posts/*/view', 'POST', 'login', 'post.view', TRUE),
    ('/api/v1/posts', 'POST', 'login', 'post.create', TRUE),
    ('/api/v1/posts/*', 'PUT', 'login', 'post.update', TRUE),
    ('/api/v1/posts/*', 'DELETE', 'login', 'post.delete', TRUE),
    ('/api/v1/posts/*/resolve', 'PUT', 'login', 'post.resolve', TRUE),
    ('/api/v1/posts/*/sold', 'PUT', 'login', 'post.sold', TRUE),
    ('/api/v1/posts/*/like', 'POST', 'login', 'post.like', TRUE),
    ('/api/v1/posts/*/like', 'DELETE', 'login', 'post.unlike', TRUE),
    ('/api/v1/posts/*/bookmark', 'POST', 'login', 'post.bookmark', TRUE),
    ('/api/v1/posts/*/bookmark', 'DELETE', 'login', 'post.unbookmark', TRUE),
    ('/api/v1/posts/bookmarks/batch', 'POST', 'login', 'post.bookmark.batch', TRUE),
    ('/api/v1/posts/bookmarks', 'GET', 'login', 'post.bookmarks', TRUE),
    ('/api/v1/posts/search', 'GET', 'login', 'post.search', TRUE),
    ('/api/v1/posts/*/report', 'POST', 'login', 'post.report', TRUE),
    ('/api/v1/posts/reports/batch', 'POST', 'login', 'post.report.batch', TRUE),
    ('/api/v1/comments', 'POST', 'login', 'comment.create', TRUE),
    ('/api/v1/comments/*', 'DELETE', 'login', 'comment.delete', TRUE),
    ('/api/v1/market/orders', 'POST', 'login', 'market.order.create', TRUE),
    ('/api/v1/market/orders/*', 'GET', 'login', 'market.order.detail', TRUE),
    ('/api/v1/market/orders/*/buyer-confirm', 'PUT', 'login', 'market.order.buyer.confirm', TRUE),
    ('/api/v1/market/orders/*/seller-confirm', 'PUT', 'login', 'market.order.seller.confirm', TRUE),
    ('/api/v1/market/orders/*/cancel', 'PUT', 'login', 'market.order.cancel', TRUE),
    ('/api/v1/market/orders/buyer', 'GET', 'login', 'market.order.buyer.list', TRUE),
    ('/api/v1/market/orders/seller', 'GET', 'login', 'market.order.seller.list', TRUE),
    ('/api/v1/market/orders/credit', 'GET', 'login', 'market.credit', TRUE),
    ('/api/v1/market/orders/can-post', 'GET', 'login', 'market.can.post', TRUE),
    ('/api/v1/files/upload', 'POST', 'login', 'file.upload', TRUE),
    ('/api/v1/files/*', 'DELETE', 'login', 'file.delete', TRUE),
    ('/api/v1/system/menu/routes', 'GET', 'login', 'menu.routes', TRUE),
    ('/api/v1/system/user/permissions', 'GET', 'login', 'user.permissions', TRUE),
    ('/api/v1/notices', 'GET', 'login', 'notice.visible', TRUE),
    ('/api/v1/notices/*', 'GET', 'login', 'notice.detail', TRUE),
    ('/api/v1/console/statistics/*', 'GET', 'login', 'console.statistics', TRUE);

-- System / Console (permission-bound)
INSERT INTO sys_api_permissions (url, http_method, permission, description, status) VALUES
    ('/api/v1/console/config/email-domains', 'GET', 'system:config:email-domains:view', 'console.config.email.domains.list', TRUE),
    ('/api/v1/console/config/email-domains', 'PUT', 'system:config:email-domains:edit', 'console.config.email.domains.update', TRUE),
    ('/api/v1/console/config/smtp', 'GET', 'system:config:smtp:view', 'console.config.smtp.get', TRUE),
    ('/api/v1/console/config/smtp', 'PUT', 'system:config:smtp:edit', 'console.config.smtp.update', TRUE),
    ('/api/v1/console/config/smtp/test', 'POST', 'system:config:smtp:test', 'console.config.smtp.test', TRUE),
    ('/api/v1/console/config/email-templates', 'GET', 'system:config:email-templates:view', 'console.config.email.templates.get', TRUE),
    ('/api/v1/console/config/email-templates', 'PUT', 'system:config:email-templates:edit', 'console.config.email.templates.update', TRUE),
    ('/api/v1/console/config/student-ids', 'GET', 'content:verification:whitelist:list', 'console.config.student.ids.get', TRUE),
    ('/api/v1/console/config/student-ids', 'PUT', 'content:verification:whitelist:edit', 'console.config.student.ids.update', TRUE),
    ('/api/v1/console/upload-policies', 'GET', 'system:upload-policy:list', 'console.upload.policy.list', TRUE),
    ('/api/v1/console/upload-policies/*', 'PUT', 'system:upload-policy:edit', 'console.upload.policy.update', TRUE),
    ('/api/v1/console/users', 'GET', 'system:user:list', 'console.users.list', TRUE),
    ('/api/v1/console/users/deleted', 'GET', 'system:user:list', 'console.users.deleted.list', TRUE),
    ('/api/v1/console/users/*', 'GET', 'system:user:list', 'console.users.detail', TRUE),
    ('/api/v1/console/users', 'POST', 'system:user:add', 'console.users.add', TRUE),
    ('/api/v1/console/users/*', 'PUT', 'system:user:edit', 'console.users.edit', TRUE),
    ('/api/v1/console/users', 'DELETE', 'system:user:delete', 'console.users.delete', TRUE),
    ('/api/v1/console/users/*/role', 'PUT', 'system:user:role', 'console.users.role', TRUE),
    ('/api/v1/console/users/batch-role', 'PUT', 'system:user:role', 'console.users.role.batch', TRUE),
    ('/api/v1/console/users/batch-assign', 'POST', 'system:user:role', 'console.users.assign.by.query', TRUE),
    ('/api/v1/console/users/*/ban', 'PUT', 'system:user:ban', 'console.users.ban', TRUE),
    ('/api/v1/console/users/*/unban', 'PUT', 'system:user:unban', 'console.users.unban', TRUE),
    ('/api/v1/console/users/export', 'GET', 'system:user:export', 'console.users.export', TRUE),
    ('/api/v1/console/users/import', 'POST', 'system:user:import', 'console.users.import', TRUE),
    ('/api/v1/console/users/template', 'GET', 'system:user:import', 'console.users.template', TRUE),
    ('/api/v1/console/users/*/restore', 'PUT', 'system:user:restore', 'console.users.restore', TRUE),
    ('/api/v1/console/users/*/purge', 'DELETE', 'system:user:purge', 'console.users.purge', TRUE),
    ('/api/v1/console/posts', 'GET', 'content:post:list', 'console.posts.list', TRUE),
    ('/api/v1/console/posts', 'POST', 'content:post:add', 'console.posts.add', TRUE),
    ('/api/v1/console/posts/*', 'PUT', 'content:post:edit', 'console.posts.edit', TRUE),
    ('/api/v1/console/posts/*', 'DELETE', 'content:post:delete', 'console.posts.delete', TRUE),
    ('/api/v1/console/posts/*/resolve', 'PUT', 'content:post:resolve', 'console.posts.resolve', TRUE),
    ('/api/v1/console/posts/*/sold', 'PUT', 'content:post:sold', 'console.posts.sold', TRUE),
    ('/api/v1/console/posts/*/reject', 'PUT', 'content:post:reject', 'console.posts.reject', TRUE),
    ('/api/v1/console/posts/*/pin', 'PUT', 'content:post:pin', 'console.posts.pin', TRUE),
    ('/api/v1/console/posts/*/unpin', 'PUT', 'content:post:pin', 'console.posts.unpin', TRUE),
    ('/api/v1/console/posts/*/offline', 'PUT', 'content:post:offline', 'console.posts.offline', TRUE),
    ('/api/v1/console/posts/*/online', 'PUT', 'content:post:online', 'console.posts.online', TRUE),
    ('/api/v1/console/posts/*/lock', 'PUT', 'content:post:lock', 'console.posts.lock', TRUE),
    ('/api/v1/console/posts/*/unlock', 'PUT', 'content:post:unlock', 'console.posts.unlock', TRUE),
    ('/api/v1/console/comments', 'GET', 'content:comment:list', 'console.comments.list', TRUE),
    ('/api/v1/console/comments/*', 'DELETE', 'content:comment:delete', 'console.comments.delete', TRUE),
    ('/api/v1/console/comments/batch-delete', 'POST', 'content:comment:batch-delete', 'console.comments.batch.delete', TRUE),
    ('/api/v1/console/comments/*', 'PUT', 'content:comment:edit', 'console.comments.edit', TRUE),
    ('/api/v1/console/reports', 'GET', 'content:report:list', 'console.reports.list', TRUE),
    ('/api/v1/console/reports/*', 'GET', 'content:report:list', 'console.reports.detail', TRUE),
    ('/api/v1/console/reports/*/handle', 'PUT', 'content:report:handle', 'console.reports.handle', TRUE),
    ('/api/v1/console/reports/batch-handle', 'POST', 'content:report:batch-handle', 'console.reports.batch.handle', TRUE),
    ('/api/v1/console/reports/*', 'DELETE', 'content:report:delete', 'console.reports.delete', TRUE),
    ('/api/v1/console/verifications', 'GET', 'content:verification:list', 'console.verifications.list', TRUE),
    ('/api/v1/console/verifications/*', 'GET', 'content:verification:view', 'console.verifications.detail', TRUE),
    ('/api/v1/console/verifications/*', 'PUT', 'content:verification:handle', 'console.verifications.handle', TRUE),
    ('/api/v1/console/recycle/posts', 'GET', 'content:recycle:post:list', 'console.recycle.post.list', TRUE),
    ('/api/v1/console/recycle/posts/*/restore', 'PUT', 'content:recycle:post:restore', 'console.recycle.post.restore', TRUE),
    ('/api/v1/console/recycle/posts/*', 'DELETE', 'content:recycle:post:purge', 'console.recycle.post.purge', TRUE),
    ('/api/v1/console/recycle/comments', 'GET', 'content:recycle:comment:list', 'console.recycle.comment.list', TRUE),
    ('/api/v1/console/recycle/comments/*/restore', 'PUT', 'content:recycle:comment:restore', 'console.recycle.comment.restore', TRUE),
    ('/api/v1/console/recycle/comments/*', 'DELETE', 'content:recycle:comment:purge', 'console.recycle.comment.purge', TRUE),
    ('/api/v1/console/recycle/reports', 'GET', 'content:recycle:report:list', 'console.recycle.report.list', TRUE),
    ('/api/v1/console/recycle/reports/*/restore', 'PUT', 'content:recycle:report:restore', 'console.recycle.report.restore', TRUE),
    ('/api/v1/console/recycle/reports/*', 'DELETE', 'content:recycle:report:purge', 'console.recycle.report.purge', TRUE),
    ('/api/v1/console/notices', 'GET', 'system:notice:list', 'console.notice.list', TRUE),
    ('/api/v1/console/notices/*', 'GET', 'system:notice:list', 'console.notice.detail', TRUE),
    ('/api/v1/console/notices', 'POST', 'system:notice:add', 'console.notice.add', TRUE),
    ('/api/v1/console/notices/*', 'PUT', 'system:notice:edit', 'console.notice.edit', TRUE),
    ('/api/v1/console/notices/*/publish', 'PUT', 'system:notice:publish', 'console.notice.publish', TRUE),
    ('/api/v1/console/notices/*/offline', 'PUT', 'system:notice:offline', 'console.notice.offline', TRUE),
    ('/api/v1/console/notices/*', 'DELETE', 'system:notice:delete', 'console.notice.delete', TRUE),
    ('/api/v1/console/login-logs', 'GET', 'system:loginlog:list', 'console.loginlog.list', TRUE),
    ('/api/v1/console/login-logs/*', 'DELETE', 'system:loginlog:delete', 'console.loginlog.delete', TRUE),
    ('/api/v1/console/login-logs/clear', 'DELETE', 'system:loginlog:clear', 'console.loginlog.clear', TRUE),
    ('/api/v1/console/login-logs/export', 'GET', 'system:loginlog:export', 'console.loginlog.export', TRUE),
    ('/api/v1/console/oper-logs', 'GET', 'system:operlog:list', 'console.operlog.list', TRUE),
    ('/api/v1/console/oper-logs/*', 'DELETE', 'system:operlog:delete', 'console.operlog.delete', TRUE),
    ('/api/v1/console/oper-logs/clear', 'DELETE', 'system:operlog:clear', 'console.operlog.clear', TRUE),
    ('/api/v1/console/oper-logs/export', 'GET', 'system:operlog:export', 'console.operlog.export', TRUE),
    ('/api/v1/console/online-users', 'GET', 'system:online:list', 'console.online.list', TRUE),
    ('/api/v1/console/online-users/kickout', 'POST', 'system:online:kickout', 'console.online.kickout', TRUE),
    ('/api/v1/console/monitor/server', 'GET', 'system:monitor:server', 'console.monitor.server', TRUE),
    ('/api/v1/console/monitor/redis', 'GET', 'system:monitor:redis', 'console.monitor.redis', TRUE),
    ('/api/v1/console/monitor/blocklist', 'GET', 'system:blocklist:list', 'console.blocklist.list', TRUE),
    ('/api/v1/console/monitor/blocklist', 'POST', 'system:blocklist:add', 'console.blocklist.add', TRUE),
    ('/api/v1/console/monitor/blocklist/*', 'PUT', 'system:blocklist:edit', 'console.blocklist.edit', TRUE),
    ('/api/v1/console/monitor/blocklist/*', 'DELETE', 'system:blocklist:delete', 'console.blocklist.delete', TRUE),
    ('/api/v1/console/monitor/blocklist/batch', 'POST', 'system:blocklist:add', 'console.blocklist.batch', TRUE),
    ('/api/v1/console/files', 'GET', 'system:file:list', 'console.file.list', TRUE),
    ('/api/v1/console/files/categories', 'GET', 'system:file:list', 'console.file.categories', TRUE),
    ('/api/v1/console/files/upload', 'POST', 'system:file:upload', 'console.file.upload', TRUE),
    ('/api/v1/console/files/*', 'DELETE', 'system:file:delete', 'console.file.delete', TRUE),
    ('/api/v1/console/files/batch-delete', 'POST', 'system:file:delete', 'console.file.batch.delete', TRUE),
    ('/api/v1/console/files/*/visibility', 'POST', 'system:file:permission', 'console.file.permission', TRUE),
    ('/api/v1/console/files/cleanup/config', 'GET', 'system:file:cleanup', 'console.file.cleanup.config.get', TRUE),
    ('/api/v1/console/files/cleanup/config', 'PUT', 'system:file:cleanup', 'console.file.cleanup.config.put', TRUE),
    ('/api/v1/console/files/cleanup', 'POST', 'system:file:cleanup', 'console.file.cleanup', TRUE),
    ('/api/v1/console/gallery', 'GET', 'system:gallery:list', 'console.gallery.list', TRUE),
    ('/api/v1/console/gallery/categories', 'GET', 'system:gallery:list', 'console.gallery.categories', TRUE),
    ('/api/v1/console/gallery/upload', 'POST', 'system:gallery:upload', 'console.gallery.upload', TRUE),
    ('/api/v1/console/gallery/*', 'DELETE', 'system:gallery:delete', 'console.gallery.delete', TRUE),
    ('/api/v1/console/gallery/batch-delete', 'POST', 'system:gallery:delete', 'console.gallery.batch.delete', TRUE),
    ('/api/v1/console/gallery/*/visibility', 'POST', 'system:gallery:permission', 'console.gallery.permission', TRUE),
    ('/api/v1/console/resources', 'GET', 'system:resource:list', 'console.resource.list', TRUE),
    ('/api/v1/console/resources/categories', 'GET', 'system:resource:list', 'console.resource.categories', TRUE),
    ('/api/v1/console/resources/upload', 'POST', 'system:resource:upload', 'console.resource.upload', TRUE),
    ('/api/v1/console/resources/*', 'DELETE', 'system:resource:delete', 'console.resource.delete', TRUE),
    ('/api/v1/console/resources/batch-delete', 'POST', 'system:resource:delete', 'console.resource.batch.delete', TRUE),
    ('/api/v1/console/resources/*/visibility', 'POST', 'system:resource:permission', 'console.resource.permission', TRUE),
    ('/api/v1/console/campus/heroes', 'GET', 'campus:hero:list', 'console.hero.list', TRUE),
    ('/api/v1/console/campus/heroes/*', 'GET', 'campus:hero:list', 'console.hero.detail', TRUE),
    ('/api/v1/console/campus/heroes', 'POST', 'campus:hero:add', 'console.hero.add', TRUE),
    ('/api/v1/console/campus/heroes/*', 'PUT', 'campus:hero:edit', 'console.hero.edit', TRUE),
    ('/api/v1/console/campus/heroes/*', 'DELETE', 'campus:hero:delete', 'console.hero.delete', TRUE),
    ('/api/v1/system/roles/list', 'GET', 'system:role:list', 'system.role.list', TRUE),
    ('/api/v1/system/roles/*', 'GET', 'system:role:view', 'system.role.detail', TRUE),
    ('/api/v1/system/roles', 'POST', 'system:role:add', 'system.role.add', TRUE),
    ('/api/v1/system/roles/*', 'PUT', 'system:role:edit', 'system.role.edit', TRUE),
    ('/api/v1/system/roles/*/enable', 'PUT', 'system:role:enable', 'system.role.enable', TRUE),
    ('/api/v1/system/roles/*/disable', 'PUT', 'system:role:disable', 'system.role.disable', TRUE),
    ('/api/v1/system/roles/*', 'DELETE', 'system:role:delete', 'system.role.delete', TRUE),
    ('/api/v1/system/roles/*/delete', 'POST', 'system:role:delete', 'system.role.delete.body', TRUE),
    ('/api/v1/system/roles/*/menus', 'GET', 'system:role:view', 'system.role.menus.get', TRUE),
    ('/api/v1/system/roles/*/menus', 'PUT', 'system:role:assign', 'system.role.menus.set', TRUE),
    ('/api/v1/system/roles/*/depts', 'GET', 'system:role:view', 'system.role.depts.get', TRUE),
    ('/api/v1/system/roles/*/depts', 'PUT', 'system:role:assign', 'system.role.depts.set', TRUE),
    ('/api/v1/system/roles/*/users', 'GET', 'system:role:list', 'system.role.users', TRUE),
    ('/api/v1/system/roles/dept-tree', 'GET', 'system:role:view', 'system.role.dept.tree', TRUE),
    ('/api/v1/system/dept/list', 'GET', 'system:dept:list', 'system.dept.list', TRUE),
    ('/api/v1/system/dept/tree', 'GET', 'system:dept:list', 'system.dept.tree', TRUE),
    ('/api/v1/system/dept/*', 'GET', 'system:dept:view', 'system.dept.detail', TRUE),
    ('/api/v1/system/dept', 'POST', 'system:dept:add', 'system.dept.add', TRUE),
    ('/api/v1/system/dept/*', 'PUT', 'system:dept:edit', 'system.dept.edit', TRUE),
    ('/api/v1/system/dept/*', 'DELETE', 'system:dept:delete', 'system.dept.delete', TRUE),
    ('/api/v1/system/dept/*/delete', 'POST', 'system:dept:delete', 'system.dept.delete.body', TRUE),
    ('/api/v1/system/dept/*/status', 'PUT', 'system:dept:edit', 'system.dept.status', TRUE),
    ('/api/v1/system/dept/*/users', 'GET', 'system:dept:view', 'system.dept.users', TRUE),
    ('/api/v1/system/dept/*/user-count', 'GET', 'system:dept:view', 'system.dept.user.count', TRUE),
    ('/api/v1/system/dept/*/move', 'PUT', 'system:dept:move', 'system.dept.move', TRUE),
    ('/api/v1/system/dept/*/sort', 'PUT', 'system:dept:sort', 'system.dept.sort', TRUE),
    ('/api/v1/system/dept/export', 'GET', 'system:dept:export', 'system.dept.export', TRUE),
    ('/api/v1/system/dept/import', 'POST', 'system:dept:import', 'system.dept.import', TRUE),
    ('/api/v1/system/dept/sync', 'POST', 'system:dept:sync', 'system.dept.sync', TRUE),
    ('/api/v1/system/auth-rules', 'GET', 'system:auth-rule:list', 'system.auth.rule.list', TRUE),
    ('/api/v1/system/auth-rules', 'POST', 'system:auth-rule:add', 'system.auth.rule.add', TRUE),
    ('/api/v1/system/auth-rules/*', 'GET', 'system:auth-rule:view', 'system.auth.rule.detail', TRUE),
    ('/api/v1/system/auth-rules/*', 'PUT', 'system:auth-rule:edit', 'system.auth.rule.edit', TRUE),
    ('/api/v1/system/auth-rules/*/status', 'PATCH', 'system:auth-rule:status', 'system.auth.rule.status', TRUE),
    ('/api/v1/system/auth-rules/*/priority', 'PATCH', 'system:auth-rule:sort', 'system.auth.rule.priority', TRUE),
    ('/api/v1/system/auth-rules/*/clone', 'POST', 'system:auth-rule:clone', 'system.auth.rule.clone', TRUE),
    ('/api/v1/system/auth-rules/*', 'DELETE', 'system:auth-rule:delete', 'system.auth.rule.delete', TRUE),
    ('/api/v1/system/sensitive-words', 'GET', 'system:sensitive-word:list', 'system.sensitive.list', TRUE),
    ('/api/v1/system/sensitive-words', 'POST', 'system:sensitive-word:add', 'system.sensitive.add', TRUE),
    ('/api/v1/system/sensitive-words/batch', 'POST', 'system:sensitive-word:batch:add', 'system.sensitive.batch', TRUE),
    ('/api/v1/system/sensitive-words/*', 'DELETE', 'system:sensitive-word:delete', 'system.sensitive.delete', TRUE),
    ('/api/v1/system/sensitive-words', 'DELETE', 'system:sensitive-word:batch:delete', 'system.sensitive.delete.batch', TRUE),
    ('/api/v1/system/sensitive-words/*', 'PUT', 'system:sensitive-word:edit', 'system.sensitive.edit', TRUE),
    ('/api/v1/system/menu/list', 'GET', 'system:role:view', 'system.menu.list', TRUE),
    ('/api/v1/system/api-permissions', 'GET', 'system:role:view', 'system.api.perm.list', TRUE),
    ('/api/v1/system/api-permissions', 'POST', 'system:role:assign', 'system.api.perm.add', TRUE),
    ('/api/v1/system/api-permissions/*', 'PUT', 'system:role:assign', 'system.api.perm.edit', TRUE),
    ('/api/v1/system/api-permissions/*', 'DELETE', 'system:role:assign', 'system.api.perm.delete', TRUE),
    ('/api/v1/system/api-permissions/refresh', 'POST', 'system:role:assign', 'system.api.perm.refresh', TRUE);

-- 6) Hero 默认配置
INSERT INTO campus_heroes (
    id, page_key, page_name, enabled, theme,
    title_start, title_highlight, description, badge,
    primary_btn_text, secondary_btn_text,
    show_stats, stats_number, stats_label,
    avatar_urls, float_card_label, float_card_value, sort_order
) VALUES
    (1, 'HOME', '首页', TRUE, 'blue',
     '连接每一份', '校园心声',
     'CampusWall 是一个连接校友、分享生活、互助成长的校园社区。在这里，每一个声音都值得被倾听。',
     'New v2.0 Released', '开始探索 🚀', '热门话题 🔥',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '热门动态', '+128', 1),
    (2, 'CONFESSIONS', '表白墙', TRUE, 'pink',
     '勇敢表达', '爱的声音',
     '暗恋、表白、祝福。在这里，大声说出你的爱。让心意传递，让缘分开始。',
     'Confessions Wall', '发布表白', '最新表白',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '今日表白', '99+ 条', 2),
    (3, 'TREEHOLE', '树洞', TRUE, 'emerald',
     '倾听内心', '真实树洞',
     '匿名倾诉，释放压力。在这里，做最真实的自己。我们是你忠实的倾听者。',
     'Anonymous Treehole', '发布心声', '查看树洞',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '新收录', '58 个秘密', 3),
    (4, 'HELP', '求助', TRUE, 'blue',
     '互帮互助', '共同成长',
     '学业困惑、生活难题、求职经验。在这里，寻找答案，分享经验，温暖彼此。',
     'Q&A Help', '发起求助', '我来解答',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '已解决', '1,203 个问题', 4),
    (5, 'MARKET', '市集', TRUE, 'orange',
     '旧物新生', '跳蚤市场',
     '教材书籍、数码电子、生活用品。在这里，让闲置物品流转，发现物美价廉的宝贝。',
     'Flea Market', '发布闲置', '逛逛市场',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '今日上新', '45 件好物', 5),
    (6, 'LOST_FOUND', '失物', TRUE, 'purple',
     '寻找失物', '传递温暖',
     '丢失物品、捡到失物。在这里，发布信息，让物品回归主人，让善意流转。',
     'Lost & Found', '发布信息', '最近信息',
     TRUE, '12,000+', '同学已加入', '[]'::jsonb, '寻回率', '85%', 6);

SELECT setval('campus_heroes_id_seq', (SELECT MAX(id) FROM campus_heroes));

-- 系统配置
INSERT INTO sys_config (id, config_key, config_value, config_type, remark) VALUES
    (1, 'email.allowed_domains', '["edu.cn"]', 'json', '允许的教育邮箱域名后缀'),
    (2, 'email.verification_enabled', 'true', 'boolean', '是否启用邮箱认证');

SELECT setval('sys_config_id_seq', (SELECT MAX(id) FROM sys_config));
