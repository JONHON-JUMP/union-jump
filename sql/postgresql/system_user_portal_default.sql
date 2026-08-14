-- 用户门户默认打开系统配置表（PostgreSQL）
DROP TABLE IF EXISTS system_user_portal_default;
CREATE TABLE system_user_portal_default (
    id int8 NOT NULL,
    user_id int8 NOT NULL,
    sub_system_id int8 NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_user_portal_default ADD CONSTRAINT pk_system_user_portal_default PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_system_user_portal_default_user_id ON system_user_portal_default (user_id) WHERE deleted = 0;
CREATE INDEX idx_system_user_portal_default_sub_system_id ON system_user_portal_default (sub_system_id) WHERE deleted = 0;

COMMENT ON COLUMN system_user_portal_default.id IS '编号';
COMMENT ON COLUMN system_user_portal_default.user_id IS '用户编号';
COMMENT ON COLUMN system_user_portal_default.sub_system_id IS '默认打开的外部子系统编号，NULL 表示统一门户主页';
COMMENT ON COLUMN system_user_portal_default.creator IS '创建者';
COMMENT ON COLUMN system_user_portal_default.create_time IS '创建时间';
COMMENT ON COLUMN system_user_portal_default.updater IS '更新者';
COMMENT ON COLUMN system_user_portal_default.update_time IS '更新时间';
COMMENT ON COLUMN system_user_portal_default.deleted IS '是否删除';
COMMENT ON COLUMN system_user_portal_default.tenant_id IS '租户编号';
COMMENT ON TABLE system_user_portal_default IS '用户门户默认打开系统配置表';

DROP SEQUENCE IF EXISTS system_user_portal_default_seq;
CREATE SEQUENCE system_user_portal_default_seq
    START 1;
