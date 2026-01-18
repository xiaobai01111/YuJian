-- 认证规则配置表（用于注册/认证后自动分配角色/部门）
CREATE TABLE IF NOT EXISTS sys_auth_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    trigger_type VARCHAR(20) NOT NULL, -- REGISTER / VERIFY
    verify_method VARCHAR(20),         -- EDU_EMAIL / MANUAL / SSO / ID_LIST / OCR
    match_type VARCHAR(20) DEFAULT 'ANY', -- ANY / EMAIL_DOMAIN / STUDENT_ID_PREFIX
    match_value VARCHAR(100),
    role_ids JSONB NOT NULL DEFAULT '[]',
    dept_id BIGINT REFERENCES sys_depts(id) ON DELETE SET NULL,
    priority INT DEFAULT 100,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_rules_trigger ON sys_auth_rules(trigger_type);
CREATE INDEX IF NOT EXISTS idx_auth_rules_method ON sys_auth_rules(verify_method);
CREATE INDEX IF NOT EXISTS idx_auth_rules_enabled ON sys_auth_rules(enabled);

-- 系统管理菜单增加「认证规则」
INSERT INTO sys_menus (parent_id, name, path, component, icon, sort_order, type, perms, visible, created_at, updated_at)
SELECT id, '认证规则', '/console/auth-rule', 'views/console/auth-rule/index.vue', 'id-card', 6, 1, NULL, true, NOW(), NOW()
FROM sys_menus WHERE name = '系统管理' AND parent_id = 0;

-- 按钮权限
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order)
SELECT id, '查询规则', 'system:auth-rule:list', 2, 1
FROM sys_menus WHERE name = '认证规则' AND path = '/console/auth-rule';

INSERT INTO sys_menus (parent_id, name, perms, type, sort_order)
SELECT id, '新增规则', 'system:auth-rule:add', 2, 2
FROM sys_menus WHERE name = '认证规则' AND path = '/console/auth-rule';

INSERT INTO sys_menus (parent_id, name, perms, type, sort_order)
SELECT id, '编辑规则', 'system:auth-rule:edit', 2, 3
FROM sys_menus WHERE name = '认证规则' AND path = '/console/auth-rule';

INSERT INTO sys_menus (parent_id, name, perms, type, sort_order)
SELECT id, '删除规则', 'system:auth-rule:delete', 2, 4
FROM sys_menus WHERE name = '认证规则' AND path = '/console/auth-rule';

-- API权限映射
INSERT INTO sys_api_permissions (url, http_method, permission, description, status, created_at, updated_at)
VALUES 
('/api/v1/system/auth-rules', 'GET', 'system:auth-rule:list', '查询认证规则', true, NOW(), NOW()),
('/api/v1/system/auth-rules', 'POST', 'system:auth-rule:add', '新增认证规则', true, NOW(), NOW()),
('/api/v1/system/auth-rules/*', 'PUT', 'system:auth-rule:edit', '编辑认证规则', true, NOW(), NOW()),
('/api/v1/system/auth-rules/*', 'DELETE', 'system:auth-rule:delete', '删除认证规则', true, NOW(), NOW());

-- 默认注册规则（若存在 role_key = 'user'）
INSERT INTO sys_auth_rules (name, enabled, trigger_type, verify_method, match_type, match_value, role_ids, dept_id, priority, remark)
SELECT '注册默认角色', TRUE, 'REGISTER', NULL, 'ANY', NULL, jsonb_build_array(id), NULL, 10, '注册后自动分配普通用户角色'
FROM sys_roles WHERE role_key = 'user'
LIMIT 1;
