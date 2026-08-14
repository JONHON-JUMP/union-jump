-- 菜单说明书（可选），用于门户快捷导航「查看说明书」
ALTER TABLE system_menu ADD COLUMN IF NOT EXISTS manual_url varchar(1024) DEFAULT NULL;
COMMENT ON COLUMN system_menu.manual_url IS '菜单说明书文件地址';

ALTER TABLE sub_system_menu ADD COLUMN IF NOT EXISTS manual_url varchar(1024) DEFAULT NULL;
COMMENT ON COLUMN sub_system_menu.manual_url IS '菜单说明书文件地址';
