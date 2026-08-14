-- =============================================================================
-- JUMP 主库：智能柜（smartCabinet）子系统完整导入（PostgreSQL）
-- 数据来源：柜库 ruoyi-vue-pro-cabinet 业务菜单树 + 标准按钮权限
-- 对齐：client-id=cabinet, sub-system-id=2, secret=Cabinet@2026
-- 幂等：先删后插固定 ID
-- =============================================================================
-- oauth2_client.id = 46
-- sub_system.id = 2
-- sub_system_role.id = 202
-- sub_system_menu.id = 20001 ~ 20040
-- =============================================================================

BEGIN;

DELETE FROM sub_system_role_menu WHERE role_id = 202 OR menu_id BETWEEN 20001 AND 20040;
DELETE FROM sub_system_user_role WHERE role_id = 202
   OR user_id IN (SELECT id FROM sub_system_users WHERE sub_system_id = 2);
DELETE FROM sub_system_users WHERE sub_system_id = 2;
DELETE FROM sub_system_menu WHERE sub_system_id = 2 OR id BETWEEN 20001 AND 20040;
DELETE FROM sub_system_role WHERE id = 202 OR sub_system_id = 2;
DELETE FROM sub_system WHERE id = 2;
DELETE FROM system_oauth2_client WHERE id = 46 OR client_id = 'cabinet';

INSERT INTO system_oauth2_client (
    id, client_id, secret, name, logo, description, status,
    access_token_validity_seconds, refresh_token_validity_seconds,
    redirect_uris, authorized_grant_types, scopes, auto_approve_scopes,
    authorities, resource_ids, additional_information,
    creator, create_time, updater, update_time, deleted
) VALUES (
    46, 'cabinet', 'Cabinet@2026', '智能柜', '',
    'smartCabinet 门户接入', 0,
    1800, 43200,
    '["http://127.0.0.1:8081/cabinet/sso/callback","http://10.17.65.11:8081/cabinet/sso/callback"]',
    '["authorization_code","refresh_token"]',
    '["user.read"]', '["user.read"]',
    '[]', '[]', '{}',
    '1', NOW(), '1', NOW(), 0
);

INSERT INTO sub_system (
    id, oauth2_client_id, system_name, description, system_url, system_icon,
    status, creator, create_time, updater, update_time, deleted
) VALUES (
    2, 46, '智能柜',
    '智能柜管理系统',
    'http://10.17.65.11:8081/cabinet', '',
    0, '1', NOW(), '1', NOW(), 0
);

-- type: M目录 / C菜单 / F按钮；visible/is_cache: 0=显示/缓存（与门户极性一致）
INSERT INTO sub_system_menu (
    id, sub_system_id, menu_name, parent_id, order_num, path, component, query,
    is_cache, is_frame, type, visible, status, perms, icon, component_name, always_show,
    remark, creator, create_time, updater, update_time, deleted
) VALUES
-- 目录/菜单
(20001, 2, '基础信息', 0, 1, 'basis', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'ep:collection', NULL, 1, 'cabinet#6735', '1', NOW(), '1', NOW(), 0),
(20002, 2, '智能柜管理', 20001, 10, 'cabinetInfo', 'cabinet/info/index', NULL, 0, 1, 'C', 0, 0, 'cabinet:info:query', 'ep:box', 'CabinetInfo', 1, 'cabinet#6737', '1', NOW(), '1', NOW(), 0),
(20003, 2, '柜门管理', 20001, 20, 'doorInfo', 'cabinet/door/index', NULL, 0, 1, 'C', 0, 0, 'cabinet:door:query', 'ep:grid', 'DoorInfo', 1, 'cabinet#6738', '1', NOW(), '1', NOW(), 0),
(20004, 2, '任务管理', 0, 2, 'task', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'ep:list', NULL, 1, 'cabinet#6736', '1', NOW(), '1', NOW(), 0),
(20005, 2, '任务列表', 20004, 10, 'taskInfo', 'cabinet/task/index', NULL, 0, 1, 'C', 0, 0, 'cabinet:task:query', 'ep:document', 'TaskInfo', 1, 'cabinet#6739', '1', NOW(), '1', NOW(), 0),
(20006, 2, '任务执行记录', 20004, 20, 'taskExecutionLog', 'cabinet/taskexecutionrecord/index', NULL, 0, 1, 'C', 0, 0, 'cabinet:task-execution-record:query', 'ep:tickets', 'TaskExecutionLog', 1, 'cabinet#6740', '1', NOW(), '1', NOW(), 0),
(20007, 2, '运维管理', 0, 3, 'maintenance', NULL, NULL, 0, 1, 'M', 0, 0, NULL, 'ep:setting', NULL, 1, 'cabinet#6745', '1', NOW(), '1', NOW(), 0),
(20008, 2, '运维记录', 20007, 1, 'maintenanceRecord', 'cabinet/maintenancerecord/index', NULL, 0, 1, 'C', 0, 0, 'cabinet:maintenance-record:query', 'ep:notebook', 'MaintenanceRecord', 1, 'cabinet#6746', '1', NOW(), '1', NOW(), 0),
(20009, 2, '智能柜界面', 0, 9, 'cabinet', NULL, NULL, 0, 1, 'M', 1, 0, NULL, 'ep:monitor', NULL, 1, '柜端屏幕默认隐藏', '1', NOW(), '1', NOW(), 0),
(20010, 2, '智能柜主页', 20009, 1, 'cabinetMain', 'cabinetScreen/index', NULL, 0, 1, 'C', 1, 0, NULL, 'ep:home-filled', 'CabinetMain', 1, '柜端屏幕默认隐藏', '1', NOW(), '1', NOW(), 0),

