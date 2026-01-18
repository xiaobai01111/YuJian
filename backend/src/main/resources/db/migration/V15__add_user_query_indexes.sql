-- 用户列表查询优化索引
CREATE INDEX IF NOT EXISTS idx_users_verify_status ON users(verify_status);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_login_date ON users(login_date);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
