-- 将已回填的 UUID 格式 user_uid 纠正为：U + yyyyMMddHHmmss + 三位流水
-- 例：U20260720170405001
-- 适用：列已存在且当前是 32 位十六进制之类的旧值

UPDATE system_users
SET user_uid = 'U'
    || to_char(COALESCE(create_time, CURRENT_TIMESTAMP), 'YYYYMMDDHH24MISS')
    || lpad((id % 1000)::text, 3, '0')
WHERE user_uid IS NULL
   OR user_uid = ''
   OR user_uid !~ '^U[0-9]{17}$';

-- 同秒末三位撞号时补两位区分
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

-- 抽查
-- SELECT id, username, user_uid, create_time FROM system_users ORDER BY id LIMIT 20;
