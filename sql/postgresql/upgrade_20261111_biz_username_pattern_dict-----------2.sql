-- 挂接业务用户时，用户名下拉的生成方式。
-- value 里 {username} 是 JUMP 用户名，{workshop} 是所选车间编号。
-- 以后要加新写法，只在这个字典里加一条，不要改页面。
-- 注意：生产库字典表沿用原框架建法，id 无自增默认值且 NOT NULL，必须显式给 id（取当前 MAX+1）；
-- deleted_time 部分库为 NOT NULL，也不能插 NULL。

INSERT INTO "system_dict_type" ("id", "name", "type", "status", "remark", "creator", "create_time", "updater", "update_time", "deleted", "deleted_time")
SELECT (SELECT COALESCE(MAX(t."id"), 0) + 1 FROM "system_dict_type" t),
       '业务用户名生成方式', 'sub_system_biz_username_pattern', 0,
       '挂接业务用户名。{username}=JUMP用户名，{workshop}=车间编号',
       '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, '1970-01-01 00:00:00'::timestamp
WHERE NOT EXISTS (
    SELECT 1 FROM "system_dict_type"
    WHERE "type" = 'sub_system_biz_username_pattern' AND "deleted" = 0
);

INSERT INTO "system_dict_data" ("id", "sort", "label", "value", "dict_type", "status", "color_type", "css_class", "remark", "creator", "create_time", "updater", "update_time", "deleted")
SELECT (SELECT COALESCE(MAX(d."id"), 0) + 1 FROM "system_dict_data" d),
       1, 'JUMP用户名', '{username}', 'sub_system_biz_username_pattern', 0, '', '', '例如 77176', '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
    SELECT 1 FROM "system_dict_data"
    WHERE "dict_type" = 'sub_system_biz_username_pattern' AND "value" = '{username}' AND "deleted" = 0
);

INSERT INTO "system_dict_data" ("id", "sort", "label", "value", "dict_type", "status", "color_type", "css_class", "remark", "creator", "create_time", "updater", "update_time", "deleted")
SELECT (SELECT COALESCE(MAX(d."id"), 0) + 1 FROM "system_dict_data" d),
       2, '车间编号_工号', '{workshop}_{username}', 'sub_system_biz_username_pattern', 0, '', '', '例如 4306_77176', '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
    SELECT 1 FROM "system_dict_data"
    WHERE "dict_type" = 'sub_system_biz_username_pattern' AND "value" = '{workshop}_{username}' AND "deleted" = 0
);

INSERT INTO "system_dict_data" ("id", "sort", "label", "value", "dict_type", "status", "color_type", "css_class", "remark", "creator", "create_time", "updater", "update_time", "deleted")
SELECT (SELECT COALESCE(MAX(d."id"), 0) + 1 FROM "system_dict_data" d),
       3, '工号车间编号', '{username}{workshop}', 'sub_system_biz_username_pattern', 0, '', '', '例如 771764306', '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
    SELECT 1 FROM "system_dict_data"
    WHERE "dict_type" = 'sub_system_biz_username_pattern' AND "value" = '{username}{workshop}' AND "deleted" = 0
);
