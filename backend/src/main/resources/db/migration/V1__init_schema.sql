-- =============================================
-- 校园墙系统 - 数据库初始化脚本
-- V1: 表结构创建
-- =============================================

-- =============================================
-- 1. 系统管理模块 (System)
-- =============================================

-- 1.1 部门表
CREATE TABLE sys_depts (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    dept_name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_depts IS '部门表';
COMMENT ON COLUMN sys_depts.parent_id IS '父部门ID，0表示顶级部门';
COMMENT ON COLUMN sys_depts.status IS '状态: 0-正常, 1-停用';

-- 1.2 角色表
CREATE TABLE sys_roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_key VARCHAR(50) NOT NULL UNIQUE,
    data_scope SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_roles IS '角色表';
COMMENT ON COLUMN sys_roles.role_key IS '权限字符，用于权限校验';
COMMENT ON COLUMN sys_roles.data_scope IS '数据权限范围: 1-全部, 2-自定义, 3-本部门, 4-本部门及以下, 5-仅本人';
COMMENT ON COLUMN sys_roles.status IS '状态: 0-正常, 1-停用';

-- 1.3 菜单权限表
CREATE TABLE sys_menus (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(255),
    perms VARCHAR(100),
    icon VARCHAR(50),
    type SMALLINT DEFAULT 1,
    visible BOOLEAN DEFAULT TRUE,
    status SMALLINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_menus IS '菜单权限表';
COMMENT ON COLUMN sys_menus.parent_id IS '父菜单ID，0表示顶级菜单';
COMMENT ON COLUMN sys_menus.type IS '类型: 0-目录, 1-菜单, 2-按钮/权限';
COMMENT ON COLUMN sys_menus.perms IS '权限标识，如 system:user:add';
COMMENT ON COLUMN sys_menus.status IS '状态: 0-正常, 1-停用';

-- 1.4 角色-菜单关联表
CREATE TABLE sys_role_menus (
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    menu_id BIGINT NOT NULL REFERENCES sys_menus(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menus IS '角色-菜单关联表';

-- 1.5 角色-部门关联表（自定义数据权限）
CREATE TABLE sys_role_depts (
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    dept_id BIGINT NOT NULL REFERENCES sys_depts(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, dept_id)
);

COMMENT ON TABLE sys_role_depts IS '角色-部门关联表，用于自定义数据权限';

-- 1.6 API权限配置表
CREATE TABLE sys_api_permissions (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL DEFAULT '*',
    permission VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_api_permissions IS 'API权限配置表';
COMMENT ON COLUMN sys_api_permissions.url IS 'API路径（支持Ant风格）';
COMMENT ON COLUMN sys_api_permissions.http_method IS 'HTTP方法: GET/POST/PUT/DELETE/*';
COMMENT ON COLUMN sys_api_permissions.permission IS '权限标识';

CREATE INDEX idx_api_perm_url ON sys_api_permissions(url);
CREATE INDEX idx_api_perm_status ON sys_api_permissions(status);

-- =============================================
-- 2. 用户模块 (User)
-- =============================================

-- 2.1 用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    edu_email VARCHAR(100),
    sex SMALLINT DEFAULT 0,
    dept_id BIGINT REFERENCES sys_depts(id) ON DELETE SET NULL,
    user_type SMALLINT DEFAULT 0,
    verify_status SMALLINT DEFAULT 0,
    verify_method VARCHAR(20),
    student_id VARCHAR(50),
    student_id_hash VARCHAR(64) UNIQUE,
    status SMALLINT DEFAULT 0,
    credit_score INT DEFAULT 100,
    login_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.sex IS '性别: 0-未知, 1-男, 2-女';
COMMENT ON COLUMN users.user_type IS '用户类型: 0-普通用户, 1-管理员';
COMMENT ON COLUMN users.verify_status IS '验证状态: 0-未验证, 1-准验证/新生, 2-已验证';
COMMENT ON COLUMN users.verify_method IS '验证方式: EDU_EMAIL/ID_CARD_OCR/MANUAL';
COMMENT ON COLUMN users.status IS '状态: 0-正常, 1-封禁';
COMMENT ON COLUMN users.credit_score IS '信用分，默认100';

CREATE INDEX idx_users_dept_id ON users(dept_id);
CREATE INDEX idx_users_status ON users(status);

-- 2.2 用户-角色关联表
CREATE TABLE sys_user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_roles IS '用户-角色关联表';

-- 2.3 身份审核表
CREATE TABLE identity_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    status SMALLINT DEFAULT 0,
    reviewer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);

COMMENT ON TABLE identity_verifications IS '身份审核表';
COMMENT ON COLUMN identity_verifications.status IS '状态: 0-待审核, 1-通过, 2-拒绝';

CREATE INDEX idx_identity_verifications_user_id ON identity_verifications(user_id);
CREATE INDEX idx_identity_verifications_status ON identity_verifications(status);

-- =============================================
-- 3. 内容模块 (Content)
-- =============================================

-- 3.1 帖子表
CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    board VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    content TEXT NOT NULL,
    is_anonymous BOOLEAN DEFAULT FALSE,
    category VARCHAR(50),
    price DECIMAL(10,2),
    location VARCHAR(100),
    lost_time TIMESTAMP,
    status SMALLINT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    last_interaction_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE posts IS '帖子表';
COMMENT ON COLUMN posts.board IS '板块: confession/treehole/help/market/lost/freshman';
COMMENT ON COLUMN posts.status IS '状态: 0-正常, 1-已解决, 2-已删除, 3-待审核, 4-已下架';
COMMENT ON COLUMN posts.location IS '地点，用于失物招领';
COMMENT ON COLUMN posts.lost_time IS '丢失/拾取时间';

-- 帖子全文搜索
ALTER TABLE posts ADD COLUMN search_vector tsvector;
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);

CREATE OR REPLACE FUNCTION posts_search_trigger() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.content, '')), 'B');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER posts_search_update
    BEFORE INSERT OR UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION posts_search_trigger();

