-- 将 system_user_portal_default.default_system(clientId) 迁移为 sub_system_id
-- 适用于已执行旧版建表脚本（含 default_system 字段）的环境
-- 请先执行 sub_system_migrate_oauth2_client_id.sql（若 sub_system 仍为 client_id 列则先执行本脚本再迁 sub_system）

ALTER TABLE system_user_portal_default ADD COLUMN IF NOT EXISTS sub_system_id int8 NULL;

UPDATE system_user_portal_default pd
SET sub_system_id = ss.id
FROM sub_system ss
INNER JOIN system_oauth2_client oc ON (
    (ss.oauth2_client_id IS NOT NULL AND ss.oauth2_client_id = oc.id)
    OR (ss.oauth2_client_id IS NULL AND ss.client_id = oc.client_id)
)
WHERE pd.default_system IS NOT NULL
  AND pd.default_system <> 'main'
  AND pd.default_system = oc.client_id
  AND pd.deleted = 0
  AND ss.deleted = 0
  AND oc.deleted = 0;

UPDATE system_user_portal_default
SET sub_system_id = NULL
WHERE default_system = 'main';

ALTER TABLE system_user_portal_default DROP COLUMN IF EXISTS default_system;

CREATE INDEX IF NOT EXISTS idx_system_user_portal_default_sub_system_id
    ON system_user_portal_default (sub_system_id) WHERE deleted = 0;

COMMENT ON COLUMN system_user_portal_default.sub_system_id IS '默认打开的外部子系统编号，NULL 表示统一门户主页';
