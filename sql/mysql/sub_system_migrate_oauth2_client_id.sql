-- 将 sub_system.client_id(varchar) 迁移为 oauth2_client_id(bigint → system_oauth2_client.id)
-- MySQL，适用于已存在 client_id 列的环境

ALTER TABLE `sub_system`
    ADD COLUMN `oauth2_client_id` bigint NULL COMMENT 'OAuth2 客户端编号，关联 system_oauth2_client.id' AFTER `id`;

UPDATE `sub_system` ss
    INNER JOIN `system_oauth2_client` oc ON ss.`client_id` = oc.`client_id` AND oc.`deleted` = b'0'
SET ss.`oauth2_client_id` = oc.`id`
WHERE ss.`deleted` = b'0';

ALTER TABLE `sub_system`
    DROP COLUMN `client_id`;

ALTER TABLE `sub_system`
    ADD UNIQUE KEY `uk_oauth2_client_id` (`oauth2_client_id`, `deleted`);
