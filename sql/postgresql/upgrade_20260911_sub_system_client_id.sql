-- ============================================================================
-- 业务系统编号落到 sub_system.client_id，登记不再依赖 system_oauth2_client
-- 日期：2026-09-11
-- 幂等，可重复执行。
-- ============================================================================

ALTER TABLE "sub_system" ALTER COLUMN "oauth2_client_id" DROP NOT NULL;

ALTER TABLE "sub_system" ADD COLUMN IF NOT EXISTS "client_id" varchar(64);
COMMENT ON COLUMN "sub_system"."client_id" IS '业务系统编号，门户路由 /portal/{clientId}；不依赖 OAuth2';

-- 已有门户系统：从原 OAuth2 客户端回填编号
UPDATE "sub_system" ss
SET "client_id" = oc."client_id"
FROM "system_oauth2_client" oc
WHERE ss."oauth2_client_id" = oc."id"
  AND ss."deleted" = 0
  AND (ss."client_id" IS NULL OR ss."client_id" = '');

CREATE UNIQUE INDEX IF NOT EXISTS uk_sub_system_client_id
    ON "sub_system" ("client_id")
    WHERE "deleted" = 0 AND "client_id" IS NOT NULL AND "client_id" <> '';
