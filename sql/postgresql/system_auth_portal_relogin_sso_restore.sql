-- 过期重登后恢复已登录子系统 SSO 标记的有效期（分钟）
-- 建议 ≥ 空闲锁屏分钟数（默认锁屏 60）；写入快照时会与锁屏时长取 max
-- 120 = 同账号 120 分钟内重登主系统后，子系统静默换票；0 = 关闭恢复

INSERT INTO infra_config (id, category, type, name, config_key, value, visible, remark, creator, create_time, updater, update_time, deleted)
SELECT 15, '系统安全', 2, '过期重登恢复子系统SSO（分钟）', 'system.auth.portal-relogin-sso-restore-minutes', '120', true,
       '主系统登录过期后，同账号在该分钟数内重新登录主系统，切换子系统时用新 token 静默换票（不弹第二次登录）；建议≥锁屏分钟数；填 0 表示关闭', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM infra_config WHERE config_key = 'system.auth.portal-relogin-sso-restore-minutes' AND deleted = 0
);

-- 已有环境：若仍是 30，建议升到至少覆盖锁屏
UPDATE infra_config
SET value = '120',
    remark = '主系统登录过期后，同账号在该分钟数内重新登录主系统，切换子系统时用新 token 静默换票（不弹第二次登录）；建议≥锁屏分钟数；填 0 表示关闭',
    update_time = NOW()
WHERE config_key = 'system.auth.portal-relogin-sso-restore-minutes'
  AND deleted = 0
  AND value = '30';
