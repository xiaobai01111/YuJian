-- 为菜单表添加状态字段
ALTER TABLE sys_menus ADD COLUMN status SMALLINT DEFAULT 0;

COMMENT ON COLUMN sys_menus.status IS '状态: 0-正常, 1-停用';

-- 将所有现有菜单设置为启用状态
UPDATE sys_menus SET status = 0 WHERE status IS NULL;
