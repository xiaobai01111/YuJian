-- =============================================
-- V10: 规范 sys_notice.scope_ids 为 jsonb 并建立索引
-- 说明：安全转换，避免无效 JSON 导致迁移失败
-- =============================================

-- 安全转换函数：无效 JSON 返回 NULL
CREATE OR REPLACE FUNCTION safe_jsonb(input text) RETURNS jsonb AS $$
DECLARE
    result jsonb;
BEGIN
    IF input IS NULL OR trim(input) = '' THEN
        RETURN NULL;
    END IF;
    BEGIN
        result := input::jsonb;
        RETURN result;
    EXCEPTION WHEN others THEN
        RETURN NULL;
    END;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE sys_notice
    ALTER COLUMN scope_ids TYPE jsonb
    USING safe_jsonb(scope_ids);

DROP FUNCTION safe_jsonb(text);

CREATE INDEX IF NOT EXISTS idx_notice_scope_ids ON sys_notice USING GIN (scope_ids);
