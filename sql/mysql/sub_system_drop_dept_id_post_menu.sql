-- 移除 sub_system_role / sub_system_post / sub_system_menu 未使用的 dept_id 字段

ALTER TABLE `sub_system_role`
    DROP INDEX IF EXISTS `idx_dept_id`,
    DROP COLUMN IF EXISTS `dept_id`;

ALTER TABLE `sub_system_post`
    DROP INDEX IF EXISTS `idx_dept_id`,
    DROP COLUMN IF EXISTS `dept_id`;

ALTER TABLE `sub_system_menu`
    DROP INDEX IF EXISTS `idx_dept_id`,
    DROP COLUMN IF EXISTS `dept_id`;
