-- 快捷导航相关：一次性迁移脚本（PostgreSQL）
-- 在 mes_db 库执行本文件，完成：旧表重命名 + 角色默认快捷导航建表

-- ========== 1. 用户子系统快捷导航表重命名 ==========
-- system_user_sub_system_quick_nav → sub_system_user_quick_nav
ALTER TABLE IF EXISTS system_user_sub_system_quick_nav RENAME TO sub_system_user_quick_nav;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'system_user_sub_system_quick_nav_seq') THEN
        ALTER SEQUENCE system_user_sub_system_quick_nav_seq RENAME TO sub_system_user_quick_nav_seq;
    END IF;
END $$;

COMMENT ON TABLE sub_system_user_quick_nav IS '用户外部子系统快捷导航配置表';

-- ========== 2. 角色默认快捷导航（主系统） ==========
CREATE TABLE IF NOT EXISTS system_role_quick_nav (
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

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'pk_system_role_quick_nav'
    ) THEN
        ALTER TABLE system_role_quick_nav ADD CONSTRAINT pk_system_role_quick_nav PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_system_role_quick_nav_role_id
    ON system_role_quick_nav (role_id) WHERE deleted = 0;

COMMENT ON TABLE system_role_quick_nav IS '角色默认快捷导航配置表（主系统）';
COMMENT ON COLUMN system_role_quick_nav.role_id IS '角色编号';
COMMENT ON COLUMN system_role_quick_nav.menu_id IS '菜单编号';
COMMENT ON COLUMN system_role_quick_nav.sort IS '显示顺序';

CREATE SEQUENCE IF NOT EXISTS system_role_quick_nav_seq START 1;

-- ========== 3. 角色默认快捷导航（外部子系统） ==========
CREATE TABLE IF NOT EXISTS sub_system_role_quick_nav (
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

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'pk_sub_system_role_quick_nav'
    ) THEN
        ALTER TABLE sub_system_role_quick_nav ADD CONSTRAINT pk_sub_system_role_quick_nav PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sub_system_role_quick_nav_role_id
    ON sub_system_role_quick_nav (role_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sub_system_role_quick_nav_sub_system_id
    ON sub_system_role_quick_nav (sub_system_id) WHERE deleted = 0;

COMMENT ON TABLE sub_system_role_quick_nav IS '角色默认快捷导航配置表（外部子系统）';
COMMENT ON COLUMN sub_system_role_quick_nav.role_id IS '外部子系统角色编号';
COMMENT ON COLUMN sub_system_role_quick_nav.sub_system_id IS '外部子系统编号';
COMMENT ON COLUMN sub_system_role_quick_nav.menu_id IS '子系统菜单编号';
COMMENT ON COLUMN sub_system_role_quick_nav.sort IS '显示顺序';

CREATE SEQUENCE IF NOT EXISTS sub_system_role_quick_nav_seq START 1;
