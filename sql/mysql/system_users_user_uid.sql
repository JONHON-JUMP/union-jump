-- 主系统用户跨系统唯一标识 user_uid
-- 格式：U + yyyyMMddHHmmss + 三位流水号，例 U20260720170405001
-- 执行前请备份；可重复执行时请先确认列是否已存在

ALTER TABLE `system_users`
    ADD COLUMN `user_uid` varchar(32) NULL COMMENT '跨系统唯一用户标识（U+年月日时分秒+三位流水）' AFTER `id`;

-- 存量回填 / 纠正旧 UUID：U + 创建时间 + 主键末三位
UPDATE `system_users`
SET `user_uid` = CONCAT(
        'U',
        DATE_FORMAT(IFNULL(`create_time`, NOW()), '%Y%m%d%H%i%s'),
        LPAD(MOD(`id`, 1000), 3, '0')
    )
WHERE `user_uid` IS NULL
   OR `user_uid` = ''
   OR `user_uid` NOT REGEXP '^U[0-9]{17}$';

-- 若偶发撞号，用 id 补足差异（极少见）
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

ALTER TABLE `system_users`
    MODIFY COLUMN `user_uid` varchar(32) NOT NULL COMMENT '跨系统唯一用户标识（U+年月日时分秒+三位流水）',
    ADD UNIQUE KEY `uk_user_uid` (`user_uid`);
