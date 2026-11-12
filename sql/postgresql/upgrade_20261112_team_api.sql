-- 给已经配置了角色查询的系统，补班组查询、班组新增、班组修改、角色修改。
-- 不写叶子绑定。sort 不能为空。地址只换路径，保留原接口的主机和端口。

INSERT INTO sub_system_api_endpoint (
    sub_system_id, purpose, group_code, name, url, method,
    enabled, with_session, sort,
    creator, create_time, updater, update_time, deleted
)
SELECT
    src.sub_system_id,
    spec.purpose,
    spec.group_code,
    spec.name,
    CASE
        WHEN btrim(src.url) ~* '^https?://'
            THEN regexp_replace(btrim(src.url), '^(https?://[^/]+).*$', '\1') || spec.path
        ELSE spec.path
    END,
    'POST',
    1,
    1,
    spec.sort,
    'upgrade',
    CURRENT_TIMESTAMP,
    'upgrade',
    CURRENT_TIMESTAMP,
    0
FROM (
    SELECT DISTINCT ON (sub_system_id)
           sub_system_id, url
    FROM sub_system_api_endpoint
    WHERE deleted = 0
      AND purpose = 'role_query'
      AND url IS NOT NULL
      AND btrim(url) <> ''
    ORDER BY sub_system_id, id
) src
CROSS JOIN (
    VALUES
        ('team_query',  'team', '查询', '/BasicData/Team/getTeamInfo', 1),
        ('team_create', 'team', '新增', '/BasicData/Team/addOrUpdateTeam', 2),
        ('team_update', 'team', '修改', '/BasicData/Team/addOrUpdateTeam', 3),
        ('role_update', 'role', '修改', '/BasicData/Role/updateRoleInfo', 3)
) AS spec(purpose, group_code, name, path, sort)
WHERE NOT EXISTS (
    SELECT 1
    FROM sub_system_api_endpoint e
    WHERE e.deleted = 0
      AND e.sub_system_id = src.sub_system_id
      AND e.purpose = spec.purpose
);
