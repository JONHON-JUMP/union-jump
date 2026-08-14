-- sub_system 关联 OAuth2 客户端：使用 system_oauth2_client.id（PostgreSQL 新环境）
-- 若从旧版 client_id(varchar) 升级，请执行 sub_system_migrate_oauth2_client_id.sql

ALTER TABLE sub_system DROP COLUMN IF EXISTS client_id;

ALTER TABLE sub_system ADD COLUMN IF NOT EXISTS oauth2_client_id int8 NULL;

-- 新表场景（无历史 client_id 列时上面 DROP 无影响，需保证 NOT NULL）
-- 完整建表见下方注释块；已有库请走迁移脚本

COMMENT ON COLUMN sub_system.oauth2_client_id IS 'OAuth2 客户端编号，关联 system_oauth2_client.id';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_system_oauth2_client_id
    ON sub_system (oauth2_client_id) WHERE deleted = 0;

-- 全新安装 sub_system 表示例（与 create_tables.sql 保持一致时可单独执行）：
-- CREATE TABLE sub_system (
--     id int8 NOT NULL,
--     oauth2_client_id int8 NOT NULL,
--     system_name varchar(100) NOT NULL DEFAULT '',
--     description varchar(255) NULL,
--     system_url varchar(255) NULL,
--     system_icon varchar(255) NULL,
--     status int2 NOT NULL DEFAULT 0,
--     creator varchar(64) NULL DEFAULT '',
--     create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updater varchar(64) NULL DEFAULT '',
--     update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     deleted int2 NOT NULL DEFAULT 0,
--     PRIMARY KEY (id)
-- );
