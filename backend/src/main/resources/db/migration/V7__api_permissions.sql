-- =============================================
-- API权限配置表
-- 用于动态配置URL与权限标识的映射关系
-- =============================================

CREATE TABLE IF NOT EXISTS sys_api_permissions (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT '*',
    permission VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_permissions IS 'API权限配置表';
COMMENT ON COLUMN sys_api_permissions.url IS 'API路径（支持Ant风格）';
COMMENT ON COLUMN sys_api_permissions.http_method IS 'HTTP方法（GET/POST/PUT/DELETE/*）';
COMMENT ON COLUMN sys_api_permissions.permission IS '权限标识';
COMMENT ON COLUMN sys_api_permissions.description IS '描述';
COMMENT ON COLUMN sys_api_permissions.status IS '是否启用';

-- 创建索引
CREATE INDEX idx_api_perm_url ON sys_api_permissions(url);
CREATE INDEX idx_api_perm_status ON sys_api_permissions(status);

-- =============================================
-- 初始化系统管理模块的API权限配置
-- =============================================

-- 用户管理
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/console/user/list', 'GET', 'system:user:list', '用户列表'),
('/api/v1/console/user', 'POST', 'system:user:add', '新增用户'),
('/api/v1/console/user/*', 'PUT', 'system:user:edit', '编辑用户'),
('/api/v1/console/user/*', 'DELETE', 'system:user:delete', '删除用户'),
('/api/v1/console/user/*/status', 'PUT', 'system:user:edit', '修改用户状态'),
('/api/v1/console/user/export', 'GET', 'system:user:export', '导出用户'),
('/api/v1/console/user/import', 'POST', 'system:user:import', '导入用户');

-- 角色管理
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/system/role', 'GET', 'system:role:list', '角色列表'),
('/api/v1/system/role', 'POST', 'system:role:add', '新增角色'),
('/api/v1/system/role/*', 'PUT', 'system:role:edit', '编辑角色'),
('/api/v1/system/role/*', 'DELETE', 'system:role:delete', '删除角色'),
('/api/v1/system/role/*/menus', 'PUT', 'system:role:assign', '分配菜单权限'),
('/api/v1/system/role/*/depts', 'PUT', 'system:role:assign', '分配数据权限');

-- 菜单管理
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/system/menu/list', 'GET', 'system:menu:list', '菜单列表'),
('/api/v1/system/menu', 'POST', 'system:menu:add', '新增菜单'),
('/api/v1/system/menu/*', 'PUT', 'system:menu:edit', '编辑菜单'),
('/api/v1/system/menu/*', 'DELETE', 'system:menu:delete', '删除菜单');

-- 部门管理
INSERT INTO sys_api_permissions (url, http_method, permission, description) VALUES
('/api/v1/system/dept/tree', 'GET', 'system:dept:list', '部门树'),
('/api/v1/system/dept', 'POST', 'system:dept:add', '新增部门'),
('/api/v1/system/dept/*', 'PUT', 'system:dept:edit', '编辑部门'),
('/api/v1/system/dept/*', 'DELETE', 'system:dept:delete', '删除部门');
