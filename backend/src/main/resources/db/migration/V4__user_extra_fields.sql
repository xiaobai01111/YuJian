-- 添加用户扩展字段
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type SMALLINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sex SMALLINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS login_date TIMESTAMP;

-- 创建部门表
CREATE TABLE IF NOT EXISTS sys_depts (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    dept_name VARCHAR(50) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加部门外键
ALTER TABLE users ADD CONSTRAINT fk_users_dept FOREIGN KEY (dept_id) REFERENCES sys_depts(id) ON DELETE SET NULL;

-- 插入默认部门
INSERT INTO sys_depts (id, parent_id, dept_name, sort_order, leader, status) VALUES
(1, 0, '总部', 1, '管理员', 0),
(2, 1, '技术部', 1, NULL, 0),
(3, 1, '运营部', 2, NULL, 0);

-- 更新 admin 用户的部门
UPDATE users SET dept_id = 1, user_type = 0, sex = 0 WHERE username = 'admin';

COMMENT ON COLUMN users.phone IS '手机号码';
COMMENT ON COLUMN users.dept_id IS '部门ID';
COMMENT ON COLUMN users.user_type IS '用户类型：0=普通用户, 1=管理员';
COMMENT ON COLUMN users.sex IS '性别：0=未知, 1=男, 2=女';
COMMENT ON COLUMN users.login_date IS '最后登录时间';
