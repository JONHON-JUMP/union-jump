-- ============================================================================
-- 一次性：把 Camstar 4200 角色的 ROLEID 写入 JUMP sub_system_role.external_role_id
-- 执行库：JUMP 主库（PostgreSQL）
-- 前置：已执行 upgrade_20261010_role_external_id.sql
--
-- Camstar 与 JUMP 不在同一个库，本脚本不能直接连 ROLEDEF。
-- 先在 Camstar（Oracle）导出 4200 角色，再把结果贴进下面的 INSERT：
--
--   SELECT ROLEID, ROLENAME
--     FROM ROLEDEF
--    WHERE NOTES = '4200'
--      AND ROLENAME IS NOT NULL
--    ORDER BY ROLENAME;
--
-- 匹配规则（只动门户系统 MES4200，不动「Camstar人员管理」这个接口目标）：
--   1) sub_system_role.name = ROLENAME                  例如都是 4200_班长
--   2) 否则 '4200_' || name = ROLENAME                  JUMP 短名「班长」对 Camstar「4200_班长」
-- 已有 external_role_id 的行不覆盖。对上的行 role_registered 置为已注册。
--
-- 用法：填好 INSERT → 整段执行 → 看「将写入 / 仍未匹配」→ 无误后把最后 ROLLBACK 改成 COMMIT。
-- ============================================================================

BEGIN;

CREATE TEMP TABLE camstar_role_4200 (
    role_name varchar(64) PRIMARY KEY,
    role_id   varchar(32) NOT NULL
) ON COMMIT DROP;

INSERT INTO camstar_role_4200 (role_name, role_id) VALUES
    -- ('4200_班长', '001bda8000000001')
    ('__PLACEHOLDER_DELETE_ME__', '0000000000000000');

DELETE FROM camstar_role_4200 WHERE role_name = '__PLACEHOLDER_DELETE_ME__';

-- 将写入：核对 name 与 role_id 是否一一对应
SELECT r."id",
       r."name" AS jump_role_name,
       COALESCE(exact.role_id, prefixed.role_id) AS camstar_role_id,
       CASE WHEN exact.role_id IS NOT NULL THEN '名称相同' ELSE '补了4200_前缀' END AS match_by
  FROM "sub_system_role" r
  JOIN "sub_system" s
    ON s."id" = r."sub_system_id"
   AND s."deleted" = 0
   AND s."system_name" = 'MES4200'
  LEFT JOIN camstar_role_4200 exact
    ON exact.role_name = r."name"
  LEFT JOIN camstar_role_4200 prefixed
    ON r."name" NOT LIKE '4200\_%' ESCAPE '\'
   AND prefixed.role_name = '4200_' || r."name"
 WHERE r."deleted" = 0
   AND (r."external_role_id" IS NULL OR btrim(r."external_role_id") = '')
   AND COALESCE(exact.role_id, prefixed.role_id) IS NOT NULL
 ORDER BY r."name";

UPDATE "sub_system_role" r
   SET "external_role_id" = m.role_id,
       "role_registered" = '1',
       "updater" = '1',
       "update_time" = CURRENT_TIMESTAMP
  FROM (
        SELECT r2."id",
               COALESCE(exact.role_id, prefixed.role_id) AS role_id
          FROM "sub_system_role" r2
          JOIN "sub_system" s
            ON s."id" = r2."sub_system_id"
           AND s."deleted" = 0
           AND s."system_name" = 'MES4200'
          LEFT JOIN camstar_role_4200 exact
            ON exact.role_name = r2."name"
          LEFT JOIN camstar_role_4200 prefixed
            ON r2."name" NOT LIKE '4200\_%' ESCAPE '\'
           AND prefixed.role_name = '4200_' || r2."name"
         WHERE r2."deleted" = 0
           AND (r2."external_role_id" IS NULL OR btrim(r2."external_role_id") = '')
           AND COALESCE(exact.role_id, prefixed.role_id) IS NOT NULL
       ) m
 WHERE r."id" = m."id";

-- 仍未匹配：这些角色分配时若勾选同步，会被拦住，需改名或在页面上「关联外部」
SELECT r."id", r."name", r."code"
  FROM "sub_system_role" r
  JOIN "sub_system" s
    ON s."id" = r."sub_system_id"
   AND s."deleted" = 0
   AND s."system_name" = 'MES4200'
 WHERE r."deleted" = 0
   AND (r."external_role_id" IS NULL OR btrim(r."external_role_id") = '')
 ORDER BY r."name";

-- 核对上面两段结果后，把下一行改成 COMMIT;
ROLLBACK;
