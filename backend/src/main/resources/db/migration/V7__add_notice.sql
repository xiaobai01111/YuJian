-- 通知公告表
CREATE TABLE sys_notice (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,  -- 0:草稿 1:已发布 2:已下线
    scope_type VARCHAR(20) NOT NULL DEFAULT 'ALL',  -- ALL:全校 DEPT:部门 USERS:指定用户
    scope_ids TEXT,  -- 部门ID或用户ID列表，JSON数组格式
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    published_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_notice_status ON sys_notice(status);
CREATE INDEX idx_notice_scope_type ON sys_notice(scope_type);
CREATE INDEX idx_notice_is_pinned ON sys_notice(is_pinned);
CREATE INDEX idx_notice_published_at ON sys_notice(published_at);
CREATE INDEX idx_notice_start_at ON sys_notice(start_at);
CREATE INDEX idx_notice_end_at ON sys_notice(end_at);

-- 添加公告管理权限菜单
INSERT INTO sys_menus (parent_id, name, type, path, component, perms, icon, sort_order, status, created_at, updated_at)
VALUES 
    (1, '公告管理', 1, 'notice', 'console/notice/index', 'system:notice:list', 'Bell', 6, 0, NOW(), NOW());

-- 获取刚插入的菜单ID
DO $$
DECLARE
    notice_menu_id BIGINT;
BEGIN
    SELECT id INTO notice_menu_id FROM sys_menus WHERE name = '公告管理' AND parent_id = 1;
    
    -- 添加按钮权限
    INSERT INTO sys_menus (parent_id, name, type, path, perms, sort_order, status, created_at, updated_at)
    VALUES 
        (notice_menu_id, '新增公告', 2, '', 'system:notice:add', 1, 0, NOW(), NOW()),
        (notice_menu_id, '编辑公告', 2, '', 'system:notice:edit', 2, 0, NOW(), NOW()),
        (notice_menu_id, '发布公告', 2, '', 'system:notice:publish', 3, 0, NOW(), NOW()),
        (notice_menu_id, '下线公告', 2, '', 'system:notice:offline', 4, 0, NOW(), NOW()),
        (notice_menu_id, '删除公告', 2, '', 'system:notice:delete', 5, 0, NOW(), NOW());
    
    -- 给管理员角色分配公告权限
    INSERT INTO sys_role_menus (role_id, menu_id)
    SELECT 1, id FROM sys_menus WHERE parent_id = notice_menu_id OR id = notice_menu_id;
END $$;
