-- 角色系统头像初始数据（PostgreSQL）

-- 头像图片放在前端：jonhonjump-ui-admin-vue2/src/assets/images/avatar/

-- 数据库存储格式：static:文件名（不含扩展名），如 static:super_admin

--

-- 方式一（推荐）：在「系统管理 → 角色头像」页面手动新增三条配置，从静态库点选即可

--   super_admin  -> static:super_admin  sort=1

--   dept_leader  -> static:dept_leader  sort=2

--   common       -> static:common       sort=3

--

-- 方式二：直接 INSERT（需先确认 id 不冲突）

-- INSERT INTO system_role_avatar (id, role_code, avatar_url, sort, status, remark, creator, updater, deleted) VALUES

-- (1, 'super_admin', 'static:super_admin', 1, 0, '超级管理员默认头像', '1', '1', 0),

-- (2, 'dept_leader', 'static:dept_leader', 2, 0, '部门领导默认头像', '1', '1', 0),

-- (3, 'common',      'static:common',      3, 0, '普通角色默认头像', '1', '1', 0)

-- ON CONFLICT DO NOTHING;

--

-- 若库中仍是历史 http URL，可在配置页「修改」后重新点选静态头像保存，或执行：

-- UPDATE system_role_avatar SET avatar_url = 'static:super_admin' WHERE role_code = 'super_admin';

-- UPDATE system_role_avatar SET avatar_url = 'static:dept_leader' WHERE role_code = 'dept_leader';

-- UPDATE system_role_avatar SET avatar_url = 'static:common'      WHERE role_code = 'common';

