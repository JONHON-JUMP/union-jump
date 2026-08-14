-- 子系统用户花名册：本地身份字段 + main_user_id 可空
-- PostgreSQL

ALTER TABLE sub_system_users ADD COLUMN IF NOT EXISTS username varchar(64);
COMMENT ON COLUMN sub_system_users.username IS '子系统登录用户名';

ALTER TABLE sub_system_users ADD COLUMN IF NOT EXISTS nickname varchar(64);
COMMENT ON COLUMN sub_system_users.nickname IS '用户姓名';

-- 从主用户回填历史数据（含软删行，避免 SET NOT NULL 失败）
UPDATE sub_system_users ssu
SET
    username = COALESCE(NULLIF(ssu.username, ''), su.username),
    nickname = COALESCE(NULLIF(ssu.nickname, ''), su.nickname)
FROM system_users su
WHERE ssu.main_user_id = su.id
  AND (ssu.username IS NULL OR ssu.username = '');

-- 仍无用户名的（含软删），用占位避免非空约束失败
UPDATE sub_system_users
SET username = 'legacy_' || id
WHERE username IS NULL OR username = '';

ALTER TABLE sub_system_users ALTER COLUMN username SET NOT NULL;

-- main_user_id 改为可空
ALTER TABLE sub_system_users ALTER COLUMN main_user_id DROP NOT NULL;

-- 若曾加过多余字段，一并清理
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS employee_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS card_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS domain_no;
ALTER TABLE sub_system_users DROP COLUMN IF EXISTS erp_nos;

-- 唯一：同系统用户名
DROP INDEX IF EXISTS uk_sub_system_users_username;
CREATE UNIQUE INDEX uk_sub_system_users_username
    ON sub_system_users (sub_system_id, username)
    WHERE deleted = 0;

-- 唯一：同系统已挂主用户（仅非空）
DROP INDEX IF EXISTS uk_sub_system_users_main_user;
CREATE UNIQUE INDEX uk_sub_system_users_main_user
    ON sub_system_users (sub_system_id, main_user_id)
    WHERE deleted = 0 AND main_user_id IS NOT NULL;
