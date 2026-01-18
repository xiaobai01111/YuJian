-- 审计日志表添加索引，优化查询性能
CREATE INDEX IF NOT EXISTS idx_oper_log_target ON sys_oper_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_oper_log_action ON sys_oper_log(action);
CREATE INDEX IF NOT EXISTS idx_oper_log_created_at ON sys_oper_log(created_at);
CREATE INDEX IF NOT EXISTS idx_oper_log_operator ON sys_oper_log(operator_id);
