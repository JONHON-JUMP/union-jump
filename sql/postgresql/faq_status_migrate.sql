-- 常见 QA 状态：草稿(0) / 已发布(1) / 已删除(2)
-- 旧值：开启(0)→已发布(1)；关闭(1)→草稿(0)

UPDATE system_faq SET status = 90 WHERE status = 1 AND deleted = 0;
UPDATE system_faq SET status = 1 WHERE status = 0 AND deleted = 0;
UPDATE system_faq SET status = 0 WHERE status = 90 AND deleted = 0;
