-- ============================================================================
-- 本次更新：业务角色关联外部系统角色 ID
-- 日期：2026-10-10
-- 执行库：JUMP 主库（PostgreSQL）
-- 内容：
--   1) sub_system_role 加 external_role_id 列：JUMP 业务角色 ↔ 外部系统（Camstar）
--      角色 ID（ROLEDEF.roleId）的主键级映射。分配角色同步按 ID 上挂/解除，
--      不再依赖「车间编号_角色名」名称匹配（名称匹配不可靠，已废弃）。
-- 使用方式：
--   * 新建角色勾选「同步注册」成功后，自动回查 roleId 写入本列；
--   * 存量角色用「关联外部角色」人工绑定（业务角色管理页）；
--   * 分配角色勾选同步时，未绑定外部 roleId 的角色会中止本次同步并提示先关联。
-- 前置：sub_system_role 表已存在（upgrade_20260903_role_register.sql）。
-- ============================================================================

ALTER TABLE "sub_system_role" ADD COLUMN IF NOT EXISTS "external_role_id" varchar(32) DEFAULT NULL;
COMMENT ON COLUMN "sub_system_role"."external_role_id"
    IS '外部系统角色 ID（Camstar ROLEDEF.roleId）；同步分配角色按 ID 上挂。勾选同步时未绑定的角色会中止本次同步';
