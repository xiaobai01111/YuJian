-- 校园墙数据库初始化（重置版）

-- =========================
-- 1) 系统管理表
-- =========================

CREATE TABLE sys_depts (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    dept_name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 0,
    data_scope SMALLINT NOT NULL DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_depts IS '部门表';
COMMENT ON COLUMN sys_depts.parent_id IS '父部门ID，0表示顶级部门';
COMMENT ON COLUMN sys_depts.status IS '状态：0-正常，1-停用';
COMMENT ON COLUMN sys_depts.data_scope IS '数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人';

CREATE INDEX idx_sys_depts_parent ON sys_depts(parent_id);
CREATE INDEX idx_sys_depts_status ON sys_depts(status);

CREATE TABLE sys_roles (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_key VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_roles IS '角色表';
COMMENT ON COLUMN sys_roles.role_key IS '角色标识，用于权限校验';
COMMENT ON COLUMN sys_roles.status IS '状态：0-正常，1-停用';

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
COMMENT ON COLUMN sys_menus.type IS '类型：0-目录，1-菜单，2-按钮';
COMMENT ON COLUMN sys_menus.perms IS '权限标识';
COMMENT ON COLUMN sys_menus.status IS '状态：0-正常，1-停用';

CREATE TABLE sys_role_menus (
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    menu_id BIGINT NOT NULL REFERENCES sys_menus(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_id)
);

COMMENT ON TABLE sys_role_menus IS '角色-菜单关联表';

CREATE TABLE sys_role_depts (
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    dept_id BIGINT NOT NULL REFERENCES sys_depts(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, dept_id)
);

COMMENT ON TABLE sys_role_depts IS '角色-部门关联表';

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

CREATE INDEX idx_api_perm_url ON sys_api_permissions(url);
CREATE INDEX idx_api_perm_status ON sys_api_permissions(status);

CREATE TABLE sys_oper_log (
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

CREATE INDEX idx_oper_log_operator ON sys_oper_log(operator_id);
CREATE INDEX idx_oper_log_target ON sys_oper_log(target_type, target_id);
CREATE INDEX idx_oper_log_action ON sys_oper_log(action);
CREATE INDEX idx_oper_log_created ON sys_oper_log(created_at);

CREATE TABLE sys_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    ipaddr VARCHAR(64),
    login_location VARCHAR(100),
    browser VARCHAR(100),
    os VARCHAR(100),
    status SMALLINT DEFAULT 0,
    msg VARCHAR(255),
    user_agent TEXT,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_login_log IS '登录日志表';
COMMENT ON COLUMN sys_login_log.status IS '状态：0-成功，1-失败';
COMMENT ON COLUMN sys_login_log.login_time IS '登录时间';

CREATE INDEX idx_login_log_username ON sys_login_log(username);
CREATE INDEX idx_login_log_status ON sys_login_log(status);
CREATE INDEX idx_login_log_time ON sys_login_log(login_time);

CREATE TABLE sys_blocklist (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_value VARCHAR(128) NOT NULL,
    reason VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 0,
    expire_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_blocklist IS '阻止名单';
COMMENT ON COLUMN sys_blocklist.target_type IS '类型：IP/USER/DEVICE';
COMMENT ON COLUMN sys_blocklist.target_value IS '目标值';
COMMENT ON COLUMN sys_blocklist.status IS '状态：0-启用，1-停用';
COMMENT ON COLUMN sys_blocklist.expire_at IS '过期时间';

CREATE UNIQUE INDEX uk_sys_blocklist_target ON sys_blocklist(target_type, target_value);
CREATE INDEX idx_sys_blocklist_status ON sys_blocklist(status);
CREATE INDEX idx_sys_blocklist_expire ON sys_blocklist(expire_at);

CREATE TABLE sys_notice (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    scope_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    scope_ids TEXT,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    published_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_notice IS '系统公告表';

CREATE INDEX idx_notice_status ON sys_notice(status);
CREATE INDEX idx_notice_scope_type ON sys_notice(scope_type);
CREATE INDEX idx_notice_is_pinned ON sys_notice(is_pinned);
CREATE INDEX idx_notice_published_at ON sys_notice(published_at);
CREATE INDEX idx_notice_start_at ON sys_notice(start_at);
CREATE INDEX idx_notice_end_at ON sys_notice(end_at);

CREATE TABLE sys_auth_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    trigger_type VARCHAR(50),
    verify_method VARCHAR(50),
    match_type VARCHAR(50),
    match_value VARCHAR(200),
    role_ids JSONB,
    dept_id BIGINT,
    priority INT DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_auth_rules IS '认证规则表';

-- =========================
-- 2) 用户相关表
-- =========================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    email VARCHAR(100),
    edu_email VARCHAR(100),
    verify_status SMALLINT DEFAULT 0,
    verify_method VARCHAR(20),
    student_id VARCHAR(50),
    student_id_hash VARCHAR(64) UNIQUE,
    status SMALLINT DEFAULT 0,
    credit_score INT DEFAULT 100,
    phone VARCHAR(20),
    remark VARCHAR(255),
    dept_id BIGINT REFERENCES sys_depts(id) ON DELETE SET NULL,
    user_type SMALLINT DEFAULT 0,
    sex SMALLINT DEFAULT 0,
    login_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_reason VARCHAR(500),
    deleted INTEGER DEFAULT 0
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.sex IS '性别：0-未知，1-男，2-女';
COMMENT ON COLUMN users.user_type IS '用户类型：0-普通用户，1-管理员';
COMMENT ON COLUMN users.verify_status IS '验证状态：0-未验证，1-准验证/新生，2-已验证';
COMMENT ON COLUMN users.verify_method IS '验证方式：EDU_EMAIL/ID_CARD_OCR/MANUAL';
COMMENT ON COLUMN users.status IS '状态：0-正常，1-封禁';

CREATE INDEX idx_users_dept_id ON users(dept_id);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted ON users(deleted);

CREATE TABLE sys_user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE sys_user_roles IS '用户-角色关联表';

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

CREATE INDEX idx_identity_user_id ON identity_verifications(user_id);
CREATE INDEX idx_identity_status ON identity_verifications(status);

-- =========================
-- 3) 内容相关表
-- =========================

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    board VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    content TEXT NOT NULL,
    is_anonymous BOOLEAN DEFAULT FALSE,
    category VARCHAR(50),
    price DECIMAL(10, 2),
    location VARCHAR(100),
    lost_time TIMESTAMP,
    status SMALLINT DEFAULT 0,
    show_on_home BOOLEAN NOT NULL DEFAULT TRUE,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    last_interaction_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE posts IS '帖子表';
COMMENT ON COLUMN posts.board IS '主板块标识';
COMMENT ON COLUMN posts.status IS '状态：0-正常，1-已解决，2-已删除，3-待审核，4-已下架';
COMMENT ON COLUMN posts.show_on_home IS '是否同步首页展示';

CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_posts_board ON posts(board);
CREATE INDEX idx_posts_status ON posts(status);

CREATE TABLE post_boards (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    board VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE post_boards IS '帖子-板块关联表';

CREATE INDEX idx_post_boards_post ON post_boards(post_id);
CREATE INDEX idx_post_boards_board ON post_boards(board);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT DEFAULT 0,
    content TEXT NOT NULL,
    anonymous_id VARCHAR(50),
    is_owner BOOLEAN DEFAULT FALSE,
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE comments IS '评论表';

CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_comments_user ON comments(user_id);

CREATE TABLE likes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE likes IS '点赞表';

CREATE UNIQUE INDEX idx_likes_user_post ON likes(user_id, post_id);

CREATE TABLE bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE bookmarks IS '收藏表';

CREATE UNIQUE INDEX idx_bookmarks_user_post ON bookmarks(user_id, post_id);

CREATE TABLE anonymous_mappings (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id_encrypted VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE anonymous_mappings IS '匿名映射表';

CREATE INDEX idx_anonymous_post ON anonymous_mappings(post_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50),
    title VARCHAR(200),
    content TEXT,
    target_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notifications IS '通知表';

CREATE INDEX idx_notifications_user ON notifications(user_id);

CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT,
    status SMALLINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE announcements IS '旧公告表（兼容）';

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    reason VARCHAR(500) NOT NULL,
    status SMALLINT DEFAULT 0,
    handler_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    result VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP
);

COMMENT ON TABLE reports IS '举报表';

CREATE INDEX idx_reports_post ON reports(post_id);
CREATE INDEX idx_reports_status ON reports(status);

CREATE TABLE sensitive_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    level SMALLINT DEFAULT 2,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sensitive_words IS '敏感词表';

-- =========================
-- 4) 文件与市集表
-- =========================

CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    target_id BIGINT,
    target_type VARCHAR(50),
    filename VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    size BIGINT,
    mime_type VARCHAR(100),
    status SMALLINT DEFAULT 0,
    audit_status SMALLINT DEFAULT 0,
    storage_class VARCHAR(50),
    last_accessed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE files IS '文件记录表';

CREATE INDEX idx_files_user ON files(user_id);
CREATE INDEX idx_files_target ON files(target_type, target_id);

CREATE TABLE market_orders (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    buyer_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    price DECIMAL(10, 2) NOT NULL,
    status SMALLINT DEFAULT 0,
    buyer_confirmed BOOLEAN DEFAULT FALSE,
    seller_confirmed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

COMMENT ON TABLE market_orders IS '市集订单表';

CREATE INDEX idx_market_orders_post ON market_orders(post_id);
CREATE INDEX idx_market_orders_seller ON market_orders(seller_id);
