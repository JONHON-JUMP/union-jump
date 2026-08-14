-- 角色默认快捷导航配置表（外部子系统，PostgreSQL）
DROP TABLE IF EXISTS sub_system_role_quick_nav;
CREATE TABLE sub_system_role_quick_nav (
    id int8 NOT NULL,
    role_id int8 NOT NULL,
    sub_system_id int8 NOT NULL,
    menu_id int8 NOT NULL,
    sort int4 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE sub_system_role_quick_nav ADD CONSTRAINT pk_sub_system_role_quick_nav PRIMARY KEY (id);
CREATE INDEX idx_sub_system_role_quick_nav_role_id ON sub_system_role_quick_nav (role_id) WHERE deleted = 0;
CREATE INDEX idx_sub_system_role_quick_nav_sub_system_id ON sub_system_role_quick_nav (sub_system_id) WHERE deleted = 0;

COMMENT ON TABLE sub_system_role_quick_nav IS '角色默认快捷导航配置表（外部子系统）';
COMMENT ON COLUMN sub_system_role_quick_nav.role_id IS '外部子系统角色编号';
COMMENT ON COLUMN sub_system_role_quick_nav.sub_system_id IS '外部子系统编号';
COMMENT ON COLUMN sub_system_role_quick_nav.menu_id IS '子系统菜单编号';
COMMENT ON COLUMN sub_system_role_quick_nav.sort IS '显示顺序';

DROP SEQUENCE IF EXISTS sub_system_role_quick_nav_seq;
CREATE SEQUENCE sub_system_role_quick_nav_seq START 1;
