-- 移除 sub_system_role / sub_system_post / sub_system_menu 未使用的 dept_id 字段

DROP INDEX IF EXISTS idx_sub_system_role_dept_id;
ALTER TABLE sub_system_role DROP COLUMN IF EXISTS dept_id;

DROP INDEX IF EXISTS idx_sub_system_post_dept_id;
ALTER TABLE sub_system_post DROP COLUMN IF EXISTS dept_id;

DROP INDEX IF EXISTS idx_sub_system_menu_dept_id;
ALTER TABLE sub_system_menu DROP COLUMN IF EXISTS dept_id;
