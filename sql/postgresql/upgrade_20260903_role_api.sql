-- ============================================================================
-- 本次更新：Camstar 角色接口接入接口管理（PostgreSQL / JUMP 主库）
-- 日期：2026-09-03
-- 内容：
--   1) sub_system_api_config 加角色三列：api_role_query / api_role_create / api_role_delete
--   2) camstar 型存量配置预填默认角色接口（查询/新增/删除；只建裸角色，不挂页面）
-- 全部幂等，可重复执行。
-- 前置：sub_system_api_config 表已存在（sub_system_workshop_and_api.sql）。
-- 说明：
--   * 角色接口仅用于接口管理页「在线测试」调用（apiKey=role_query/role_create/role_delete）；
--   * Camstar 契约：全部 POST；新增/删除要求会话工号挂「管理员/Administrator」角色；
--   * updateRoleInfo 传 roleId 为空的数组即新增（裸角色，不带页面权限）。
-- ============================================================================

-- 1. 角色接口三列（幂等）
ALTER TABLE "sub_system_api_config" ADD COLUMN IF NOT EXISTS "api_role_query" varchar(512);
COMMENT ON COLUMN "sub_system_api_config"."api_role_query" IS '角色查询接口 JSON：{"name","url"|"path","method","enabled","withSession"}';
ALTER TABLE "sub_system_api_config" ADD COLUMN IF NOT EXISTS "api_role_create" varchar(512);
COMMENT ON COLUMN "sub_system_api_config"."api_role_create" IS '角色新增接口 JSON（只建裸角色，不挂页面）';
ALTER TABLE "sub_system_api_config" ADD COLUMN IF NOT EXISTS "api_role_delete" varchar(512);
COMMENT ON COLUMN "sub_system_api_config"."api_role_delete" IS '角色删除接口 JSON';

-- 2. camstar 型存量配置预填默认角色接口（幂等：仅 api_role_query 为空的行）
UPDATE "sub_system_api_config"
SET "api_role_query"  = '{"name":"角色查询","url":"' || "base_url" || '/BasicData/Role/getRoleInfo","method":"POST","enabled":true}',
    "api_role_create" = '{"name":"角色新增","url":"' || "base_url" || '/BasicData/Role/updateRoleInfo","method":"POST","enabled":true}',
    "api_role_delete" = '{"name":"角色删除","url":"' || "base_url" || '/BasicData/Role/deleteRoleInfo","method":"POST","enabled":true}',
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND "api_type" = 'camstar'
  AND ("api_role_query" IS NULL OR "api_role_query" = '');
