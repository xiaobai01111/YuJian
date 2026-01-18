-- 添加 scope_type 检查约束
ALTER TABLE sys_notice 
ADD CONSTRAINT chk_scope_type 
CHECK (scope_type IS NULL OR scope_type IN ('ALL', 'DEPT', 'USERS'));

-- 为 scope_ids jsonb 查询添加 GIN 索引（提升 @> 查询性能）
CREATE INDEX IF NOT EXISTS idx_notice_scope_ids ON sys_notice USING GIN ((scope_ids::jsonb));

-- 为常用查询字段添加索引
CREATE INDEX IF NOT EXISTS idx_notice_status_published ON sys_notice (status, published_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_notice_pinned ON sys_notice (is_pinned DESC, published_at DESC NULLS LAST);
