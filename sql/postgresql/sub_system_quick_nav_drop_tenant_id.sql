-- 子系统快捷导航表移除 tenant_id（子系统不做租户隔离）

ALTER TABLE sub_system_role_quick_nav DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE sub_system_user_quick_nav DROP COLUMN IF EXISTS tenant_id;
