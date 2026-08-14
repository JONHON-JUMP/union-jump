-- 菜单颜色表升级为菜单样式表（颜色 + 形状），菜单关联字段改为 style_id

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'system_menu_color')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'system_menu_style') THEN
        ALTER TABLE system_menu_color RENAME TO system_menu_style;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS system_menu_style (
    id int8 NOT NULL,
    name varchar(50) NOT NULL,
    shape varchar(20) NOT NULL DEFAULT 'rounded',
    color varchar(7) NOT NULL,
    mes_category varchar(32) NULL,
    remark varchar(500) NULL,
    sort int4 NOT NULL DEFAULT 0,
    status int2 NOT NULL DEFAULT 0,
    creator varchar(64) NULL DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) NULL DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0
);

ALTER TABLE system_menu_style ADD COLUMN IF NOT EXISTS shape varchar(20) NOT NULL DEFAULT 'rounded';
UPDATE system_menu_style SET shape = 'rounded' WHERE shape IS NULL OR shape = '';

COMMENT ON TABLE system_menu_style IS '菜单样式主数据表（颜色+形状）';
COMMENT ON COLUMN system_menu_style.shape IS '图标形状：rounded/square/circle/pill';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'system_menu' AND column_name = 'color_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'system_menu' AND column_name = 'style_id') THEN
        ALTER TABLE system_menu RENAME COLUMN color_id TO style_id;
    END IF;
END $$;

ALTER TABLE system_menu ADD COLUMN IF NOT EXISTS style_id int8 NULL;
COMMENT ON COLUMN system_menu.style_id IS '菜单样式编号，关联 system_menu_style.id';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sub_system_menu' AND column_name = 'color_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'sub_system_menu' AND column_name = 'style_id') THEN
        ALTER TABLE sub_system_menu RENAME COLUMN color_id TO style_id;
    END IF;
END $$;

ALTER TABLE sub_system_menu ADD COLUMN IF NOT EXISTS style_id int8 NULL;
COMMENT ON COLUMN sub_system_menu.style_id IS '菜单样式编号，关联 system_menu_style.id';

CREATE INDEX IF NOT EXISTS idx_system_menu_style_id ON system_menu (style_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sub_system_menu_style_id ON sub_system_menu (style_id) WHERE deleted = 0;
