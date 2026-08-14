-- 子系统用户快捷导航表重命名（PostgreSQL）
-- 由 system_user_sub_system_quick_nav 改为 sub_system_user_quick_nav

ALTER TABLE IF EXISTS system_user_sub_system_quick_nav RENAME TO sub_system_user_quick_nav;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'system_user_sub_system_quick_nav_seq') THEN
        ALTER SEQUENCE system_user_sub_system_quick_nav_seq RENAME TO sub_system_user_quick_nav_seq;
    END IF;
END $$;

COMMENT ON TABLE sub_system_user_quick_nav IS '用户外部子系统快捷导航配置表';
