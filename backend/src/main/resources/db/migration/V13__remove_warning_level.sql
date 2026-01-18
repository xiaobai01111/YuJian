-- 移除警告级别，全部改为拦截
UPDATE sensitive_words SET level = 2 WHERE level = 1;

-- 更新初始数据中的警告词为拦截
UPDATE sensitive_words SET level = 2 WHERE level != 2;
