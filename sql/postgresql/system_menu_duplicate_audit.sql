-- 审计主系统 system_menu 中目录/菜单（type=1,2）是否存在重名
-- 按钮（type=3）允许不同父菜单下同名，不在此检查范围内

SELECT name,
       COUNT(*) AS duplicate_count,
       STRING_AGG(id::text, ',' ORDER BY id) AS menu_ids
FROM system_menu
WHERE deleted = 0
  AND type IN (1, 2)
GROUP BY name
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, name;

-- 查看重名菜单详情（将下方 IN 列表替换为上面查询得到的 id）
-- SELECT id, parent_id, name, type, path, component, permission
-- FROM system_menu
-- WHERE deleted = 0 AND id IN (102, 2119)
-- ORDER BY name, id;
