-- 主系统「外部系统」目录下菜单名称统一加「外部」前缀（PostgreSQL）
-- 范围：component 为 system/subSystem/* 的目录/菜单，或其父级为「外部系统」的菜单
UPDATE system_menu m
SET name = '外部' || m.name,
    updater = 'system',
    update_time = CURRENT_TIMESTAMP
WHERE m.deleted = 0
  AND m.type IN (1, 2)
  AND m.name <> '外部系统'
  AND m.name NOT LIKE '外部%'
  AND (
    m.component LIKE 'system/subSystem/%'
    OR m.parent_id IN (
      SELECT id FROM system_menu
      WHERE name = '外部系统' AND parent_id = 0 AND deleted = 0
    )
  );
