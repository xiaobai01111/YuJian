-- 资产管理菜单规划优化（共享后台）

-- 1) 修正按钮权限的归属（公共媒体库 ↔ 业务附件库）
UPDATE sys_menus
SET parent_id = 40
WHERE id IN (281, 284, 285, 287);

UPDATE sys_menus
SET parent_id = 41
WHERE id IN (280, 282, 283, 286, 288);

-- 2) 版主角色默认可使用公共媒体库与业务附件库（不包含系统资源库）
INSERT INTO sys_role_menus (role_id, menu_id) VALUES
    (2, 5),   -- 资产管理（父级）
    (2, 40),  -- 公共媒体库
    (2, 41),  -- 业务附件库
    (2, 280), -- 查询文件
    (2, 281), -- 查询图库
    (2, 282), -- 上传文件
    (2, 283), -- 删除文件
    (2, 284), -- 上传图库
    (2, 285), -- 删除图库
    (2, 286), -- 设置文件权限
    (2, 287)  -- 设置图库权限
ON CONFLICT (role_id, menu_id) DO NOTHING;
