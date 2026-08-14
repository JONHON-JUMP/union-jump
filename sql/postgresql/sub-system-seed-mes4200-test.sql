-- =============================================================================
-- JUMP 主库：老 MES（4200）子系统测试数据（PostgreSQL）
-- 用途：门户顶栏展示、菜单/角色/权限联调；可配合 Mock 子系统，无需真 Oracle
-- 说明：幂等脚本（先删后插固定 ID）。执行前请确认 ID 不与生产业务冲突。
-- =============================================================================

-- 固定 ID
-- oauth2_client.id = 45, client_id = mes4200
-- sub_system.id = 3
-- sub_system_role.id = 201
-- sub_system_menu.id = 42001 ~ 42010

BEGIN;

DELETE FROM sub_system_role_menu WHERE role_id = 201 OR menu_id BETWEEN 42001 AND 42010;
DELETE FROM sub_system_user_role WHERE role_id = 201
   OR user_id IN (SELECT id FROM sub_system_users WHERE sub_system_id = 3);
DELETE FROM sub_system_users WHERE sub_system_id = 3;
DELETE FROM sub_system_menu WHERE sub_system_id = 3 OR id BETWEEN 42001 AND 42010;
DELETE FROM sub_system_role WHERE id = 201 OR sub_system_id = 3;
DELETE FROM sub_system WHERE id = 3;
DELETE FROM system_oauth2_client WHERE id = 45 OR client_id = 'mes4200';

INSERT INTO system_oauth2_client (
    id, client_id, secret, name, logo, description, status,
    access_token_validity_seconds, refresh_token_validity_seconds,
    redirect_uris, authorized_grant_types, scopes, auto_approve_scopes,
    authorities, resource_ids, additional_information,
    creator, create_time, updater, update_time, deleted
) VALUES (
    45, 'mes4200', 'Mes4200@Test', '老MES（4200）', '',
    'Camstar 老 MES 测试接入（Mock）', 0,
    1800, 43200,
    '["http://10.17.65.11:8081/mes4200/sso/callback"]',
    '["authorization_code","refresh_token"]',
    '["user.read"]', '["user.read"]',
    '[]', '[]', '{}',
    '1', NOW(), '1', NOW(), 0
);

INSERT INTO sub_system (
    id, oauth2_client_id, system_name, description, system_url, system_icon,
    status, creator, create_time, updater, update_time, deleted
) VALUES (
    3, 45, '老MES（4200）',
    '厂内老 MES / Camstar，用于 JUMP 统一门户与权限联调测试',
    'http://10.17.65.11:8081/mes4200', '',
    0, '1', NOW(), '1', NOW(), 0
);

INSERT INTO sub_system_menu (
    id, sub_system_id, menu_name, parent_id, order_num, path, component, query,
    is_cache, is_frame, type, visible, status, perms, icon, component_name, always_show,
    remark, creator, create_time, updater, update_time, deleted
) VALUES
(42001, 3, '生产管理', 0, 1, 'production', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'guide', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42002, 3, '工单管理', 42001, 1, 'workorder', 'production/workorder/index', NULL, 0, 1, 'C', 0, 0, 'mes4200:workorder:list', 'list', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42003, 3, '工序报工', 42001, 2, 'report', 'production/report/index', NULL, 0, 1, 'C', 0, 0, 'mes4200:report:list', 'edit', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42004, 3, '质量管理', 0, 2, 'quality', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'example', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42005, 3, '质检记录', 42004, 1, 'inspect', 'quality/inspect/index', NULL, 0, 1, 'C', 0, 0, 'mes4200:inspect:list', 'documentation', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42006, 3, '系统管理', 0, 9, 'system', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'system', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42007, 3, '用户管理', 42006, 1, 'user', 'system/user/index', NULL, 0, 1, 'C', 0, 0, 'mes4200:user:list', 'user', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42008, 3, '工单查询', 42002, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'mes4200:workorder:query', '#', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42009, 3, '工单新增', 42002, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'mes4200:workorder:add', '#', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0),
(42010, 3, '工单修改', 42002, 3, '', NULL, NULL, 0, 1, 'F', 0, 0, 'mes4200:workorder:edit', '#', NULL, 0, '4200测试菜单', '1', NOW(), '1', NOW(), 0);

INSERT INTO sub_system_role (
    id, sub_system_id, name, code, sort, data_scope, data_scope_dept_ids,
    menu_check_strictly, dept_check_strictly, status, type,
    creator, create_time, updater, update_time, deleted
) VALUES (
    201, 3, '4200管理员', 'mes4200_admin', 1, 1, '[]',
    1, 1, 0, 1,
    '1', NOW(), '1', NOW(), 0
);

-- role_menu.id 使用较大偏移，避免与现网冲突；若冲突请按库内 MAX(id)+1 调整
INSERT INTO sub_system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 600000 + row_number() OVER (ORDER BY id), 201, id, '1', NOW(), '1', NOW(), 0
FROM sub_system_menu WHERE sub_system_id = 3 AND deleted = 0;

INSERT INTO sub_system_users (
    id, main_user_id, sub_system_id, workshop_id, team_id, home_menu_id,
    status, remark, creator, create_time, updater, update_time, deleted
) VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM sub_system_users),
    1, 3, 'WS4200', 'TEAM01', 42002,
    '0', '门户admin访问老MES4200测试', '1', NOW(), '1', NOW(), 0
);

INSERT INTO sub_system_user_role (id, user_id, role_id, creator, create_time, updater, update_time, deleted)
VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM sub_system_user_role),
    (SELECT id FROM sub_system_users WHERE sub_system_id = 3 AND main_user_id = 1 AND deleted = 0 LIMIT 1),
    201, '1', NOW(), '1', NOW(), 0
);

SELECT setval('system_oauth2_client_seq', GREATEST((SELECT MAX(id) FROM system_oauth2_client), 1));
SELECT setval('sub_system_seq', GREATEST((SELECT MAX(id) FROM sub_system), 1));
SELECT setval('sub_system_menu_seq', GREATEST((SELECT MAX(id) FROM sub_system_menu), 1));
SELECT setval('sub_system_role_seq', GREATEST((SELECT MAX(id) FROM sub_system_role), 1));
SELECT setval('sub_system_users_seq', GREATEST((SELECT MAX(id) FROM sub_system_users), 1));
SELECT setval('sub_system_role_menu_seq', GREATEST((SELECT MAX(id) FROM sub_system_role_menu), 1));
SELECT setval('sub_system_user_role_seq', GREATEST((SELECT MAX(id) FROM sub_system_user_role), 1));

COMMIT;

-- 写入后建议：重启主系统后端，或清理 Redis：DEL oauth_client::mes4200
-- 登录门户 admin，顶栏应出现「老MES（4200）」
