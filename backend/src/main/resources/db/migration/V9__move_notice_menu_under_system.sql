-- =============================================
-- V9: 将公告管理菜单归入“系统管理”
-- 目标：公告管理显示在系统管理目录下
-- =============================================

DO $$
DECLARE
    system_menu_id BIGINT;
BEGIN
    SELECT id INTO system_menu_id
    FROM sys_menus
    WHERE type = 0 AND name = '系统管理'
    ORDER BY id
    LIMIT 1;

    IF system_menu_id IS NOT NULL THEN
        UPDATE sys_menus
        SET parent_id = system_menu_id,
            updated_at = NOW()
        WHERE type = 1 AND name = '公告管理';
    END IF;
END $$;
