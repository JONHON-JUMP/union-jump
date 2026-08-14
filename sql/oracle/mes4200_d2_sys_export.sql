-- =============================================================================
-- Oracle（MESDB1）导出：4200 用户 / 角色 / 菜单及关系
-- 库：MESDB1
-- 表：D2_SYS_MENU / D2_SYS_ROLE / D2_SYS_ROLE_MENU / D2_SYS_USER / D2_SYS_USER_ROLE
--
-- 用法（SQL*Plus / SQLcl 示例）：
--   SET COLSEP ','
--   SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING ON
--   SPOOL d2_sys_menu.csv
--   @mes4200_d2_sys_export.sql   -- 或分段执行下列 SELECT
--   SPOOL OFF
--
-- 导出 CSV 后，在 PostgreSQL 用 COPY 装入 staging（见 mes4200_d2_sys_import_to_sub_system.sql）
-- =============================================================================

-- ---------- 1) 菜单 D2_SYS_MENU ----------
SELECT
    menu_id,
    menu_name,
    NVL(parent_id, 0) AS parent_id,
    NVL(order_num, 0) AS order_num,
    path,
    component,
    query,
    NVL(is_frame, '1') AS is_frame,
    NVL(is_cache, '0') AS is_cache,
    menu_type,
    NVL(visible, '0') AS visible,
    NVL(status, '0') AS status,
    NVL(perms, '') AS perms,
    icon,
    remark,
    create_by,
    TO_CHAR(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time,
    update_by,
    TO_CHAR(update_time, 'YYYY-MM-DD HH24:MI:SS') AS update_time
FROM D2_SYS_MENU
ORDER BY parent_id, order_num, menu_id;

-- ---------- 2) 角色 D2_SYS_ROLE（未删除）----------
SELECT
    role_id,
    role_name,
    role_key,
    NVL(role_sort, 0) AS role_sort,
    NVL(data_scope, '1') AS data_scope,
    NVL(menu_check_strictly, 1) AS menu_check_strictly,
    NVL(dept_check_strictly, 1) AS dept_check_strictly,
    NVL(status, '0') AS status,
    NVL(del_flag, '0') AS del_flag,
    remark,
    create_by,
    TO_CHAR(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time,
    update_by,
    TO_CHAR(update_time, 'YYYY-MM-DD HH24:MI:SS') AS update_time
FROM D2_SYS_ROLE
WHERE NVL(del_flag, '0') = '0'
ORDER BY role_sort, role_id;

-- ---------- 3) 角色-菜单 D2_SYS_ROLE_MENU ----------
SELECT role_id, menu_id
FROM D2_SYS_ROLE_MENU
ORDER BY role_id, menu_id;

-- ---------- 4) 用户 D2_SYS_USER（未删除）----------
SELECT
    user_id,
    dept_id,
    user_name,
    nick_name,
    email,
    phonenumber,
    NVL(sex, '0') AS sex,
    avatar,
    NVL(status, '0') AS status,
    NVL(del_flag, '0') AS del_flag,
    remark,
    create_by,
    TO_CHAR(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time
FROM D2_SYS_USER
WHERE NVL(del_flag, '0') = '0'
ORDER BY user_id;

-- ---------- 5) 用户-角色 D2_SYS_USER_ROLE ----------
SELECT user_id, role_id
FROM D2_SYS_USER_ROLE
ORDER BY user_id, role_id;
