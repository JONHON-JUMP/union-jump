-- ============================================================================
-- 本次更新：外部系统角色「接口注册」状态（PostgreSQL / JUMP 主库）
-- 日期：2026-09-03
-- 内容：
--   sub_system_role 增加 role_registered：0未注册 1已注册
--   调对方「角色新增」接口成功自动置 1；角色管理页可手改；改回 0 可重推
-- 全部幂等，可重复执行。
-- ============================================================================

ALTER TABLE "sub_system_role" ADD COLUMN IF NOT EXISTS "role_registered" varchar(2) NOT NULL DEFAULT '0';
COMMENT ON COLUMN "sub_system_role"."role_registered" IS '角色接口注册状态（0未注册 1已注册；调角色新增接口成功自动置1，页面可改）';
