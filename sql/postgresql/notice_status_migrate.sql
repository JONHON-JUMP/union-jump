-- 通知状态：草稿(0) / 已发布(1) / 已删除(2)
-- 旧值迁移：开启(0)→已发布(1)；关闭(1)→草稿(0)

-- 1) 字典类型（id 可按库内实际最大值调整）
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted)
SELECT COALESCE((SELECT MAX(id) FROM system_dict_type), 0) + 1,
       '通知状态', 'system_notice_status', 0, '草稿/已发布/已删除', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'system_notice_status' AND deleted = 0);

-- 2) 字典数据
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT COALESCE((SELECT MAX(id) FROM system_dict_data), 0) + 1,
       1, '草稿', '0', 'system_notice_status', 0, 'info', '', '', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'system_notice_status' AND value = '0' AND deleted = 0);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT COALESCE((SELECT MAX(id) FROM system_dict_data), 0) + 1,
       2, '已发布', '1', 'system_notice_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'system_notice_status' AND value = '1' AND deleted = 0);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT COALESCE((SELECT MAX(id) FROM system_dict_data), 0) + 1,
       3, '已删除', '2', 'system_notice_status', 0, 'danger', '', '', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'system_notice_status' AND value = '2' AND deleted = 0);

-- 3) 存量通知状态迁移
UPDATE system_notice SET status = 90 WHERE status = 1 AND deleted = 0;
UPDATE system_notice SET status = 1 WHERE status = 0 AND deleted = 0;
UPDATE system_notice SET status = 0 WHERE status = 90 AND deleted = 0;
