-- 菜单表增加 color_id，关联 system_menu_color.id

ALTER TABLE system_menu ADD COLUMN IF NOT EXISTS color_id int8 NULL;
COMMENT ON COLUMN system_menu.color_id IS '菜单颜色编号，关联 system_menu_color.id';

ALTER TABLE sub_system_menu ADD COLUMN IF NOT EXISTS color_id int8 NULL;
COMMENT ON COLUMN sub_system_menu.color_id IS '菜单颜色编号，关联 system_menu_color.id';

CREATE INDEX IF NOT EXISTS idx_system_menu_color_id ON system_menu (color_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_sub_system_menu_color_id ON sub_system_menu (color_id) WHERE deleted = 0;
