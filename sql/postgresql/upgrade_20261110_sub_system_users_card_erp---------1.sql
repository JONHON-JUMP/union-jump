-- 业务账号增加刷卡卡号、ERP 号。这两项写在业务用户上，不写 JUMP 主用户。
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "card_no" varchar(64);
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "erp_no" varchar(64);
COMMENT ON COLUMN "sub_system_users"."card_no" IS '刷卡卡号，挂在这条业务账号上';
COMMENT ON COLUMN "sub_system_users"."erp_no" IS 'ERP号，一条业务账号一个';
