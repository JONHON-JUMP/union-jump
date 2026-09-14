package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ErrorCode;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

/** MPM token 统一入口：只在成功获取有效 token 后替换，失败时保留上一份。 */
@Slf4j
@Service
public class MpmTokenService {
    @Resource
    private RestTemplate restTemplate;

    @Value("${jonhonjump.mes.process.mpm-login-url:http://mpm.caoe.com/basic/loginController/login.do}")
    private String loginUrl;
    @Value("${jonhonjump.mes.process.mpm-login-username:MPMJCUSER}")
    private String username;
    @Value("${jonhonjump.mes.process.mpm-login-password:}")
    private String password;
    @Value("${jonhonjump.mes.process.mpm-login-language:zh-CN}")
    private String defaultLanguage;

    // 配置中的 token 仅作为启动刷新成功前的兜底，不再作为固定请求头。
    @Value("${jonhonjump.mes.process.mpm-access-token:}")
    private volatile String token;

    public String getToken() {
        String currentToken = token;
        if (StringUtils.isBlank(currentToken)) {
            throw exception(new ErrorCode(500, "MPM登录凭证不可用，请检查登录配置或稍后重试"));
        }
        return currentToken;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refreshToken();
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Shanghai")
    public synchronized void refreshToken() {
        if (StringUtils.isAnyBlank(loginUrl, username, password, defaultLanguage)) {
            log.error("MPM token刷新失败：登录配置不完整，保留原token");
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("username", username);
            request.put("password", password);
            request.put("defaultLanguage", defaultLanguage);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(loginUrl,
                    new HttpEntity<>(request.toJSONString(), headers), String.class);
            JSONObject body = response.getStatusCode().is2xxSuccessful()
                    ? JSONObject.parseObject(response.getBody()) : null;
            Object code = body == null ? null : body.get("code");
            Object result = body == null ? null : body.get("result");
            Object newToken = result instanceof JSONObject ? ((JSONObject) result).get("token") : null;
            if (body == null || !Boolean.TRUE.equals(body.get("success"))
                    || !(code instanceof Number) || ((Number) code).intValue() != 200
                    || !(newToken instanceof String) || StringUtils.isBlank((String) newToken)) {
                log.error("MPM token刷新失败：登录响应无效，保留原token");
                return;
            }
            token = ((String) newToken).trim();
            log.info("MPM token刷新成功");
        } catch (RuntimeException failure) {
            // HTTP/JSON异常可能包含密码或token，只记录异常类型。
            log.error("MPM token刷新失败，保留原token，异常类型：{}", failure.getClass().getSimpleName());
        }
    }
}
