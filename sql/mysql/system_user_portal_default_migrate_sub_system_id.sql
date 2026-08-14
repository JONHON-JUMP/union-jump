-- 将 system_user_portal_default.default_system(clientId) 迁移为 sub_system_id
-- 适用于已执行旧版建表脚本（含 default_system 字段）的环境

ALTER TABLE `system_user_portal_default`
    ADD COLUMN `sub_system_id` bigint NULL DEFAULT NULL COMMENT '默认打开的外部子系统编号，NULL 表示统一门户主页' AFTER `user_id`;

UPDATE `system_user_portal_default` pd
    INNER JOIN `sub_system` ss ON ss.`deleted` = b'0'
    INNER JOIN `system_oauth2_client` oc ON (
        (ss.`oauth2_client_id` IS NOT NULL AND ss.`oauth2_client_id` = oc.`id`)
        OR (ss.`oauth2_client_id` IS NULL AND ss.`client_id` = oc.`client_id`)
    ) AND oc.`deleted` = b'0'
SET pd.`sub_system_id` = ss.`id`
WHERE pd.`default_system` IS NOT NULL
  AND pd.`default_system` <> 'main'
  AND pd.`default_system` = oc.`client_id`
  AND pd.`deleted` = b'0';

UPDATE `system_user_portal_default`
SET `sub_system_id` = NULL
WHERE `default_system` = 'main';

ALTER TABLE `system_user_portal_default`
    DROP COLUMN `default_system`;

ALTER TABLE `system_user_portal_default`
    ADD INDEX `idx_sub_system_id` (`sub_system_id`, `deleted`);
