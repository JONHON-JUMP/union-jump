-- 菜单样式表移除浅底字段（门户仅使用主色 color）

ALTER TABLE system_menu_style DROP COLUMN IF EXISTS bg_color;
