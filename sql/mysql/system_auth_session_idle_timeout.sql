-- 主系统空闲锁屏时长（分钟），可在后台「基础设施 → 参数配置」修改
-- 0 = 关闭空闲锁屏；60 = 60 分钟（默认）

INSERT INTO `infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 14, '系统安全', 2, '空闲锁屏时长（分钟）', 'system.auth.session-idle-timeout-minutes', '60', b'1',
       '用户无操作超过该分钟数后锁屏，需重新登录；填 0 表示关闭', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `infra_config` WHERE `config_key` = 'system.auth.session-idle-timeout-minutes' AND `deleted` = b'0'
);

-- 已有环境：改为 60 分钟
UPDATE `infra_config`
SET `value` = '60',
    `updater` = '1',
    `update_time` = NOW()
WHERE `config_key` = 'system.auth.session-idle-timeout-minutes'
  AND `deleted` = b'0';
