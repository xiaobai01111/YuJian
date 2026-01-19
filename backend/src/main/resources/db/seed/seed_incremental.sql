-- Incremental seed data for local/dev usage (PostgreSQL)
-- Safe to run multiple times; uses deterministic keys and NOT EXISTS checks.

BEGIN;

ALTER TABLE reports ADD COLUMN IF NOT EXISTS deleted SMALLINT DEFAULT 0;

-- 0) Ensure roles exist
INSERT INTO sys_roles (role_name, role_key, status, sort_order, remark)
VALUES
    ('系统管理员', 'admin', 0, 1, '系统角色'),
    ('版主', 'moderator', 0, 2, '内容管理权限'),
    ('普通用户', 'user', 0, 3, '基础用户权限')
ON CONFLICT (role_key) DO NOTHING;

-- 0.1) Ensure recycle menus and button permissions exist
UPDATE sys_menus
SET parent_id = (SELECT id FROM sys_menus WHERE name = '系统工具' ORDER BY id LIMIT 1),
    icon = 'recycle'
WHERE path = '/console/recycle';

UPDATE sys_menus
SET icon = 'recycle'
WHERE path IN ('/console/recycle/post', '/console/recycle/comment', '/console/recycle/report');

WITH tool_parent AS (
    SELECT id FROM sys_menus WHERE name = '系统工具' ORDER BY id LIMIT 1
)
INSERT INTO sys_menus (parent_id, name, path, component, type, icon, sort_order, visible, status)
SELECT tool_parent.id, '回收站', '/console/recycle', 'Layout', 0, 'recycle', 3, TRUE, 0
FROM tool_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus WHERE path = '/console/recycle'
);

WITH recycle_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle' ORDER BY id LIMIT 1
)
INSERT INTO sys_menus (parent_id, name, path, component, type, icon, sort_order, visible, status)
SELECT recycle_parent.id, '帖子回收站', '/console/recycle/post', 'views/console/recycle/post.vue', 1, 'recycle', 1, TRUE, 0
FROM recycle_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus WHERE path = '/console/recycle/post'
);

WITH recycle_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle' ORDER BY id LIMIT 1
)
INSERT INTO sys_menus (parent_id, name, path, component, type, icon, sort_order, visible, status)
SELECT recycle_parent.id, '评论回收站', '/console/recycle/comment', 'views/console/recycle/comment.vue', 1, 'recycle', 2, TRUE, 0
FROM recycle_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus WHERE path = '/console/recycle/comment'
);

WITH recycle_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle' ORDER BY id LIMIT 1
)
INSERT INTO sys_menus (parent_id, name, path, component, type, icon, sort_order, visible, status)
SELECT recycle_parent.id, '举报回收站', '/console/recycle/report', 'views/console/recycle/report.vue', 1, 'recycle', 3, TRUE, 0
FROM recycle_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus WHERE path = '/console/recycle/report'
);

WITH comment_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/comment' ORDER BY id LIMIT 1
),
perm_seed AS (
    SELECT * FROM (VALUES
        ('编辑评论', 'content:comment:edit', 3),
        ('批量删除评论', 'content:comment:batch-delete', 4)
    ) AS v(name, perms, sort_order)
)
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order, visible, status)
SELECT comment_parent.id, v.name, v.perms, 2, v.sort_order, TRUE, 0
FROM perm_seed v, comment_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus m WHERE m.perms = v.perms
);

WITH report_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/report' ORDER BY id LIMIT 1
),
perm_seed AS (
    SELECT * FROM (VALUES
        ('批量处理举报', 'content:report:batch-handle', 3),
        ('删除举报', 'content:report:delete', 4)
    ) AS v(name, perms, sort_order)
)
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order, visible, status)
SELECT report_parent.id, v.name, v.perms, 2, v.sort_order, TRUE, 0
FROM perm_seed v, report_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus m WHERE m.perms = v.perms
);

