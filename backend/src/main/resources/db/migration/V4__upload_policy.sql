-- 上传策略与资产分类

ALTER TABLE files ADD COLUMN IF NOT EXISTS asset_type VARCHAR(50);

UPDATE files
SET asset_type = CASE
    WHEN target_type IN ('file', 'gallery', 'resource') THEN target_type
    WHEN mime_type LIKE 'image/%' THEN 'gallery'
    ELSE 'file'
END
WHERE asset_type IS NULL;

ALTER TABLE files ALTER COLUMN asset_type SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_files_asset_type ON files(asset_type);

CREATE TABLE IF NOT EXISTS sys_upload_policies (
    id BIGSERIAL PRIMARY KEY,
    scene_code VARCHAR(64) NOT NULL UNIQUE,
    scene_name VARCHAR(100) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    visibility VARCHAR(16),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_upload_policies IS '上传策略配置表';
COMMENT ON COLUMN sys_upload_policies.scene_code IS '上传场景编码';
COMMENT ON COLUMN sys_upload_policies.scene_name IS '上传场景名称';
COMMENT ON COLUMN sys_upload_policies.asset_type IS '归档资产类型(file/gallery/resource)';
COMMENT ON COLUMN sys_upload_policies.visibility IS '默认可见性';

INSERT INTO sys_upload_policies (scene_code, scene_name, asset_type, visibility) VALUES
    ('post', '帖子图片', 'gallery', NULL),
    ('comment', '评论图片', 'gallery', NULL),
    ('avatar', '头像', 'gallery', NULL),
    ('id_card', '身份材料', 'file', 'PRIVATE'),
    ('public', '公共上传', 'gallery', NULL),
    ('package', '安装包', 'resource', 'PUBLIC'),
    ('file', '业务附件库', 'file', NULL),
    ('gallery', '公共媒体库', 'gallery', NULL),
    ('resource', '系统资源库', 'resource', NULL),
    ('campus.hero', '校园展示位', 'resource', NULL),
    ('campus.school', '校园信息', 'resource', NULL)
ON CONFLICT (scene_code) DO NOTHING;

-- 上传策略菜单与权限
INSERT INTO sys_menus (id, parent_id, name, path, component, type, icon, sort_order, visible, status) VALUES
    (900, 5, '上传策略', '/console/asset/upload-policy', 'views/console/asset/upload-policy/index.vue', 1, 'setting', 4, TRUE, 0);

INSERT INTO sys_menus (id, parent_id, name, perms, type, sort_order, visible, status) VALUES
    (901, 900, '查询上传策略', 'system:upload-policy:list', 2, 1, TRUE, 0),
    (902, 900, '编辑上传策略', 'system:upload-policy:edit', 2, 2, TRUE, 0);

INSERT INTO sys_role_menus (role_id, menu_id) VALUES
    (1, 900),
    (1, 901),
    (1, 902)
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_api_permissions (url, http_method, permission, description, status) VALUES
    ('/api/v1/console/upload-policies', 'GET', 'system:upload-policy:list', 'console.upload.policy.list', TRUE),
    ('/api/v1/console/upload-policies/*', 'PUT', 'system:upload-policy:edit', 'console.upload.policy.update', TRUE)
ON CONFLICT (http_method, url) DO NOTHING;
