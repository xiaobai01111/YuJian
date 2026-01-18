-- =============================================
-- V8: 统一通知公告菜单与权限标识
-- 目标：
-- 1) 菜单路径统一为 /console/notice
-- 2) 组件统一为 views/console/notice/index.vue
-- 3) 权限标识统一为 system:notice:*
-- =============================================

DO $$
DECLARE
    canonical_id BIGINT;
BEGIN
    -- 选择一个公告菜单作为规范项，优先“公告管理”
    SELECT id INTO canonical_id
    FROM sys_menus
    WHERE type = 1 AND name IN ('公告管理', '通知公告')
    ORDER BY CASE WHEN name = '公告管理' THEN 1 ELSE 0 END DESC, id DESC
    LIMIT 1;

    IF canonical_id IS NOT NULL THEN
        -- 规范化主菜单
        UPDATE sys_menus
        SET name = '公告管理',
            path = '/console/notice',
            component = 'views/console/notice/index.vue',
            icon = 'bell',
            perms = 'system:notice:list',
            status = 0,
            visible = TRUE,
            updated_at = NOW()
        WHERE id = canonical_id;

        -- 归并旧菜单的子权限到规范菜单
        UPDATE sys_menus
        SET parent_id = canonical_id,
            updated_at = NOW()
        WHERE parent_id IN (
            SELECT id
            FROM sys_menus
            WHERE type = 1 AND name IN ('公告管理', '通知公告') AND id <> canonical_id
        );

        -- 停用旧菜单，避免重复展示
        UPDATE sys_menus
        SET status = 1,
            visible = FALSE,
            updated_at = NOW()
        WHERE type = 1 AND name IN ('公告管理', '通知公告') AND id <> canonical_id;
    END IF;

    -- 权限标识统一（system:announcement:* -> system:notice:*）
    UPDATE sys_menus
    SET perms = REPLACE(perms, 'system:announcement:', 'system:notice:'),
        updated_at = NOW()
    WHERE perms LIKE 'system:announcement:%';

    -- 同步 API 权限配置（如存在）
    UPDATE sys_api_permissions
    SET permission = REPLACE(permission, 'system:announcement:', 'system:notice:'),
        updated_at = NOW()
    WHERE permission LIKE 'system:announcement:%';
END $$;
