-- 去掉凭空加的「同步域账号/卡号/ERP号」。
-- 这条接口可能出现在：
--   1. sub_system_api_endpoint：用途 account_update，或地址含 /Employee/updateUserAccount，或名称就是「同步域账号/卡号/ERP号」
--   2. sub_system_api_binding：endpoint_id 还指着上面这些行
-- 域账号、刷卡卡号、ERP号只走已有「修改人员」(purpose=update，/BasicData/Employee/addOrUpdateUser)。
-- 先把绑定改到同一接口系统的修改人员，再软删多余接口行。没有修改人员接口的系统，绑定先不动，避免指空。

UPDATE "sub_system_api_binding" AS b
SET "endpoint_id" = upd."id",
    "updater" = 'upgrade',
    "update_time" = CURRENT_TIMESTAMP
FROM "sub_system_api_endpoint" AS fake
JOIN "sub_system_api_endpoint" AS upd
  ON upd."sub_system_id" = fake."sub_system_id"
 AND upd."purpose" = 'update'
 AND upd."deleted"::text IN ('0', 'f', 'false')
 AND (upd."enabled" IS NULL OR upd."enabled"::text IN ('1', 't', 'true'))
WHERE b."endpoint_id" = fake."id"
  AND b."deleted"::text IN ('0', 'f', 'false')
  AND fake."deleted"::text IN ('0', 'f', 'false')
  AND (
        fake."purpose" = 'account_update'
        OR COALESCE(fake."url", '') LIKE '%/Employee/updateUserAccount%'
        OR fake."name" = '同步域账号/卡号/ERP号'
      )
  AND b."endpoint_id" IS DISTINCT FROM upd."id";

-- 分配角色同步同样回写「修改人员」，没有单独的分配角色接口。
UPDATE "sub_system_api_binding" AS b
SET "endpoint_id" = upd."id",
    "updater" = 'upgrade',
    "update_time" = CURRENT_TIMESTAMP
FROM "sub_system_api_endpoint" AS upd
WHERE b."scene" = 'role_assign'
  AND b."deleted"::text IN ('0', 'f', 'false')
  AND upd."sub_system_id" = b."api_sub_system_id"
  AND upd."purpose" = 'update'
  AND upd."deleted"::text IN ('0', 'f', 'false')
  AND (upd."enabled" IS NULL OR upd."enabled"::text IN ('1', 't', 'true'))
  AND b."endpoint_id" IS DISTINCT FROM upd."id";

UPDATE "sub_system_api_endpoint"
SET "deleted" = 1,
    "updater" = 'upgrade',
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted"::text IN ('0', 'f', 'false')
  AND (
        "purpose" = 'account_update'
        OR COALESCE("url", '') LIKE '%/Employee/updateUserAccount%'
        OR "name" = '同步域账号/卡号/ERP号'
      );
