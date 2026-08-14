-- 菜单样式管理后台菜单（PostgreSQL）
-- 挂到「系统管理」目录下；执行后需给目标角色授权 system_role_menu

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, updater, deleted)
VALUES (5050, '菜单样式', 'system:menu-style:query', 2, 25, 1, 'menu-style', 'theme', 'system/menuColor/index', 'SystemMenuStyle', 0, true, true, true, '1', '1', 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, status, visible, keep_alive, always_show, creator, updater, deleted) VALUES
(5051, '菜单样式查询', 'system:menu-style:query', 3, 1, 5050, '', '', 0, true, true, true, '1', '1', 0),
(5052, '菜单样式新增', 'system:menu-style:create', 3, 2, 5050, '', '', 0, true, true, true, '1', '1', 0),
(5053, '菜单样式修改', 'system:menu-style:update', 3, 3, 5050, '', '', 0, true, true, true, '1', '1', 0),
(5054, '菜单样式删除', 'system:menu-style:delete', 3, 4, 5050, '', '', 0, true, true, true, '1', '1', 0)
ON CONFLICT (id) DO NOTHING;