WITH recycle_post_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle/post' ORDER BY id LIMIT 1
),
perm_seed AS (
    SELECT * FROM (VALUES
        ('查看帖子回收站', 'content:recycle:post:list', 1),
        ('恢复帖子', 'content:recycle:post:restore', 2),
        ('彻底删除帖子', 'content:recycle:post:purge', 3)
    ) AS v(name, perms, sort_order)
)
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order, visible, status)
SELECT recycle_post_parent.id, v.name, v.perms, 2, v.sort_order, TRUE, 0
FROM perm_seed v, recycle_post_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus m WHERE m.perms = v.perms
);

WITH recycle_comment_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle/comment' ORDER BY id LIMIT 1
),
perm_seed AS (
    SELECT * FROM (VALUES
        ('查看评论回收站', 'content:recycle:comment:list', 1),
        ('恢复评论', 'content:recycle:comment:restore', 2),
        ('彻底删除评论', 'content:recycle:comment:purge', 3)
    ) AS v(name, perms, sort_order)
)
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order, visible, status)
SELECT recycle_comment_parent.id, v.name, v.perms, 2, v.sort_order, TRUE, 0
FROM perm_seed v, recycle_comment_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus m WHERE m.perms = v.perms
);

WITH recycle_report_parent AS (
    SELECT id FROM sys_menus WHERE path = '/console/recycle/report' ORDER BY id LIMIT 1
),
perm_seed AS (
    SELECT * FROM (VALUES
        ('查看举报回收站', 'content:recycle:report:list', 1),
        ('恢复举报', 'content:recycle:report:restore', 2),
        ('彻底删除举报', 'content:recycle:report:purge', 3)
    ) AS v(name, perms, sort_order)
)
INSERT INTO sys_menus (parent_id, name, perms, type, sort_order, visible, status)
SELECT recycle_report_parent.id, v.name, v.perms, 2, v.sort_order, TRUE, 0
FROM perm_seed v, recycle_report_parent
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menus m WHERE m.perms = v.perms
);

INSERT INTO sys_role_menus (role_id, menu_id)
SELECT r.id, m.id
FROM sys_roles r
JOIN sys_menus m
  ON m.path IN (
      '/console/recycle',
      '/console/recycle/post',
      '/console/recycle/comment',
      '/console/recycle/report'
  )
  OR m.perms IN (
      'content:comment:edit',
      'content:comment:batch-delete',
      'content:report:batch-handle',
      'content:report:delete',
      'content:recycle:post:list',
      'content:recycle:post:restore',
      'content:recycle:post:purge',
      'content:recycle:comment:list',
      'content:recycle:comment:restore',
      'content:recycle:comment:purge',
      'content:recycle:report:list',
      'content:recycle:report:restore',
      'content:recycle:report:purge'
  )
