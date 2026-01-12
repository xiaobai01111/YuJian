-- 角色-部门关联表（用于自定义数据权限）
CREATE TABLE IF NOT EXISTS sys_role_depts (
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    dept_id BIGINT NOT NULL REFERENCES sys_depts(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, dept_id)
);

COMMENT ON TABLE sys_role_depts IS '角色-部门关联表，用于自定义数据权限';

-- 数据权限说明（存储在 sys_roles.data_scope 字段）:
-- 1 = 全部数据权限
-- 2 = 自定义数据权限（根据 sys_role_depts 表）
-- 3 = 本部门数据权限
-- 4 = 本部门及以下数据权限
-- 5 = 仅本人数据权限