CREATE INDEX idx_posts_board ON posts(board);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_status ON posts(status);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

-- 3.2 评论表
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    anonymous_id VARCHAR(20),
    is_owner BOOLEAN DEFAULT FALSE,
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE comments IS '评论表';
COMMENT ON COLUMN comments.anonymous_id IS '匿名标识，用于树洞帖子';
COMMENT ON COLUMN comments.is_owner IS '是否为帖子作者';
COMMENT ON COLUMN comments.status IS '状态: 0-正常, 1-已删除';

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);

-- 3.3 点赞表
CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id)
);

COMMENT ON TABLE likes IS '点赞表';

CREATE INDEX idx_likes_post_id ON likes(post_id);

-- 3.4 收藏表
CREATE TABLE bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id)
);

COMMENT ON TABLE bookmarks IS '收藏表';

CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_post_id ON bookmarks(post_id);

-- 3.5 举报表
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status SMALLINT DEFAULT 0,
    handler_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    result VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP
);

COMMENT ON TABLE reports IS '举报表';
COMMENT ON COLUMN reports.status IS '状态: 0-待处理, 1-已处理';

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_post_id ON reports(post_id);

-- 3.6 匿名映射表
CREATE TABLE anonymous_mappings (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id_encrypted VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE anonymous_mappings IS '匿名映射表，用于树洞帖子的真实用户关联，仅司法取证使用';
COMMENT ON COLUMN anonymous_mappings.user_id_encrypted IS '加密后的用户ID';

CREATE INDEX idx_anonymous_mappings_post_id ON anonymous_mappings(post_id);

-- =============================================
-- 4. 通知模块 (Notification)
-- =============================================

-- 4.1 通知表
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    target_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notifications IS '通知表';
COMMENT ON COLUMN notifications.type IS '类型: like/comment/system/report';

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- 4.2 公告表
CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE announcements IS '公告表';
COMMENT ON COLUMN announcements.status IS '状态: 0-正常, 1-已下架';

CREATE INDEX idx_announcements_status ON announcements(status);

-- =============================================
-- 5. 文件模块 (File)
-- =============================================

CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id BIGINT,
    target_type VARCHAR(20),
    filename VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    status SMALLINT DEFAULT 0,
    audit_status SMALLINT DEFAULT 0,
    storage_class VARCHAR(20) DEFAULT 'STANDARD',
    last_accessed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE files IS '文件表';
COMMENT ON COLUMN files.target_type IS '关联类型: POST/COMMENT/AVATAR/ID_CARD';
COMMENT ON COLUMN files.status IS '状态: 0-正常, 1-待清理, 2-已删除';
COMMENT ON COLUMN files.audit_status IS '审核状态: 0-待审核, 1-通过, 2-违规';
COMMENT ON COLUMN files.storage_class IS '存储类型: STANDARD/IA/ARCHIVE';

CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_target ON files(target_id, target_type);
CREATE INDEX idx_files_status ON files(status);

-- =============================================
-- 6. 市集模块 (Market)
-- =============================================

CREATE TABLE market_orders (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    price DECIMAL(10,2) NOT NULL,
    status SMALLINT DEFAULT 0,
    buyer_confirmed BOOLEAN DEFAULT FALSE,
    seller_confirmed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

COMMENT ON TABLE market_orders IS '市集订单表';
COMMENT ON COLUMN market_orders.status IS '状态: 0-待确认, 1-已完成, 2-已取消, 3-纠纷中';

CREATE INDEX idx_market_orders_post_id ON market_orders(post_id);
CREATE INDEX idx_market_orders_seller_id ON market_orders(seller_id);
CREATE INDEX idx_market_orders_buyer_id ON market_orders(buyer_id);
CREATE INDEX idx_market_orders_status ON market_orders(status);

-- =============================================
-- 7. 配置模块 (Config)
-- =============================================

CREATE TABLE sensitive_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    level SMALLINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sensitive_words IS '敏感词表';
COMMENT ON COLUMN sensitive_words.level IS '级别: 1-警告, 2-拦截';

CREATE INDEX idx_sensitive_words_level ON sensitive_words(level);
