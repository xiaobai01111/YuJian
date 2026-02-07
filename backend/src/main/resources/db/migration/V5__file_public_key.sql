-- 为文件增加对外访问的随机标识，避免通过ID枚举访问

ALTER TABLE files ADD COLUMN IF NOT EXISTS public_key VARCHAR(64);

UPDATE files
SET public_key = md5(id::text || '-' || random()::text || '-' || clock_timestamp()::text)
WHERE public_key IS NULL;

ALTER TABLE files ALTER COLUMN public_key SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_files_public_key ON files(public_key);
