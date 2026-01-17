-- 操作审计日志表
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(100),
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    action VARCHAR(50) NOT NULL,
    reason TEXT,
    before_value JSONB,
    after_value JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_oper_log IS '操作审计日志';
COMMENT ON COLUMN sys_oper_log.operator_id IS '操作者ID';
COMMENT ON COLUMN sys_oper_log.operator_name IS '操作者用户名';
COMMENT ON COLUMN sys_oper_log.target_type IS '操作对象类型：user/post/order等';
COMMENT ON COLUMN sys_oper_log.target_id IS '操作对象ID';
COMMENT ON COLUMN sys_oper_log.action IS '操作类型：create/update/delete/ban/unban/role_assign等';
COMMENT ON COLUMN sys_oper_log.reason IS '操作原因';
COMMENT ON COLUMN sys_oper_log.before_value IS '变更前值（JSON）';
COMMENT ON COLUMN sys_oper_log.after_value IS '变更后值（JSON）';
COMMENT ON COLUMN sys_oper_log.ip_address IS '操作IP';

-- 索引
CREATE INDEX IF NOT EXISTS idx_oper_log_operator ON sys_oper_log(operator_id);
CREATE INDEX IF NOT EXISTS idx_oper_log_target ON sys_oper_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_oper_log_action ON sys_oper_log(action);
CREATE INDEX IF NOT EXISTS idx_oper_log_created ON sys_oper_log(created_at);
