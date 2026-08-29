-- ============================================================================
-- 车间对照 + 人员接口接入 DDL / 菜单说明（PostgreSQL）
-- 执行库：JUMP 主库
-- 产品约定：
--   1) 车间页 = JUMP 部门 ↔ 业务系统车间对照（单表，不按左侧选系统）
--   2) 接口页 = 菜单式树：目录(系统/鉴权/人员) + 叶子接口；每叶子独立完整 URL / 启停 / 测试
--   3) 不提供独立「子系统人员」菜单页（与外部用户管理重复；后端联动接口可保留）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 外部车间表：JUMP 部门 ↔ 子系统车间 映射
-- ----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS sub_system_workshop_seq;

CREATE TABLE IF NOT EXISTS "sub_system_workshop" (
    "id"            bigint       NOT NULL DEFAULT nextval('sub_system_workshop_seq'),
    "sub_system_id" bigint       NOT NULL,
    "dept_id"       bigint       DEFAULT NULL,
    "workshop_code" varchar(100) NOT NULL DEFAULT '',
    "workshop_name" varchar(100) NOT NULL DEFAULT '',
    "description"   varchar(200) DEFAULT NULL,
    "creator"       varchar(64)  DEFAULT '',
    "create_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"       varchar(64)  DEFAULT '',
    "update_time"   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"       smallint     NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
CREATE INDEX IF NOT EXISTS idx_sub_system_workshop_system ON "sub_system_workshop"("sub_system_id");
CREATE INDEX IF NOT EXISTS idx_sub_system_workshop_dept ON "sub_system_workshop"("dept_id");
COMMENT ON TABLE "sub_system_workshop" IS '子系统车间信息表（JUMP部门与子系统车间映射）';
COMMENT ON COLUMN "sub_system_workshop"."sub_system_id" IS '外部系统 ID（sub_system.id）';
COMMENT ON COLUMN "sub_system_workshop"."dept_id" IS 'JUMP 部门 ID（system_dept.id），多个部门可映射同一车间';
COMMENT ON COLUMN "sub_system_workshop"."workshop_code" IS '业务系统车间编码（如制造部门编号 4200；与「Camstar人员管理」系统名无关）';
COMMENT ON COLUMN "sub_system_workshop"."workshop_name" IS '子系统车间名称';

-- ----------------------------------------------------------------------------
-- 2. 子系统人员接口配置表：每系统一行，页面维护
-- ----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS sub_system_api_config_seq;

CREATE TABLE IF NOT EXISTS "sub_system_api_config" (
    "id"                 bigint        NOT NULL DEFAULT nextval('sub_system_api_config_seq'),
    "sub_system_id"      bigint        NOT NULL,
    "api_type"           varchar(32)   NOT NULL DEFAULT 'http',
    "base_url"           varchar(255)  NOT NULL DEFAULT '',
    "auth_type"          varchar(32)   NOT NULL DEFAULT 'none',
    "auth_config"        varchar(1024) DEFAULT NULL,
    "api_query"          varchar(512)  DEFAULT NULL,
    "api_create"         varchar(512)  DEFAULT NULL,
    "api_update"         varchar(512)  DEFAULT NULL,
    "api_delete"         varchar(512)  DEFAULT NULL,
    "api_team_combo"     varchar(512)  DEFAULT NULL,
    "api_catalog"        text          DEFAULT NULL,
    "param_mapping"      varchar(2048) DEFAULT NULL,
    "response_mapping"   varchar(2048) DEFAULT NULL,
    "delete_tip"         varchar(200)  DEFAULT NULL,
    "connect_timeout_ms" bigint        NOT NULL DEFAULT 10000,
    "read_timeout_ms"    bigint        NOT NULL DEFAULT 30000,
    "status"             smallint      NOT NULL DEFAULT 0,
    "creator"            varchar(64)   DEFAULT '',
    "create_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updater"            varchar(64)   DEFAULT '',
    "update_time"        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "deleted"            smallint      NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_system_api_config_system ON "sub_system_api_config"("sub_system_id") WHERE "deleted" = 0;
COMMENT ON TABLE "sub_system_api_config" IS '子系统人员接口配置表';
COMMENT ON COLUMN "sub_system_api_config"."api_type" IS '适配器类型：camstar=Camstar专用（Cookie会话）、http=通用HTTP（配置驱动）';
COMMENT ON COLUMN "sub_system_api_config"."auth_config" IS '鉴权叶子接口 JSON：{"name","url","method","enabled","userCode","cookieName"}（兼容旧 loginPath）';
COMMENT ON COLUMN "sub_system_api_config"."api_query" IS '查询叶子 JSON：{"name","url"|"path","method","enabled"}';
COMMENT ON COLUMN "sub_system_api_config"."api_create" IS '新增叶子 JSON，格式同 api_query；enabled=false 时用户同步下拉不展示该系统';
COMMENT ON COLUMN "sub_system_api_config"."api_delete" IS '删除叶子 JSON，格式同 api_query';
COMMENT ON COLUMN "sub_system_api_config"."api_team_combo" IS '班组下拉接口 JSON，格式同 api_query（可选）';
COMMENT ON COLUMN "sub_system_api_config"."api_catalog" IS '接口目录树 JSON：目录+叶子；叶子 purpose=auth/query/create/update/delete；用户同步调 create';
COMMENT ON COLUMN "sub_system_api_config"."param_mapping" IS '参数映射 JSON：JUMP标准参数名→对方参数名，如 {"userCode":"empNo"}';
COMMENT ON COLUMN "sub_system_api_config"."response_mapping" IS '响应映射 JSON（http适配器用）：{"successField":"code","successValue":"200","listPath":"data.list","totalPath":"data.total"}';
COMMENT ON COLUMN "sub_system_api_config"."delete_tip" IS '删除二次确认提示语（如：将同时删除该用户的域账号）';
COMMENT ON COLUMN "sub_system_api_config"."status" IS '兼容字段；业务启停以各叶子 enabled 为准';

-- ----------------------------------------------------------------------------
-- 3. 升级：接口目标 ≠ JUMP 门户业务系统；MES4200 ≠ Camstar人员管理
-- ----------------------------------------------------------------------------
-- 人员接口可「手动新建」仅接口系统（oauth2_client_id 为空），不进入外部用户管理左侧列表
ALTER TABLE "sub_system" ALTER COLUMN "oauth2_client_id" DROP NOT NULL;
COMMENT ON COLUMN "sub_system"."oauth2_client_id" IS 'OAuth2 客户端 ID；为空=仅接口目标（如 Camstar人员管理），非门户业务系统';

-- 纠错：此前误把 MES4200 门户系统改名为 Camstar人员管理 → 改回 MES4200
UPDATE "sub_system" ss
SET "system_name" = 'MES4200',
    "update_time" = CURRENT_TIMESTAMP
FROM "system_oauth2_client" oc
WHERE ss."oauth2_client_id" = oc."id"
  AND ss."deleted" = 0
  AND oc."client_id" IN ('mes4200', 'MES4200')
  AND ss."system_name" ILIKE '%Camstar%';

UPDATE "sub_system"
SET "system_name" = 'MES4200',
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND "oauth2_client_id" IS NOT NULL
  AND "system_name" = 'Camstar人员管理';

-- 单独创建「Camstar人员管理」仅接口目标（无 OAuth；与 MES4200 门户系统分离）
-- 现场库 id 往往无 IDENTITY 默认值，须显式 nextval（与 MyBatis @KeySequence("sub_system_seq") 一致）
CREATE SEQUENCE IF NOT EXISTS sub_system_seq;
SELECT setval('sub_system_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM "sub_system"), 1));

INSERT INTO "sub_system"
    ("id", "oauth2_client_id", "system_name", "description", "status", "creator", "create_time", "updater", "update_time", "deleted")
SELECT nextval('sub_system_seq'), NULL, 'Camstar人员管理', 'Camstar 人员接口目标（非 JUMP 门户业务系统）', 0, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
    SELECT 1 FROM "sub_system"
    WHERE "system_name" = 'Camstar人员管理' AND "oauth2_client_id" IS NULL AND "deleted" = 0
);

-- 迁移前：若 Camstar人员管理 上已有占位配置，而 MES4200 上仍有真实 camstar 配置，先清掉占位，避免唯一约束挡住迁移
UPDATE "sub_system_api_config" stub
SET "deleted" = 1, "update_time" = CURRENT_TIMESTAMP
FROM "sub_system" cam
CROSS JOIN "sub_system" mes
INNER JOIN "system_oauth2_client" oc ON oc."id" = mes."oauth2_client_id"
INNER JOIN "sub_system_api_config" real_cfg
        ON real_cfg."sub_system_id" = mes."id" AND real_cfg."deleted" = 0
WHERE stub."sub_system_id" = cam."id"
  AND stub."deleted" = 0
  AND stub."id" <> real_cfg."id"
  AND cam."system_name" = 'Camstar人员管理'
  AND cam."oauth2_client_id" IS NULL
  AND cam."deleted" = 0
  AND mes."deleted" = 0
  AND oc."client_id" IN ('mes4200', 'MES4200');

-- 把挂在 MES4200 门户系统上的接口配置迁到「Camstar人员管理」（接口管理左侧应显示 Camstar人员管理，而不是 MES4200）
UPDATE "sub_system_api_config" cfg
SET "sub_system_id" = cam."id",
    "update_time" = CURRENT_TIMESTAMP
FROM "sub_system" cam
CROSS JOIN "sub_system" mes
INNER JOIN "system_oauth2_client" oc ON oc."id" = mes."oauth2_client_id"
WHERE cam."system_name" = 'Camstar人员管理'
  AND cam."oauth2_client_id" IS NULL
  AND cam."deleted" = 0
  AND mes."deleted" = 0
  AND oc."client_id" IN ('mes4200', 'MES4200')
  AND cfg."sub_system_id" = mes."id"
  AND cfg."deleted" = 0
  AND NOT EXISTS (
      SELECT 1 FROM "sub_system_api_config" x
      WHERE x."sub_system_id" = cam."id" AND x."deleted" = 0
  );

-- 若尚无接口配置：给 Camstar人员管理 写一份默认 camstar 配置（base_url 待现场改）
CREATE SEQUENCE IF NOT EXISTS sub_system_api_config_seq;
SELECT setval('sub_system_api_config_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM "sub_system_api_config"), 1));

INSERT INTO "sub_system_api_config"
    ("id", "sub_system_id", "api_type", "base_url", "auth_type", "auth_config", "api_query", "api_create", "api_update", "api_delete", "api_team_combo", "delete_tip", "status")
SELECT nextval('sub_system_api_config_seq'), cam."id", 'camstar', 'http://127.0.0.1:8090', 'cookie_sso',
       '{"name":"SSO登录","url":"http://127.0.0.1:8090/Base/SSOLogin/SSOLoginIn","method":"GET","enabled":true,"userCode":"","cookieName":"Nancal_Cam_SessionId"}',
       '{"name":"查询","url":"http://127.0.0.1:8090/BasicData/Employee/getEmployeeInfo","method":"POST","enabled":true}',
       '{"name":"新增","url":"http://127.0.0.1:8090/BasicData/Employee/addOrUpdateUser","method":"POST","enabled":true}',
       '{"name":"修改","url":"http://127.0.0.1:8090/BasicData/Employee/addOrUpdateUser","method":"POST","enabled":true}',
       '{"name":"删除","url":"http://127.0.0.1:8090/BasicData/Employee/deleteEmployeeInfo","method":"POST","enabled":true}',
       NULL,
       '删除将同时删除该用户在 Camstar 的域账号，不可恢复！', 0
FROM "sub_system" cam
WHERE cam."system_name" = 'Camstar人员管理'
  AND cam."oauth2_client_id" IS NULL
  AND cam."deleted" = 0
  AND NOT EXISTS (
      SELECT 1 FROM "sub_system_api_config" c
      WHERE c."sub_system_id" = cam."id" AND c."deleted" = 0
  );

-- 已有库升级：增加目录树列（幂等）
ALTER TABLE "sub_system_api_config" ADD COLUMN IF NOT EXISTS "api_catalog" text;
COMMENT ON COLUMN "sub_system_api_config"."api_catalog" IS '接口目录树 JSON：目录+叶子；叶子 purpose=auth/query/create/update/delete；用户同步调 create';

-- ----------------------------------------------------------------------------
-- 4. 菜单（parent_id 需按现场"外部系统"目录实际编号调整，此处示例用变量说明）
-- type: 1目录 2菜单 3按钮；status: 0正常
-- ----------------------------------------------------------------------------
-- 4.1 车间对照（单表：JUMP 部门 ↔ 业务系统车间；不要左侧「选系统」）
-- INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater)
-- VALUES ('车间对照', '', 2, 7, :parent_external_system, 'workshop', 'ep:office-building', 'system/subSystem/workshop/index', 'SubSystemWorkshop', 0, '1', '1', '1', '1', '1');
-- 按钮：sub-system:workshop:list / create / update / delete
--
-- 4.2 人员接口接入（菜单式：目录 + 叶子接口）
-- ('人员接口接入', '', 2, 8, :parent_external_system, 'apiConfig', 'ep:set-up', 'system/subSystem/apiConfig/index', 'SubSystemApiConfig', ...)
--   sub-system:apiconfig:list / create / update / delete / test
--
-- 4.3 已删除前端页 system/subSystem/employee/index —— 现场请删菜单：
-- DELETE FROM system_menu WHERE component = 'system/subSystem/employee/index' AND deleted = 0;
