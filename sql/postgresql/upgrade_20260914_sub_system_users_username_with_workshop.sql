-- ----------------------------------------------------------------------------
-- upgrade_20260914_sub_system_users_username_with_workshop.sql
-- 新增人员接口注册：用户名可选拼接车间编号
--   背景：对接系统（如 Camstar）统一管理多个 MES 的用户且用户名全局唯一，同一工号需以
--         车间编号_工号（如 4200_10086）注册才能登录不同车间 MES。
--   username_with_workshop：0 不拼接（默认，存量行为不变）；1 注册时用户名拼接车间编号，
--         此时该用户在对接系统的用户名与子系统访问身份均为 车间编号_工号。
--         调「新增人员」接口注册时按弹窗勾选写入，对任意类型（camstar/http）接口均可选。
--   registered_api_type：注册成功时记录所调接口的适配器类型（camstar/http）；
--         Camstar Cookie 身份预取只认注册到 camstar 的行，避免拼接注册到其它系统时
--         Cookie 用户名与 Camstar 侧账号不一致。
-- ----------------------------------------------------------------------------
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "username_with_workshop" varchar(2) NOT NULL DEFAULT '0';
ALTER TABLE "sub_system_users" ADD COLUMN IF NOT EXISTS "registered_api_type" varchar(16) DEFAULT NULL;

COMMENT ON COLUMN "sub_system_users"."username_with_workshop" IS '注册时用户名是否拼接车间编号（0否 1是；为1时对接系统用户名与子系统访问身份均为 车间编号_工号）';
COMMENT ON COLUMN "sub_system_users"."registered_api_type" IS '注册成功时所调「新增人员」接口的适配器类型（camstar/http；Camstar Cookie 身份预取只认 camstar）';
