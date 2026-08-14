-- 清理快捷导航历史软删积压（一次性）
-- deleted 字段为 smallint：0 未删，1 已删

DELETE FROM system_user_quick_nav WHERE deleted = 1;
DELETE FROM sub_system_user_quick_nav WHERE deleted = 1;
DELETE FROM system_role_quick_nav WHERE deleted = 1;
DELETE FROM sub_system_role_quick_nav WHERE deleted = 1;
