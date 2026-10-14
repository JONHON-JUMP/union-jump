-- ============================================================================
-- 本次更新：sub_system_api_config 竖向化（一行一接口）
-- 日期：2026-09-30
-- 执行库：JUMP 主库（PostgreSQL）
-- 内容：
--   1) 新表 sub_system_api_endpoint：一行 = 一个接口（purpose 区分用途）
--   2) 存量 8 个 JSON 接口列拆行迁入新表（幂等，可重复执行）
--   3) 主表删除 api_query/api_create/api_update/api_delete/api_team_combo/
--      api_role_query/api_role_create/api_role_delete/api_catalog 列
-- 前置：sub_system_api_config 表已存在（sub_system_workshop_and_api.sql）。
-- 说明：
--   * 主表保留系统级配置：base_url/api_type/auth_type/auth_config/
--     param_mapping/response_mapping/delete_tip/超时/status；鉴权仍系统级；
--   * 接口行与适配器类型无关（camstar/http 通用），新增接口类型 = 加一行；
--   * group_code 供前端固定分组渲染：person=人员接口 / role=角色接口 / other=其他；
--   * purpose 为空串 = 未指定用途的自定义接口行（不参与唯一约束）；
--   * assign_role（人员分配角色）从旧 api_update 列复制一份：Camstar 现网与修改人员
--     同一条 addOrUpdateUser，请求体 roleOnly=true 区分；未配置时适配器回落 update 行。
-- ============================================================================

-- 1. 接口表（幂等）
CREATE SEQUENCE IF NOT EXISTS sub_system_api_endpoint_seq;

CREATE TABLE IF NOT EXISTS "sub_system_api_endpoint" (
    "id"            bigint       NOT NULL DEFAULT nextval('sub_system_api_endpoint_seq'),
    "sub_system_id" bigint       NOT NULL,
    "purpose"       varchar(32)  NOT NULL DEFAULT '',
    "group_code"    varchar(32)  NOT NULL DEFAULT 'other',
    "name"          varchar(64)  NOT NULL DEFAULT '',
    "url"           varchar(512) NOT NULL DEFAULT '',
    "method"        varchar(10)  NOT NULL DEFAULT 'POST',
    "enabled"       smallint     NOT NULL DEFAULT 1,
    "with_session"  smallint     NOT NULL DEFAULT 1,
    "sort"          int          NOT NULL DEFAULT 0,
    "creator"       varchar(64)  DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       smallint     NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS idx_sub_system_api_endpoint_system ON "sub_system_api_endpoint"("sub_system_id");
COMMENT ON TABLE "sub_system_api_endpoint" IS '子系统接口配置表（一行 = 一个接口）';
COMMENT ON COLUMN "sub_system_api_endpoint"."sub_system_id" IS '外部系统 ID（sub_system.id）';
COMMENT ON COLUMN "sub_system_api_endpoint"."purpose" IS '用途：query/create/update/delete/team_combo/role_query/role_create/role_delete；空串=未指定';
COMMENT ON COLUMN "sub_system_api_endpoint"."group_code" IS '前端分组：person=人员接口 / role=角色接口 / other=其他';
COMMENT ON COLUMN "sub_system_api_endpoint"."url" IS '完整地址或相对路径（相对时拼主表 base_url）';
COMMENT ON COLUMN "sub_system_api_endpoint"."with_session" IS '是否携带系统会话 Cookie：1=携带 0=不带（null 默认携带语义落库为 1）';
COMMENT ON COLUMN "sub_system_api_endpoint"."sort" IS '组内排序';
DROP INDEX IF EXISTS uk_sub_system_api_endpoint_system_purpose;
CREATE UNIQUE INDEX uk_sub_system_api_endpoint_system_purpose
    ON "sub_system_api_endpoint"("sub_system_id", "purpose")
    WHERE "deleted" = 0 AND "purpose" <> '';

-- 2. 存量拆行（幂等：仅拆未拆过的 (sub_system_id, purpose)）
--    各列 JSON：{"name","url"|"path","method","enabled","withSession"}（缺省 method=POST、enabled=1、withSession=1）
INSERT INTO "sub_system_api_endpoint"
    ("sub_system_id", "purpose", "group_code", "name", "url", "method", "enabled", "with_session", "sort", "creator", "updater")
SELECT c."sub_system_id", v."purpose", v."group_code",
       CASE v."purpose" WHEN 'assign_role' THEN '人员分配角色'
            ELSE COALESCE(NULLIF(j->>'name', ''), v."purpose") END,
       COALESCE(j->>'url', j->>'path', ''),
       COALESCE(NULLIF(j->>'method', ''), 'POST'),
       CASE WHEN lower(j->>'enabled') = 'false' THEN 0 ELSE 1 END,
       CASE WHEN lower(j->>'withSession') = 'false' THEN 0 ELSE 1 END,
       v."sort", '1', '1'
FROM "sub_system_api_config" c
CROSS JOIN (VALUES
    ('query',       'person', 10),
    ('create',      'person', 20),
    ('update',      'person', 30),
    ('assign_role', 'person', 35),
    ('delete',      'person', 40),
    ('team_combo',  'person', 50),
    ('role_query',  'role',   10),
    ('role_create', 'role',   20),
    ('role_delete', 'role',   30)
) AS v("purpose", "group_code", "sort")
CROSS JOIN LATERAL (
    SELECT CASE v."purpose"
        WHEN 'query'       THEN NULLIF(c."api_query", '')
        WHEN 'create'      THEN NULLIF(c."api_create", '')
        WHEN 'update'      THEN NULLIF(c."api_update", '')
        WHEN 'assign_role' THEN NULLIF(c."api_update", '')   -- 分配角色与修改人员同接口（roleOnly 参数区分）
        WHEN 'delete'      THEN NULLIF(c."api_delete", '')
        WHEN 'team_combo'  THEN NULLIF(c."api_team_combo", '')
        WHEN 'role_query'  THEN NULLIF(c."api_role_query", '')
        WHEN 'role_create' THEN NULLIF(c."api_role_create", '')
        WHEN 'role_delete' THEN NULLIF(c."api_role_delete", '')
    END::jsonb AS j
) x
WHERE c."deleted" = 0
  AND x.j IS NOT NULL
  AND COALESCE(x.j->>'url', x.j->>'path', '') <> ''
  AND NOT EXISTS (
      SELECT 1 FROM "sub_system_api_endpoint" e
      WHERE e."sub_system_id" = c."sub_system_id"
        AND e."purpose" = v."purpose"
        AND e."deleted" = 0
  );

-- 3. 主表删除旧接口列（拆行完成后执行；执行前建议备份该表）
ALTER TABLE "sub_system_api_config"
    DROP COLUMN IF EXISTS "api_query",
    DROP COLUMN IF EXISTS "api_create",
    DROP COLUMN IF EXISTS "api_update",
    DROP COLUMN IF EXISTS "api_delete",
    DROP COLUMN IF EXISTS "api_team_combo",
    DROP COLUMN IF EXISTS "api_role_query",
    DROP COLUMN IF EXISTS "api_role_create",
    DROP COLUMN IF EXISTS "api_role_delete",
    DROP COLUMN IF EXISTS "api_catalog";

-- 附：新增接入的默认接口行（新装库可参考；现场已有配置请走界面维护）
-- INSERT INTO "sub_system_api_endpoint" ("sub_system_id","purpose","group_code","name","url","method")
-- VALUES (:subSystemId, 'query', 'person', '查询', :baseUrl || '/BasicData/Employee/getEmployeeInfo', 'POST') ...
