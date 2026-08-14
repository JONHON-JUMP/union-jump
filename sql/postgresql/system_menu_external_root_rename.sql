-- 主系统一级目录「外部系统管理」重命名为「外部系统」（PostgreSQL）
-- 仅改 parent_id = 0 的顶层目录，子菜单「外部系统管理」等名称不变，避免全局重名

UPDATE system_menu
SET name = '外部系统',
    updater = 'system',
    update_time = CURRENT_TIMESTAMP
WHERE deleted = 0
  AND parent_id = 0
  AND type IN (1, 2)
  AND name = '外部系统管理';
