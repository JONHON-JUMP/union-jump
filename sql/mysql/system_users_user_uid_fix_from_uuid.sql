-- 将已回填的 UUID 格式 user_uid 纠正为：U + yyyyMMddHHmmss + 三位流水
-- 例：U20260720170405001
-- 适用：列已存在且当前是 32 位十六进制之类的旧值

UPDATE `system_users`
SET `user_uid` = CONCAT(
        'U',
        DATE_FORMAT(IFNULL(`create_time`, NOW()), '%Y%m%d%H%i%s'),
        LPAD(MOD(`id`, 1000), 3, '0')
    )
WHERE `user_uid` IS NULL
   OR `user_uid` = ''
   OR `user_uid` NOT REGEXP '^U[0-9]{17}$';

-- 同秒末三位撞号时补两位区分
UPDATE `system_users` u
    INNER JOIN (
        SELECT `user_uid`, MIN(`id`) AS keep_id
        FROM `system_users`
        WHERE `user_uid` IS NOT NULL
        GROUP BY `user_uid`
        HAVING COUNT(*) > 1
    ) d ON u.`user_uid` = d.`user_uid` AND u.`id` <> d.keep_id
SET u.`user_uid` = CONCAT(
        'U',
        DATE_FORMAT(IFNULL(u.`create_time`, NOW()), '%Y%m%d%H%i%s'),
        LPAD(MOD(u.`id`, 1000), 3, '0'),
        LPAD(MOD(u.`id`, 100), 2, '0')
    );

-- 抽查
-- SELECT id, username, user_uid, create_time FROM system_users ORDER BY id LIMIT 20;
