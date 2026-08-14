-- 角色头像配置后台菜单（PostgreSQL）
-- 说明：若 5060 已被其他菜单占用，请改用 5075-5079 段（见下方备选脚本）
-- 当前 mes_db 已使用 5060-5064 作为「角色头像」菜单

-- ========== 方案 A：5060 未被占用时使用 ==========
-- INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
-- VALUES (5060, '角色头像', 'system:role-avatar:query', 2, 26, 1, 'role-avatar', 'user', 'system/roleAvatar/index', 'SystemRoleAvatar', 0, true, true, true, '1', '1', 0)
-- ON CONFLICT (id) DO NOTHING;

-- ========== 方案 B：5060 已被占用时使用（5075-5079） ==========
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT 5075, '角色头像', 'system:role-avatar:query', 2, 26, 1, 'role-avatar', 'user', 'system/roleAvatar/index', 'SystemRoleAvatar', 0, true, true, true, '1', '1', 0
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'system:role-avatar:query' AND type = 2 AND deleted = 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, status, visible, keep_alive, always_show, creator, updater, deleted)
SELECT v.id, v.name, v.permission, 3, v.sort, 5075, '', '', 0, true, true, true, '1', '1', 0
FROM (VALUES
    (5076, '角色头像查询', 'system:role-avatar:query', 1),
    (5077, '角色头像新增', 'system:role-avatar:create', 2),
    (5078, '角色头像修改', 'system:role-avatar:update', 3),
    (5079, '角色头像删除', 'system:role-avatar:delete', 4)
) AS v(id, name, permission, sort)
WHERE EXISTS (SELECT 1 FROM system_menu WHERE id = 5075 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM system_menu m WHERE m.permission = v.permission AND m.type = 3 AND m.parent_id = 5075 AND m.deleted = 0)
ON CONFLICT (id) DO NOTHING;

-- 超级管理员授权（自动匹配已存在的角色头像菜单 id）
INSERT INTO system_role_menu (id, role_id, menu_id, creator, updater, deleted, tenant_id)
SELECT base.max_id + ROW_NUMBER() OVER (ORDER BY m.id), 1, m.id, '1', '1', 0, 1
FROM system_menu m
CROSS JOIN (SELECT COALESCE(MAX(id), 0) AS max_id FROM system_role_menu) base
WHERE m.permission LIKE 'system:role-avatar%'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = 0
  );
