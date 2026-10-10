-- ============================================================================
-- 本次更新：侧栏菜单名称「外部系统 / 子系统」统一为「业务系统」
-- 日期：2026-09-10
-- 幂等，可重复执行。
-- ============================================================================

UPDATE "system_menu"
SET "name" = replace("name", '外部系统', '业务系统'),
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND "name" LIKE '%外部系统%';

UPDATE "system_menu"
SET "name" = replace("name", '外部用户', '业务系统用户'),
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND "name" LIKE '%外部用户%';

UPDATE "system_menu"
SET "name" = replace("name", '子系统', '业务系统'),
    "update_time" = CURRENT_TIMESTAMP
WHERE "deleted" = 0
  AND "name" LIKE '%子系统%';
