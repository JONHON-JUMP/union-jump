-- 刷卡卡号、ERP 号都挂在业务账号上，允许多条业务账号使用同一个卡号或同一个 ERP 号。
-- 不建唯一索引。若之前建过 uk_sub_system_users_card_no_alive，这里删掉。
-- 主用户上的卡号不再维护。业务账号卡号为空时，用主用户卡号回填，多条业务账号都回填同一张卡。

DROP INDEX IF EXISTS "uk_sub_system_users_card_no_alive";

UPDATE "sub_system_users" r
SET "card_no"     = btrim(u."card_no"),
    "updater"     = 'upgrade_20261111_card_no_backfill',
    "update_time" = CURRENT_TIMESTAMP
FROM "system_users" u
WHERE r."main_user_id" = u."id"
  AND r."deleted" = 0
  AND u."deleted" = 0
  AND (r."card_no" IS NULL OR btrim(r."card_no") = '')
  AND u."card_no" IS NOT NULL
  AND btrim(u."card_no") <> '';
