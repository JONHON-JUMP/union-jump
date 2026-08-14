-- 将 sub_system.client_id(varchar) 迁移为 oauth2_client_id(bigint → system_oauth2_client.id)
-- PostgreSQL，适用于已存在 client_id 列的环境

ALTER TABLE sub_system ADD COLUMN IF NOT EXISTS oauth2_client_id int8 NULL;

UPDATE sub_system ss
SET oauth2_client_id = oc.id
FROM system_oauth2_client oc
WHERE ss.client_id = oc.client_id
  AND ss.deleted = 0
  AND oc.deleted = 0;

-- 无法匹配 OAuth2 客户端的记录需人工处理后再执行下一行
-- ALTER TABLE sub_system ALTER COLUMN oauth2_client_id SET NOT NULL;

ALTER TABLE sub_system DROP COLUMN IF EXISTS client_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_system_oauth2_client_id
    ON sub_system (oauth2_client_id) WHERE deleted = 0;

COMMENT ON COLUMN sub_system.oauth2_client_id IS 'OAuth2 客户端编号，关联 system_oauth2_client.id';
