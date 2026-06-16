-- Mock data for local/dev (PostgreSQL)
-- Safe to run multiple times; uses NOT EXISTS/ON CONFLICT guards.

BEGIN;

-- 1) Root dept
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT 0, '系统部门', 0, 'system', 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts WHERE dept_name = '系统部门'
);

-- 2) Top-level depts
WITH root AS (
    SELECT id FROM sys_depts WHERE dept_name = '系统部门' ORDER BY id LIMIT 1
),
top AS (
    SELECT * FROM (VALUES
        ('校务中心', 10, 1),
        ('学院体系', 20, 1),
        ('后勤中心', 30, 1),
        ('信息中心', 40, 2),
        ('图书体系', 50, 3)
    ) AS v(name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT COALESCE((SELECT id FROM root), 1),
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM top v
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 3) Second-level depts
WITH parents AS (
    SELECT id, dept_name FROM sys_depts
    WHERE dept_name IN ('校务中心', '学院体系', '后勤中心', '信息中心', '图书体系')
),
second AS (
    SELECT * FROM (VALUES
        ('校务中心', '教务处', 11, 1),
        ('校务中心', '学生处', 12, 1),
        ('校务中心', '宣传部', 13, 1),
        ('学院体系', '理学院', 21, 3),
        ('学院体系', '文学院', 22, 3),
        ('学院体系', '工学院', 23, 4),
        ('后勤中心', '保卫处', 31, 3),
        ('后勤中心', '宿管中心', 32, 3),
        ('后勤中心', '餐饮中心', 33, 3),
        ('信息中心', '网络运维', 41, 2),
        ('信息中心', '系统开发', 42, 2),
        ('图书体系', '图书馆', 51, 3),
        ('图书体系', '资源建设', 52, 3)
    ) AS v(parent_name, name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT p.id,
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM second v
JOIN parents p ON p.dept_name = v.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 4) Third-level depts
WITH parents AS (
    SELECT id, dept_name FROM sys_depts
    WHERE dept_name IN ('教务处', '学生处', '理学院', '文学院', '工学院', '保卫处', '宿管中心', '餐饮中心', '网络运维', '系统开发', '图书馆')
),
third AS (
    SELECT * FROM (VALUES
        ('教务处', '本科教学', 111, 3),
        ('教务处', '研究生教学', 112, 3),
        ('学生处', '资助管理', 121, 3),
        ('学生处', '心理健康', 122, 3),
        ('理学院', '数学系', 211, 4),
        ('理学院', '物理系', 212, 4),
        ('文学院', '中文系', 221, 4),
        ('文学院', '历史系', 222, 4),
        ('工学院', '计算机系', 231, 4),
        ('工学院', '机械系', 232, 4),
        ('保卫处', '校园巡逻', 311, 3),
        ('宿管中心', '男生宿管', 321, 3),
        ('宿管中心', '女生宿管', 322, 3),
        ('餐饮中心', '一食堂', 331, 3),
        ('餐饮中心', '二食堂', 332, 3),
        ('网络运维', '出口网关', 411, 2),
        ('网络运维', '内网维护', 412, 2),
        ('系统开发', '业务系统', 421, 2),
        ('系统开发', '移动端', 422, 2),
        ('图书馆', '借阅服务', 511, 3),
        ('图书馆', '自习管理', 512, 3)
    ) AS v(parent_name, name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT p.id,
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM third v
JOIN parents p ON p.dept_name = v.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 5) Roles
INSERT INTO sys_roles (role_name, role_key, status, sort_order, remark)
SELECT '系统管理员', 'admin', 0, 1, '系统内置'
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_key = 'admin');

INSERT INTO sys_roles (role_name, role_key, status, sort_order, remark)
SELECT '版主', 'moderator', 0, 2, '内容管理权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_key = 'moderator');

INSERT INTO sys_roles (role_name, role_key, status, sort_order, remark)
SELECT '普通用户', 'user', 0, 3, '基础权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_key = 'user');

INSERT INTO sys_roles (role_name, role_key, status, sort_order, remark)
SELECT '教师', 'teacher', 0, 4, '教学相关权限'
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_key = 'teacher');

-- 6) Role-Dept bindings
INSERT INTO sys_role_depts (role_id, dept_id)
SELECT r.id, d.id
FROM sys_roles r
CROSS JOIN sys_depts d
WHERE r.role_key = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_depts rd WHERE rd.role_id = r.id AND rd.dept_id = d.id
  );

INSERT INTO sys_role_depts (role_id, dept_id)
SELECT r.id, d.id
FROM sys_roles r
JOIN sys_depts d ON d.dept_name IN ('学生处', '教务处', '心理健康', '资助管理')
WHERE r.role_key = 'moderator'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_depts rd WHERE rd.role_id = r.id AND rd.dept_id = d.id
  );

