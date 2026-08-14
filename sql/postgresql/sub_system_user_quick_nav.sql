-- 用户外部子系统快捷导航配置表（PostgreSQL，新环境建表）
DROP TABLE IF EXISTS sub_system_user_quick_nav;
CREATE TABLE sub_system_user_quick_nav (
    id int8 NOT NULL,
    user_id int8 NOT NULL,
    sub_system_id int8 NOT NULL,
    menu_id int8 NOT NULL,
    sort int4 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE sub_system_user_quick_nav ADD CONSTRAINT pk_sub_system_user_quick_nav PRIMARY KEY (id);

COMMENT ON COLUMN sub_system_user_quick_nav.id IS '编号';
COMMENT ON COLUMN sub_system_user_quick_nav.user_id IS '主系统用户编号';
COMMENT ON COLUMN sub_system_user_quick_nav.sub_system_id IS '外部子系统编号';
COMMENT ON COLUMN sub_system_user_quick_nav.menu_id IS '子系统菜单编号';
COMMENT ON COLUMN sub_system_user_quick_nav.sort IS '显示顺序';
COMMENT ON COLUMN sub_system_user_quick_nav.creator IS '创建者';
COMMENT ON COLUMN sub_system_user_quick_nav.create_time IS '创建时间';
COMMENT ON COLUMN sub_system_user_quick_nav.updater IS '更新者';
COMMENT ON COLUMN sub_system_user_quick_nav.update_time IS '更新时间';
COMMENT ON COLUMN sub_system_user_quick_nav.deleted IS '是否删除';
COMMENT ON TABLE sub_system_user_quick_nav IS '用户外部子系统快捷导航配置表';

DROP SEQUENCE IF EXISTS sub_system_user_quick_nav_seq;
CREATE SEQUENCE sub_system_user_quick_nav_seq
    START 1;