WHERE r.role_key = 'moderator'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 1) Ensure department hierarchy (multi-level)
-- 1.1 Level-1 units under root
WITH root_dept AS (
    SELECT id FROM sys_depts WHERE dept_name = '系统部门' ORDER BY id LIMIT 1
),
level1 AS (
    SELECT * FROM (VALUES
        ('Campus-Admin', 10, 1),
        ('Campus-IT', 20, 3),
        ('Campus-Ops', 30, 3),
        ('Student-Affairs', 40, 3),
        ('Library', 50, 3),
        ('Graduate-School', 60, 4),
        ('College-Science', 70, 4),
        ('College-Arts', 80, 4),
        ('College-Biz', 90, 4),
        ('College-Engineering', 100, 4),
        ('Dept-A', 110, 3),
        ('Dept-B', 120, 3),
        ('Dept-C', 130, 4),
        ('Dept-D', 140, 5)
    ) AS v(name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT COALESCE((SELECT id FROM root_dept), 1),
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM level1 v
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 1.2 Level-2 units under level-1
WITH parent_map AS (
    SELECT id, dept_name
    FROM sys_depts
    WHERE dept_name IN (
        'Campus-Admin','Campus-IT','Campus-Ops','Student-Affairs','Library','Graduate-School',
        'College-Science','College-Arts','College-Biz','College-Engineering'
    )
),
level2 AS (
    SELECT * FROM (VALUES
        ('Campus-Admin', 'Office-HR', 10, 3),
        ('Campus-Admin', 'Office-Finance', 20, 3),
        ('Campus-Admin', 'Office-Security', 30, 3),
        ('Campus-Admin', 'Office-Compliance', 40, 2),
        ('Campus-IT', 'Office-DevOps', 10, 3),
        ('Campus-IT', 'Office-Support', 20, 3),
        ('Campus-IT', 'Office-Network', 30, 3),
        ('Campus-IT', 'Office-Data', 40, 2),
        ('Campus-Ops', 'Office-Facilities', 10, 3),
        ('Campus-Ops', 'Office-Logistics', 20, 3),
        ('Campus-Ops', 'Office-Procurement', 30, 3),
        ('Student-Affairs', 'Office-Student', 10, 3),
        ('Student-Affairs', 'Office-Career', 20, 3),
        ('Student-Affairs', 'Office-Dorm', 30, 3),
        ('Student-Affairs', 'Office-Activities', 40, 3),
        ('Library', 'Library-Services', 10, 3),
        ('Library', 'Library-Digital', 20, 3),
        ('Graduate-School', 'Graduate-Admissions', 10, 3),
        ('Graduate-School', 'Graduate-Research', 20, 4),
        ('College-Science', 'Dept-CS', 10, 4),
        ('College-Science', 'Dept-Math', 20, 3),
        ('College-Science', 'Dept-Physics', 30, 3),
        ('College-Science', 'Dept-Chem', 40, 3),
        ('College-Arts', 'Dept-Lang', 10, 3),
        ('College-Arts', 'Dept-History', 20, 3),
        ('College-Arts', 'Dept-Design', 30, 3),
        ('College-Biz', 'Dept-Accounting', 10, 3),
        ('College-Biz', 'Dept-Marketing', 20, 3),
        ('College-Biz', 'Dept-Finance', 30, 3),
        ('College-Biz', 'Dept-Econ', 40, 3),
        ('College-Engineering', 'Dept-EEE', 10, 4),
        ('College-Engineering', 'Dept-Civil', 20, 3),
        ('College-Engineering', 'Dept-Mechanical', 30, 3),
        ('College-Engineering', 'Dept-Env', 40, 3)
    ) AS v(parent_name, name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT p.id,
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM level2 v
JOIN parent_map p ON p.dept_name = v.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 1.3 Level-3 units under level-2
WITH parent_map AS (
    SELECT id, dept_name
    FROM sys_depts
    WHERE dept_name IN (
        'Office-HR','Office-Finance','Office-Security','Office-Compliance',
        'Office-DevOps','Office-Support','Office-Network','Office-Data',
        'Office-Facilities','Office-Logistics','Office-Procurement',
        'Office-Student','Office-Career','Office-Dorm','Office-Activities',
        'Library-Services','Library-Digital','Graduate-Admissions','Graduate-Research',
        'Dept-CS','Dept-Math','Dept-Physics','Dept-Chem','Dept-Lang','Dept-History','Dept-Design',
        'Dept-Accounting','Dept-Marketing','Dept-Finance','Dept-Econ',
        'Dept-EEE','Dept-Civil','Dept-Mechanical','Dept-Env'
    )
),
level3 AS (
    SELECT * FROM (VALUES
        ('Office-HR', 'Team-HR-Recruiting', 10, 5),
        ('Office-HR', 'Team-HR-Payroll', 20, 5),
        ('Office-Finance', 'Team-Finance-AP', 10, 5),
        ('Office-Finance', 'Team-Finance-AR', 20, 5),
        ('Office-Security', 'Team-Security-Patrol', 10, 5),
        ('Office-Security', 'Team-Security-Audit', 20, 5),
        ('Office-Compliance', 'Team-Compliance-Policy', 10, 5),
        ('Office-DevOps', 'Team-DevOps-Platform', 10, 5),
        ('Office-DevOps', 'Team-DevOps-Release', 20, 5),
        ('Office-Support', 'Team-Support-Helpdesk', 10, 5),
        ('Office-Network', 'Team-Network-Infra', 10, 5),
        ('Office-Data', 'Team-Data-Warehouse', 10, 5),
        ('Office-Facilities', 'Team-Facilities-Maintenance', 10, 5),
        ('Office-Logistics', 'Team-Logistics-Transport', 10, 5),
        ('Office-Procurement', 'Team-Procurement-Vendor', 10, 5),
        ('Office-Student', 'Team-Student-Services', 10, 5),
        ('Office-Career', 'Team-Career-Placement', 10, 5),
        ('Office-Dorm', 'Team-Dorm-Resident', 10, 5),
        ('Office-Activities', 'Team-Activities-Events', 10, 5),
        ('Library-Services', 'Team-Library-Frontdesk', 10, 5),
        ('Library-Digital', 'Team-Library-Digitalization', 10, 5),
        ('Graduate-Admissions', 'Team-Grad-Admissions', 10, 5),
        ('Graduate-Research', 'Team-Grad-Research-Grant', 10, 5),
        ('Dept-CS', 'Class-CS-2023-A', 10, 5),
        ('Dept-CS', 'Class-CS-2023-B', 20, 5),
        ('Dept-CS', 'Class-CS-2024-A', 30, 5),
        ('Dept-Math', 'Class-Math-2023-A', 10, 5),
        ('Dept-Math', 'Class-Math-2024-A', 20, 5),
        ('Dept-Physics', 'Class-Physics-2023-A', 10, 5),
        ('Dept-Physics', 'Class-Physics-2024-A', 20, 5),
        ('Dept-Chem', 'Class-Chem-2023-A', 10, 5),
        ('Dept-Chem', 'Class-Chem-2024-A', 20, 5),
        ('Dept-Lang', 'Class-Lang-2023-A', 10, 5),
        ('Dept-Lang', 'Class-Lang-2024-A', 20, 5),
        ('Dept-History', 'Class-History-2023-A', 10, 5),
        ('Dept-History', 'Class-History-2024-A', 20, 5),
        ('Dept-Design', 'Class-Design-2023-A', 10, 5),
        ('Dept-Design', 'Class-Design-2024-A', 20, 5),
        ('Dept-Accounting', 'Class-Accounting-2023-A', 10, 5),
        ('Dept-Accounting', 'Class-Accounting-2024-A', 20, 5),
        ('Dept-Marketing', 'Class-Marketing-2023-A', 10, 5),
        ('Dept-Marketing', 'Class-Marketing-2024-A', 20, 5),
        ('Dept-Finance', 'Class-Finance-2023-A', 10, 5),
        ('Dept-Finance', 'Class-Finance-2024-A', 20, 5),
        ('Dept-Econ', 'Class-Econ-2023-A', 10, 5),
        ('Dept-Econ', 'Class-Econ-2024-A', 20, 5),
        ('Dept-EEE', 'Class-EEE-2023-A', 10, 5),
        ('Dept-EEE', 'Class-EEE-2024-A', 20, 5),
        ('Dept-Civil', 'Class-Civil-2023-A', 10, 5),
        ('Dept-Civil', 'Class-Civil-2024-A', 20, 5),
        ('Dept-Mechanical', 'Class-Mechanical-2023-A', 10, 5),
        ('Dept-Mechanical', 'Class-Mechanical-2024-A', 20, 5),
        ('Dept-Env', 'Class-Env-2023-A', 10, 5),
        ('Dept-Env', 'Class-Env-2024-A', 20, 5)
    ) AS v(parent_name, name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT p.id,
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM level3 v
JOIN parent_map p ON p.dept_name = v.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 1.4 Level-4 units under classes/teams
WITH parent_map AS (
    SELECT id, dept_name
    FROM sys_depts
    WHERE dept_name IN (
        'Class-CS-2023-A','Class-CS-2023-B','Class-CS-2024-A',
        'Class-Accounting-2023-A','Class-Accounting-2024-A',
        'Team-DevOps-Platform','Team-DevOps-Release','Team-Support-Helpdesk',
        'Team-Student-Services','Team-Dorm-Resident'
    )
),
level4 AS (
    SELECT * FROM (VALUES
        ('Class-CS-2023-A', 'Group-CS-2023-A1', 10, 5),
        ('Class-CS-2023-A', 'Group-CS-2023-A2', 20, 5),
        ('Class-CS-2023-B', 'Group-CS-2023-B1', 10, 5),
        ('Class-CS-2023-B', 'Group-CS-2023-B2', 20, 5),
        ('Class-CS-2024-A', 'Group-CS-2024-A1', 10, 5),
        ('Class-CS-2024-A', 'Group-CS-2024-A2', 20, 5),
        ('Class-Accounting-2023-A', 'Group-Accounting-2023-A1', 10, 5),
        ('Class-Accounting-2024-A', 'Group-Accounting-2024-A1', 10, 5),
        ('Team-DevOps-Platform', 'Squad-DevOps-Platform-A', 10, 5),
        ('Team-DevOps-Platform', 'Squad-DevOps-Platform-B', 20, 5),
        ('Team-DevOps-Release', 'Squad-DevOps-Release-A', 10, 5),
        ('Team-Support-Helpdesk', 'Squad-Support-Helpdesk-A', 10, 5),
        ('Team-Student-Services', 'Squad-Student-Services-A', 10, 5),
        ('Team-Dorm-Resident', 'Squad-Dorm-Resident-A', 10, 5)
    ) AS v(parent_name, name, sort_order, data_scope)
)
INSERT INTO sys_depts (parent_id, dept_name, sort_order, leader, status, data_scope)
SELECT p.id,
       v.name,
       v.sort_order,
       'system',
       0,
       v.data_scope
FROM level4 v
JOIN parent_map p ON p.dept_name = v.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM sys_depts d WHERE d.dept_name = v.name
);

-- 2) Users user0001..user1000 (password = Admin@123)
WITH dept_pool AS (
    SELECT array_agg(id ORDER BY id) AS ids FROM sys_depts
),
series AS (
    SELECT generate_series(1, 1000) AS n
)
INSERT INTO users (
    username, password, nickname, dept_id, user_type, verify_status, status,
    credit_score, phone, created_at, updated_at
)
SELECT
    'user' || lpad(n::text, 4, '0'),
    '$2y$10$tyTtSrcuzDqXLgTlfM0GTuh0TR7/.cFUceo6y720YixN4Anr0HaWy',
    'User ' || lpad(n::text, 4, '0'),
    (dept_pool.ids)[(n % array_length(dept_pool.ids, 1)) + 1],
    0,
    CASE WHEN n % 10 = 0 THEN 1 WHEN n % 7 = 0 THEN 0 ELSE 2 END,
    CASE WHEN n % 37 = 0 THEN 1 ELSE 0 END,
    60 + (n % 41),
    '1880000' || lpad(n::text, 4, '0'),
    NOW() - (n || ' hours')::interval,
    NOW() - (n || ' hours')::interval
FROM series, dept_pool
ON CONFLICT (username) DO NOTHING;

-- 3) Assign roles (one role per user)
WITH role_map AS (
    SELECT role_key, id
    FROM sys_roles
    WHERE role_key IN ('moderator', 'user')
),
target_users AS (
    SELECT id, username
    FROM users
    WHERE username LIKE 'user%'
)
INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id,
       (SELECT id FROM role_map WHERE role_key = CASE WHEN u.username <= 'user0020' THEN 'moderator' ELSE 'user' END)
FROM target_users u
LEFT JOIN sys_user_roles ur ON ur.user_id = u.id
WHERE ur.user_id IS NULL;

-- 4) Posts (5k) linked to seeded users
WITH user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
series AS (
    SELECT generate_series(1, 5000) AS n
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
    'Seed post ' || lpad(n::text, 5, '0'),
    'Seed content for post ' || lpad(n::text, 5, '0') || '.',
    (n % 9 = 0),
    CASE (n % 4)
        WHEN 0 THEN 'general'
        WHEN 1 THEN 'help'
        WHEN 2 THEN 'market'
        ELSE 'lost-found'
    END,
    CASE WHEN (n % 5) = 3 THEN (n % 200) + 10 ELSE NULL END,
    CASE WHEN (n % 5) = 4 THEN 'Campus Zone ' || (n % 12) ELSE NULL END,
    CASE WHEN (n % 5) = 4 THEN NOW() - ((n % 240) || ' hours')::interval ELSE NULL END,
    CASE WHEN n % 25 = 0 THEN 1 WHEN n % 40 = 0 THEN 2 ELSE 0 END,
    CASE WHEN n % 6 = 0 THEN FALSE ELSE TRUE END,
    0,
    0,
    (n * 7) % 500,
    NOW() - ((n % 240) || ' hours')::interval,
    NOW() - ((n % 240) || ' hours')::interval,
    NOW() - ((n % 240) || ' hours')::interval
FROM series
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.title = 'Seed post ' || lpad(n::text, 5, '0')
);

-- 5) Ensure post_boards are populated
INSERT INTO post_boards (post_id, board)
SELECT p.id, p.board
FROM posts p
WHERE NOT EXISTS (
    SELECT 1 FROM post_boards pb WHERE pb.post_id = p.id AND pb.board = p.board
);

-- 6) Comments (15k) on seeded posts
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Seed post %'
),
post_count AS (
    SELECT count(*) AS cnt FROM post_list
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
series AS (
    SELECT generate_series(1, 15000) AS n
)
INSERT INTO comments (
    post_id, user_id, parent_id, content, anonymous_id, is_owner, status, created_at
)
SELECT
    p.id,
    u.id,
    NULL,
    'Seed comment ' || lpad(n::text, 6, '0'),
    NULL,
    (p.id % 7 = u.id % 7),
    CASE WHEN n % 23 = 0 THEN 1 ELSE 0 END,
    NOW() - ((n % 240) || ' hours')::interval
FROM series
JOIN post_list p ON p.rn = (n % (SELECT cnt FROM post_count)) + 1
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM comments c WHERE c.content = 'Seed comment ' || lpad(n::text, 6, '0')
);

