-- system_oauth2_client.client_id 唯一约束（PostgreSQL）
-- 执行前请先运行下方重复数据检查；若存在重复需人工处理后再建索引

-- 重复检查（deleted=0 的有效记录）
SELECT client_id, COUNT(*) AS cnt
FROM system_oauth2_client
WHERE deleted = 0
GROUP BY client_id
HAVING COUNT(*) > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_oauth2_client_client_id
    ON system_oauth2_client (client_id) WHERE deleted = 0;
