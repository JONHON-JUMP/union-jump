-- system_oauth2_client.client_id 唯一约束（MySQL）
-- 执行前请先运行下方重复数据检查；若存在重复需人工处理后再建索引

-- 重复检查（deleted=0 的有效记录）
SELECT client_id, COUNT(*) AS cnt
FROM `system_oauth2_client`
WHERE deleted = b'0'
GROUP BY client_id
HAVING COUNT(*) > 1;

ALTER TABLE `system_oauth2_client`
    ADD UNIQUE KEY `uk_client_id` (`client_id`, `deleted`);