-- 7) Likes (deterministic pattern)
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Seed post %'
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%'
)
INSERT INTO likes (user_id, post_id, created_at)
SELECT u.id, p.id, NOW() - ((p.id % 120) || ' hours')::interval
FROM user_list u
JOIN post_list p ON (p.rn % 37) = (u.rn % 37)
ON CONFLICT (user_id, post_id) DO NOTHING;

-- 8) Bookmarks (deterministic pattern)
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Seed post %'
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%'
)
INSERT INTO bookmarks (user_id, post_id, created_at)
SELECT u.id, p.id, NOW() - ((p.id % 200) || ' hours')::interval
FROM user_list u
JOIN post_list p ON (p.rn % 43) = (u.rn % 43)
ON CONFLICT (user_id, post_id) DO NOTHING;

-- 9) Reports (500)
WITH post_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM posts
    WHERE title LIKE 'Seed post %'
),
post_count AS (
    SELECT count(*) AS cnt FROM post_list
),
user_list AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM users
    WHERE username LIKE 'user%'
),
user_count AS (
    SELECT count(*) AS cnt FROM user_list
),
admin_user AS (
    SELECT id FROM users WHERE username = 'admin' ORDER BY id LIMIT 1
),
series AS (
    SELECT generate_series(1, 500) AS n
)
INSERT INTO reports (
    reporter_id, post_id, reason, status, handler_id, result, created_at, handled_at
)
SELECT
    u.id,
    p.id,
    'Seed report ' || lpad(n::text, 4, '0'),
    CASE WHEN n % 3 = 0 THEN 1 ELSE 0 END,
    CASE WHEN n % 3 = 0 THEN (SELECT id FROM admin_user) ELSE NULL END,
    CASE WHEN n % 3 = 0 THEN 'auto-reviewed' ELSE NULL END,
    NOW() - ((n % 240) || ' hours')::interval,
    CASE WHEN n % 3 = 0 THEN NOW() - ((n % 120) || ' hours')::interval ELSE NULL END
FROM series
JOIN post_list p ON p.rn = (n % (SELECT cnt FROM post_count)) + 1
JOIN user_list u ON u.rn = (n % (SELECT cnt FROM user_count)) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM reports r WHERE r.reason = 'Seed report ' || lpad(n::text, 4, '0')
);

-- 10) Recalculate counters
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

COMMIT;