INSERT INTO sys_role_depts (role_id, dept_id)
SELECT r.id, d.id
FROM sys_roles r
JOIN sys_depts d ON d.dept_name IN ('理学院', '文学院', '工学院', '图书馆')
WHERE r.role_key = 'teacher'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_depts rd WHERE rd.role_id = r.id AND rd.dept_id = d.id
  );

-- 7) Users (user0001..user0200)
WITH dept_pool AS (
    SELECT array_agg(id ORDER BY id) AS ids FROM sys_depts WHERE status = 0
),
series AS (
    SELECT generate_series(1, 200) AS n
)
INSERT INTO users (
    username, password, nickname, dept_id, user_type, verify_status, status,
    credit_score, phone, created_at, updated_at
)
SELECT
    'user' || lpad(n::text, 4, '0'),
    '$2y$10$tyTtSrcuzDqXLgTlfM0GTuh0TR7/.cFUceo6y720YixN4Anr0HaWy',
    '用户' || lpad(n::text, 4, '0'),
    (dept_pool.ids)[(n % array_length(dept_pool.ids, 1)) + 1],
    0,
    CASE WHEN n % 8 = 0 THEN 1 WHEN n % 5 = 0 THEN 2 ELSE 0 END,
    CASE WHEN n % 37 = 0 THEN 1 ELSE 0 END,
    60 + (n % 41),
    '1880000' || lpad(n::text, 4, '0'),
    NOW() - (n || ' hours')::interval,
    NOW() - (n || ' hours')::interval
FROM series, dept_pool
ON CONFLICT (username) DO NOTHING;

-- Moderators
WITH dept_pool AS (
    SELECT array_agg(id ORDER BY id) AS ids FROM sys_depts WHERE dept_name IN ('学生处', '教务处')
),
series AS (
    SELECT generate_series(1, 5) AS n
)
INSERT INTO users (
    username, password, nickname, dept_id, user_type, verify_status, status,
    credit_score, phone, created_at, updated_at
)
SELECT
    'mod' || lpad(n::text, 3, '0'),
    '$2y$10$tyTtSrcuzDqXLgTlfM0GTuh0TR7/.cFUceo6y720YixN4Anr0HaWy',
    '版主' || lpad(n::text, 3, '0'),
    (dept_pool.ids)[(n % array_length(dept_pool.ids, 1)) + 1],
    0,
    2,
    0,
    90,
    '1890000' || lpad(n::text, 4, '0'),
    NOW() - (n || ' days')::interval,
    NOW() - (n || ' days')::interval
FROM series, dept_pool
ON CONFLICT (username) DO NOTHING;

-- Teachers
WITH dept_pool AS (
    SELECT array_agg(id ORDER BY id) AS ids FROM sys_depts WHERE dept_name IN ('理学院', '文学院', '工学院')
),
series AS (
    SELECT generate_series(1, 8) AS n
)
INSERT INTO users (
    username, password, nickname, dept_id, user_type, verify_status, status,
    credit_score, phone, created_at, updated_at
)
SELECT
    'teacher' || lpad(n::text, 3, '0'),
    '$2y$10$tyTtSrcuzDqXLgTlfM0GTuh0TR7/.cFUceo6y720YixN4Anr0HaWy',
    '教师' || lpad(n::text, 3, '0'),
    (dept_pool.ids)[(n % array_length(dept_pool.ids, 1)) + 1],
    0,
    2,
    0,
    95,
    '1870000' || lpad(n::text, 4, '0'),
    NOW() - (n || ' days')::interval,
    NOW() - (n || ' days')::interval
FROM series, dept_pool
ON CONFLICT (username) DO NOTHING;

-- 8) User-Role bindings
INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_roles r ON r.role_key = 'user'
WHERE u.username LIKE 'user%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_roles r ON r.role_key = 'moderator'
WHERE u.username LIKE 'mod%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_roles r ON r.role_key = 'teacher'
WHERE u.username LIKE 'teacher%'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN sys_roles r ON r.role_key = 'admin'
WHERE u.user_type = 1
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- 9) Posts
WITH user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%' OR username LIKE 'mod%' OR username LIKE 'teacher%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
series AS (
    SELECT generate_series(1, 800) AS n
)
INSERT INTO posts (
    user_id, board, title, content, is_anonymous, category, price, location,
    lost_time, status, show_on_home, like_count, comment_count, view_count,
    last_interaction_at, created_at, updated_at
)
SELECT
    u.id,
    CASE (n % 5)
        WHEN 0 THEN 'confessions'
        WHEN 1 THEN 'treehole'
        WHEN 2 THEN 'help'
        WHEN 3 THEN 'market'
        ELSE 'lost-found'
    END,
    'Mock post ' || lpad(n::text, 4, '0'),
    'Mock content for post ' || lpad(n::text, 4, '0') || '.',
    (n % 7 = 0),
    CASE (n % 4)
        WHEN 0 THEN 'general'
        WHEN 1 THEN 'help'
        WHEN 2 THEN 'market'
        ELSE 'lost-found'
    END,
    CASE WHEN (n % 5) = 3 THEN (n % 200) + 10 ELSE NULL END,
    CASE WHEN (n % 5) = 4 THEN 'Campus Zone ' || (n % 12) ELSE NULL END,
    CASE WHEN (n % 5) = 4 THEN NOW() - ((n % 240) || ' hours')::interval ELSE NULL END,
    CASE WHEN n % 29 = 0 THEN 1 WHEN n % 40 = 0 THEN 3 ELSE 0 END,
    CASE WHEN n % 6 = 0 THEN FALSE ELSE TRUE END,
    0,
    0,
    0,
    NOW() - ((n % 240) || ' hours')::interval,
    NOW() - ((n % 240) || ' hours')::interval,
    NOW() - ((n % 240) || ' hours')::interval
