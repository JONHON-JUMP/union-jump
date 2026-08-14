-- 子系统菜单 path：192.168.240.127:4200 → 192.168.240.12794200
-- 避免 Vue Router 把 :4200 当成路由参数。
-- 默认只改 4200 子系统（sub_system.id = 3）；其它系统可改 WHERE。

BEGIN;

-- 预览（可选）
-- SELECT id, name, path
-- FROM sub_system_menu
-- WHERE deleted = 0
--   AND sub_system_id = 3
--   AND path LIKE '%:4200%';

UPDATE sub_system_menu
SET path = REPLACE(path, ':4200', '94200'),
    update_time = NOW()
WHERE deleted = 0
  AND sub_system_id = 3
  AND path LIKE '%:4200%';

-- 若 component 里也误存了带冒号的壳 path，一并改
UPDATE sub_system_menu
SET component = REPLACE(component, ':4200', '94200'),
    update_time = NOW()
WHERE deleted = 0
  AND sub_system_id = 3
  AND component LIKE '%:4200%';

COMMIT;
