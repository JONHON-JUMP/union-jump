-- 角色系统头像配置表（PostgreSQL）

CREATE TABLE IF NOT EXISTS system_role_avatar (
    id int8 NOT NULL,
    role_code varchar(64) NOT NULL,
    avatar_url varchar(512) NOT NULL,
    sort int4 NOT NULL DEFAULT 0,
    status int2 NOT NULL DEFAULT 0,
    remark varchar(255) NULL,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE system_role_avatar ADD CONSTRAINT pk_system_role_avatar PRIMARY KEY (id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_role_avatar_role_code ON system_role_avatar (role_code) WHERE deleted = 0;

COMMENT ON TABLE system_role_avatar IS '角色系统头像配置';
COMMENT ON COLUMN system_role_avatar.role_code IS '角色标识，对应 system_role.code';
COMMENT ON COLUMN system_role_avatar.avatar_url IS '头像访问 URL';
COMMENT ON COLUMN system_role_avatar.sort IS '显示排序，越小优先级越高';
