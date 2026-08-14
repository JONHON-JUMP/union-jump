-- 主系统用户跨系统唯一标识 user_uid（PostgreSQL）
-- 格式：U + yyyyMMddHHmmss + 三位流水号，例 U20260720170405001

ALTER TABLE system_users ADD COLUMN IF NOT EXISTS user_uid varchar(32);

UPDATE system_users
SET user_uid = 'U'
    || to_char(COALESCE(create_time, CURRENT_TIMESTAMP), 'YYYYMMDDHH24MISS')
    || lpad((id % 1000)::text, 3, '0')
WHERE user_uid IS NULL
   OR user_uid = ''
   OR user_uid !~ '^U[0-9]{17}$';

-- 撞号补救（极少见）
UPDATE system_users u
SET user_uid = 'U'
    || to_char(COALESCE(u.create_time, CURRENT_TIMESTAMP), 'YYYYMMDDHH24MISS')
    || lpad((u.id % 1000)::text, 3, '0')
    || lpad((u.id % 100)::text, 2, '0')
WHERE u.id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY user_uid ORDER BY id) AS rn
        FROM system_users
        WHERE user_uid IS NOT NULL
    ) t
    WHERE t.rn > 1
);

ALTER TABLE system_users ALTER COLUMN user_uid SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_users_user_uid ON system_users (user_uid);

COMMENT ON COLUMN system_users.user_uid IS '跨系统唯一用户标识（U+年月日时分秒+三位流水）';
