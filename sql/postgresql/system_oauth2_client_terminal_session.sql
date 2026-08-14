-- 主系统现场模式：缩短 Refresh Token 有效期（一班 8 小时）
-- Access Token 保持 30 分钟（1800 秒）不变
-- 执行后需用户重新登录才生效

UPDATE system_oauth2_client
SET refresh_token_validity_seconds = 28800,
    updater = '1',
    update_time = NOW()
WHERE client_id = 'default'
  AND deleted = 0;
