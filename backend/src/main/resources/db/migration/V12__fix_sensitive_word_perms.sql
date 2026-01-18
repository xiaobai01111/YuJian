-- 修复敏感词权限字符串：system:sensitive:* -> system:sensitive-word:*
UPDATE sys_menus SET perms = 'system:sensitive-word:list' WHERE id = 140 AND perms = 'system:sensitive:list';
UPDATE sys_menus SET perms = 'system:sensitive-word:add' WHERE id = 141 AND perms = 'system:sensitive:add';
UPDATE sys_menus SET perms = 'system:sensitive-word:delete' WHERE id = 142 AND perms = 'system:sensitive:delete';
