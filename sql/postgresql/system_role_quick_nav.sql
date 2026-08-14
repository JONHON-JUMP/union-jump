-- 角色默认快捷导航配置表（主系统，PostgreSQL）
DROP TABLE IF EXISTS system_role_quick_nav;
CREATE TABLE system_role_quick_nav (
    id int8 NOT NULL,
    role_id int8 NOT NULL,
    menu_id int8 NOT NULL,
    sort int4 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0
);

ALTER TABLE system_role_quick_nav ADD CONSTRAINT pk_system_role_quick_nav PRIMARY KEY (id);
CREATE INDEX idx_system_role_quick_nav_role_id ON system_role_quick_nav (role_id) WHERE deleted = 0;

COMMENT ON TABLE system_role_quick_nav IS '角色默认快捷导航配置表（主系统）';
COMMENT ON COLUMN system_role_quick_nav.role_id IS '角色编号';
COMMENT ON COLUMN system_role_quick_nav.menu_id IS '菜单编号';
COMMENT ON COLUMN system_role_quick_nav.sort IS '显示顺序';

DROP SEQUENCE IF EXISTS system_role_quick_nav_seq;
CREATE SEQUENCE system_role_quick_nav_seq START 1;
