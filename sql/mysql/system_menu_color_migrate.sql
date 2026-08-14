-- 菜单表增加 color_id（MySQL）

ALTER TABLE `system_menu`
    ADD COLUMN `color_id` bigint NULL COMMENT '菜单颜色编号，关联 system_menu_color.id' AFTER `icon`;

ALTER TABLE `sub_system_menu`
    ADD COLUMN `color_id` bigint NULL COMMENT '菜单颜色编号，关联 system_menu_color.id' AFTER `icon`;