FROM series
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.title = 'Mock post ' || lpad(n::text, 4, '0')
);

-- 10) Post boards
INSERT INTO post_boards (post_id, board)
SELECT p.id, p.board
FROM posts p
WHERE p.title LIKE 'Mock post %'
  AND NOT EXISTS (
      SELECT 1 FROM post_boards pb WHERE pb.post_id = p.id AND pb.board = p.board
  );

-- 11) Comments
WITH post_list AS (
    SELECT id, user_id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Mock post %'
),
post_count AS (
    SELECT count(*) AS cnt FROM post_list
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%' OR username LIKE 'mod%' OR username LIKE 'teacher%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
series AS (
    SELECT generate_series(1, 2000) AS n
)
INSERT INTO comments (
    post_id, user_id, parent_id, content, anonymous_id, is_owner, status, created_at
)
SELECT
    p.id,
    u.id,
    0,
    'Mock comment ' || lpad(n::text, 5, '0'),
    NULL,
    (p.user_id = u.id),
    0,
    NOW() - ((n % 240) || ' hours')::interval
FROM series
JOIN post_list p ON p.rn = (n % (SELECT cnt FROM post_count)) + 1
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM comments c WHERE c.content = 'Mock comment ' || lpad(n::text, 5, '0')
);

-- 12) Likes / Bookmarks
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Mock post %'
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%' OR username LIKE 'mod%' OR username LIKE 'teacher%'
)
INSERT INTO likes (user_id, post_id, created_at)
SELECT u.id, p.id, NOW() - ((p.id % 120) || ' hours')::interval
FROM user_list u
JOIN post_list p ON (p.rn % 37) = (u.rn % 37)
ON CONFLICT (user_id, post_id) DO NOTHING;

WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Mock post %'
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%' OR username LIKE 'mod%' OR username LIKE 'teacher%'
)
INSERT INTO bookmarks (user_id, post_id, created_at)
SELECT u.id, p.id, NOW() - ((p.id % 200) || ' hours')::interval
FROM user_list u
JOIN post_list p ON (p.rn % 43) = (u.rn % 43)
ON CONFLICT (user_id, post_id) DO NOTHING;

-- 13) Reports
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Mock post %'
),
post_count AS (
    SELECT count(*) AS cnt FROM post_list
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%' OR username LIKE 'mod%' OR username LIKE 'teacher%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
series AS (
    SELECT generate_series(1, 120) AS n
)
INSERT INTO reports (
    reporter_id, post_id, reason, status, handler_id, result, created_at, handled_at
)
SELECT
    u.id,
    p.id,
    'Mock report ' || lpad(n::text, 4, '0'),
    CASE WHEN n % 3 = 0 THEN 1 ELSE 0 END,
    CASE WHEN n % 3 = 0 THEN 1 ELSE NULL END,
    CASE WHEN n % 3 = 0 THEN 'auto-reviewed' ELSE NULL END,
    NOW() - ((n % 240) || ' hours')::interval,
    CASE WHEN n % 3 = 0 THEN NOW() - ((n % 120) || ' hours')::interval ELSE NULL END
FROM series
JOIN post_list p ON p.rn = (n % (SELECT cnt FROM post_count)) + 1
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM reports r WHERE r.reason = 'Mock report ' || lpad(n::text, 4, '0')
);

-- 14) Recalculate counters
UPDATE posts p
SET comment_count = COALESCE(c.cnt, 0)
FROM (
    SELECT post_id, COUNT(*) AS cnt
    FROM comments
    WHERE status = 0
    GROUP BY post_id
) c
WHERE p.id = c.post_id;

UPDATE posts p
SET like_count = COALESCE(l.cnt, 0)
FROM (
    SELECT post_id, COUNT(*) AS cnt
    FROM likes
    GROUP BY post_id
) l
WHERE p.id = l.post_id;

UPDATE posts
SET view_count = (id % 500) + 5
WHERE title LIKE 'Mock post %';

COMMIT;
