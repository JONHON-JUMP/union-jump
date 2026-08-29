-- ============================================================================
-- 本次更新：用户新增联动登记 + 人员接口手动注册（PostgreSQL / JUMP 主库）
-- 日期：2026-08-29
-- 内容：
--   1) 花名册表加「人员接口注册状态」列
--   2) Camstar 人员接口占位地址修正为真实地址 192.168.240.125:8888
--   3) 「Camstar人员管理」若尚无接口配置，插入默认配置（真实地址）
-- 全部幂等，可重复执行。
-- 前置：sub_system_users / sub_system_api_config 表已存在
--      （若未执行过 sub_system_workshop_and_api.sql，请先执行该脚本建表）。
-- ============================================================================

-- 1. 花名册「人员接口注册状态」（幂等）：0未注册 1已注册
--    用户新增联动登记 / 手动建花名册默认 0；调「新增人员」接口成功自动置 1；
--    外部用户管理页可手动修改（人工在对方系统建过人 → 标 1 避免重复推送；改回 0 可重推）
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "employee_registered" varchar(2) NOT NULL DEFAULT '0';
COMMENT ON COLUMN "sub_system_users"."employee_registered" IS '子系统人员接口注册状态（0未注册 1已注册；调新增人员接口成功自动置1，页面可改）';

-- 2. Camstar 人员接口地址修正（幂等）：占位 127.0.0.1:8090 → 真实 192.168.240.125:8888
--    只替换仍为占位地址的配置；现场已改成其它真实地址的不受影响
UPDATE "sub_system_api_config"
SET "base_url"    = replace("base_url", '127.0.0.1:8090', '192.168.240.125:8888'),
    "auth_config" = replace("auth_config", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_query"   = replace("api_query", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_create"  = replace("api_create", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_update"  = replace("api_update", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_delete"  = replace("api_delete", '127.0.0.1:8090', '192.168.240.125:8888'),
    "api_team_combo" = replace("api_team_combo", '127.0.0.1:8090', '192.168.240.125:8888'),
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND (
        "base_url"    LIKE '%127.0.0.1:8090%'
     OR "auth_config" LIKE '%127.0.0.1:8090%'
     OR "api_query"   LIKE '%127.0.0.1:8090%'
     OR "api_create"  LIKE '%127.0.0.1:8090%'
     OR "api_update"  LIKE '%127.0.0.1:8090%'
     OR "api_delete"  LIKE '%127.0.0.1:8090%'
     OR "api_team_combo" LIKE '%127.0.0.1:8090%'
  );

-- 3. 若「Camstar人员管理」尚无接口配置：插入默认配置（真实地址；幂等）
CREATE SEQUENCE IF NOT EXISTS sub_system_api_config_seq;
SELECT setval('sub_system_api_config_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM "sub_system_api_config"), 1));

INSERT INTO "sub_system_api_config"
    ("id", "sub_system_id", "api_type", "base_url", "auth_type", "auth_config", "api_query", "api_create", "api_update", "api_delete", "api_team_combo", "delete_tip", "status")
SELECT nextval('sub_system_api_config_seq'), cam."id", 'camstar', 'http://192.168.240.125:8888', 'cookie_sso',
       '{"name":"SSO登录","url":"http://192.168.240.125:8888/Base/SSOLogin/SSOLoginIn","method":"GET","enabled":true,"userCode":"","cookieName":"Nancal_Cam_SessionId"}',
       '{"name":"查询","url":"http://192.168.240.125:8888/BasicData/Employee/getEmployeeInfo","method":"POST","enabled":true}',
       '{"name":"新增","url":"http://192.168.240.125:8888/BasicData/Employee/addOrUpdateUser","method":"POST","enabled":true}',
       '{"name":"修改","url":"http://192.168.240.125:8888/BasicData/Employee/addOrUpdateUser","method":"POST","enabled":true}',
       '{"name":"删除","url":"http://192.168.240.125:8888/BasicData/Employee/deleteEmployeeInfo","method":"POST","enabled":true}',
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
