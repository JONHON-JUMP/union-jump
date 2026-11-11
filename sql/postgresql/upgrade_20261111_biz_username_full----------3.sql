-- 业务用户名改为业务系统里的真实账号。
-- 「关联车间编号」=1 且用户名还是工号的，改成 车间编号_工号，并把标记清成 0。
-- 之后 Cookie、注册、同步都直接用 sub_system_users.username，不再按标记拼接。
-- 目标用户名在同一叶子下已经存在的行会跳过，避免撞 uk_sub_system_users_leaf_username。
-- 用户名已经是「车间编号_」开头的，只清标记。

UPDATE "sub_system_users" u
SET "username_with_workshop" = '0',
    "updater" = 'upgrade_20261111_full_biz_username',
    "update_time" = CURRENT_TIMESTAMP
WHERE u."deleted" = 0
  AND u."username_with_workshop" = '1'
  AND u."workshop_id" IS NOT NULL
  AND btrim(u."workshop_id") <> ''
  AND u."username" LIKE btrim(u."workshop_id") || '\_%' ESCAPE '\';

UPDATE "sub_system_users" u
SET "username" = btrim(u."workshop_id") || '_' || u."username",
    "username_with_workshop" = '0',
    "updater" = 'upgrade_20261111_full_biz_username',
    "update_time" = CURRENT_TIMESTAMP
WHERE u."deleted" = 0
  AND u."username_with_workshop" = '1'
  AND u."workshop_id" IS NOT NULL
  AND btrim(u."workshop_id") <> ''
  AND u."username" IS NOT NULL
  AND btrim(u."username") <> ''
  AND u."username" NOT LIKE btrim(u."workshop_id") || '\_%' ESCAPE '\'
  AND char_length(btrim(u."workshop_id") || '_' || u."username") <= 64
  AND NOT EXISTS (
        SELECT 1
        FROM "sub_system_users" x
        WHERE x."deleted" = 0
          AND x."sub_system_id" = u."sub_system_id"
          AND x."workshop_id" IS NOT DISTINCT FROM u."workshop_id"
          AND x."username" = btrim(u."workshop_id") || '_' || u."username"
          AND x."id" <> u."id"
  );