-- 按钮权限（门户角色授权可见；柜端 Controller 当前未强校验，供主系统权限管理）
(20011, 2, '智能柜查询', 20002, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:info:query', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20012, 2, '智能柜新增', 20002, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:info:create', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20013, 2, '智能柜修改', 20002, 3, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:info:update', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20014, 2, '智能柜删除', 20002, 4, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:info:delete', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20015, 2, '智能柜导出', 20002, 5, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:info:export', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),

(20016, 2, '柜门查询', 20003, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:door:query', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20017, 2, '柜门新增', 20003, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:door:create', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20018, 2, '柜门修改', 20003, 3, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:door:update', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20019, 2, '柜门删除', 20003, 4, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:door:delete', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20020, 2, '柜门导出', 20003, 5, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:door:export', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),

(20021, 2, '任务查询', 20005, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task:query', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20022, 2, '任务新增', 20005, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task:create', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20023, 2, '任务修改', 20005, 3, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task:update', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20024, 2, '任务删除', 20005, 4, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task:delete', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20025, 2, '任务导出', 20005, 5, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task:export', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),

(20026, 2, '执行记录查询', 20006, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task-execution-record:query', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20027, 2, '执行记录导出', 20006, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:task-execution-record:export', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),

(20028, 2, '运维记录查询', 20008, 1, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:maintenance-record:query', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20029, 2, '运维记录新增', 20008, 2, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:maintenance-record:create', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20030, 2, '运维记录修改', 20008, 3, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:maintenance-record:update', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20031, 2, '运维记录删除', 20008, 4, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:maintenance-record:delete', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0),
(20032, 2, '运维记录导出', 20008, 5, '', NULL, NULL, 0, 1, 'F', 0, 0, 'cabinet:maintenance-record:export', '#', NULL, 0, 'button', '1', NOW(), '1', NOW(), 0);

INSERT INTO sub_system_role (
    id, sub_system_id, name, code, sort, data_scope, data_scope_dept_ids,
    menu_check_strictly, dept_check_strictly, status, type,
    creator, create_time, updater, update_time, deleted
) VALUES (
    202, 2, '智能柜管理员', 'cabinet_admin', 1, 1, '[]',
    1, 1, 0, 1,
    '1', NOW(), '1', NOW(), 0
);

-- 管理端全部菜单+按钮（不含柜端屏幕 20009/20010）
INSERT INTO sub_system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 700000 + row_number() OVER (ORDER BY id), 202, id, '1', NOW(), '1', NOW(), 0
FROM sub_system_menu
WHERE sub_system_id = 2 AND deleted = 0 AND id BETWEEN 20001 AND 20032
  AND id NOT IN (20009, 20010);

INSERT INTO sub_system_users (
    id, main_user_id, sub_system_id, workshop_id, team_id, home_menu_id,
    status, remark, creator, create_time, updater, update_time, deleted
) VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM sub_system_users),
    1, 2, NULL, NULL, 20002,
    '0', '门户admin→智能柜', '1', NOW(), '1', NOW(), 0
);

INSERT INTO sub_system_user_role (id, user_id, role_id, creator, create_time, updater, update_time, deleted)
VALUES (
    (SELECT COALESCE(MAX(id), 0) + 1 FROM sub_system_user_role),
    (SELECT id FROM sub_system_users WHERE sub_system_id = 2 AND main_user_id = 1 AND deleted = 0 LIMIT 1),
    202, '1', NOW(), '1', NOW(), 0
);

SELECT setval('system_oauth2_client_seq', GREATEST((SELECT MAX(id) FROM system_oauth2_client), 1));
SELECT setval('sub_system_seq', GREATEST((SELECT MAX(id) FROM sub_system), 1));
SELECT setval('sub_system_menu_seq', GREATEST((SELECT MAX(id) FROM sub_system_menu), 1));
SELECT setval('sub_system_role_seq', GREATEST((SELECT MAX(id) FROM sub_system_role), 1));
SELECT setval('sub_system_users_seq', GREATEST((SELECT MAX(id) FROM sub_system_users), 1));
SELECT setval('sub_system_role_menu_seq', GREATEST((SELECT MAX(id) FROM sub_system_role_menu), 1));
SELECT setval('sub_system_user_role_seq', GREATEST((SELECT MAX(id) FROM sub_system_user_role), 1));

COMMIT;

-- 执行后务必清理 Redis（否则门户仍显示旧测试菜单）：
-- DEL portal_my_menus:1:2
-- DEL portal:perm:context:1:1:2
-- DEL sub_system_user_quick_nav:1:2
