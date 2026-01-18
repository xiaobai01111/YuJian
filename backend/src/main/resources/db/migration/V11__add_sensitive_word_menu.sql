-- 添加敏感词管理菜单（放在系统管理下）
INSERT INTO sys_menus (parent_id, name, path, component, icon, sort_order, type, perms, visible, created_at, updated_at)
SELECT id, '敏感词管理', 'sensitive-word', 'console/sensitive-word/index', 'warning', 55, 1, NULL, true, NOW(), NOW()
FROM sys_menus WHERE name = '系统管理' AND parent_id = 0;

-- 添加敏感词相关权限
INSERT INTO sys_api_permissions (url, http_method, permission, description, status, created_at, updated_at)
VALUES 
('/api/v1/system/sensitive-words', 'GET', 'system:sensitive-word:list', '查询敏感词列表', true, NOW(), NOW()),
('/api/v1/system/sensitive-words', 'POST', 'system:sensitive-word:add', '添加敏感词', true, NOW(), NOW()),
('/api/v1/system/sensitive-words/batch', 'POST', 'system:sensitive-word:add', '批量导入敏感词', true, NOW(), NOW()),
('/api/v1/system/sensitive-words/*', 'PUT', 'system:sensitive-word:edit', '编辑敏感词', true, NOW(), NOW()),
('/api/v1/system/sensitive-words/*', 'DELETE', 'system:sensitive-word:delete', '删除敏感词', true, NOW(), NOW()),
('/api/v1/system/sensitive-words', 'DELETE', 'system:sensitive-word:delete', '批量删除敏感词', true, NOW(), NOW());

-- 给超级管理员角色添加敏感词权限
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 1, id FROM sys_menus WHERE name = '敏感词管理' AND component = 'console/sensitive-word/index'
ON CONFLICT DO NOTHING;
