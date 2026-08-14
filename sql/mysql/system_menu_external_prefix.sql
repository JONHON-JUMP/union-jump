-- 主系统「外部系统」目录下菜单名称统一加「外部」前缀（MySQL）
-- 范围：component 为 system/subSystem/* 的目录/菜单，或其父级为「外部系统」的菜单
UPDATE system_menu m
SET m.name = CONCAT('外部', m.name),
    m.updater = 'system',
    m.update_time = NOW()
WHERE m.deleted = b'0'
  AND m.type IN (1, 2)
  AND m.name <> '外部系统'
  AND m.name NOT LIKE '外部%'
  AND (
    m.component LIKE 'system/subSystem/%'
    OR m.parent_id IN (
      SELECT p.id
      FROM (
        SELECT id FROM system_menu
        WHERE name = '外部系统' AND parent_id = 0 AND deleted = b'0'
      ) p
    )
  );
