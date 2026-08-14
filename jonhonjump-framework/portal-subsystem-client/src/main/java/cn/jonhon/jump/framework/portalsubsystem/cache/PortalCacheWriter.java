package cn.jonhon.jump.framework.portalsubsystem.cache;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import cn.jonhon.jump.framework.portalsubsystem.oauth.PortalOAuthClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 写入子系统扩展缓存（固定 HTTP，主系统代写 Redis）。
 */
@Slf4j
public class PortalCacheWriter {

    private final PortalSubSystemProperties properties;
    private final PortalSubSystemHttpClient httpClient;
    private final PortalOAuthClient oauthClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PortalCacheWriter(PortalSubSystemProperties properties,
                             PortalSubSystemHttpClient httpClient,
                             PortalOAuthClient oauthClient) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.oauthClient = oauthClient;
    }

    public void put(String namespace, String key, Object value) {
        put(namespace, key, value, null);
    }

    public void put(String namespace, String key, Object value, Integer ttlSeconds) {
        validateNamespace(namespace);
        validateKey(key);
        String accessToken = oauthClient.ensureAccessToken();
        String path = httpClient.buildSubsystemApiPath("/cache/" + namespace + "/" + encodePath(key));
        String body = toJsonBody(value, ttlSeconds);
        try {
            httpClient.putJsonRequireSuccess(path, body, accessToken);
        } catch (PortalSubSystemException e) {
            if (isUnauthorized(e) && oauthClient.getCurrentAccessToken() != null) {
                log.info("扩展缓存写入 401，尝试 refresh token 后重试");
                oauthClient.refreshCurrentToken();
                httpClient.putJsonRequireSuccess(path, body, oauthClient.ensureAccessToken());
                return;
            }
            log.error("扩展缓存写入失败，namespace={}, key={}", namespace, key, e);
            throw e;
        }
    }

    public void delete(String namespace, String key) {
        validateNamespace(namespace);
        validateKey(key);
        String accessToken = oauthClient.ensureAccessToken();
        String path = httpClient.buildSubsystemApiPath("/cache/" + namespace + "/" + encodePath(key));
        httpClient.deleteRequireSuccess(path, accessToken);
    }

    public void expire(String namespace, String key, int ttlSeconds) {
        validateNamespace(namespace);
        validateKey(key);
        String accessToken = oauthClient.ensureAccessToken();
        String path = httpClient.buildSubsystemApiPath("/cache/" + namespace + "/" + encodePath(key) + "/expire");
        httpClient.postJsonRequireSuccess(path, "{\"ttlSeconds\":" + ttlSeconds + "}", accessToken);
    }

    private String toJsonBody(Object value, Integer ttlSeconds) {
        try {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("value", value);
            map.put("ttlSeconds", ttlSeconds != null ? ttlSeconds : 3600);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new PortalSubSystemException("扩展缓存 value 序列化失败", e);
        }
    }

    private void validateNamespace(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            throw new PortalSubSystemException("namespace 不能为空");
        }
        if (!properties.getCache().getAllowedNamespaces().isEmpty()
                && !properties.getCache().getAllowedNamespaces().contains(namespace)) {
            throw new PortalSubSystemException("namespace 不在白名单内：" + namespace);
        }
    }

    private void validateKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new PortalSubSystemException("key 不能为空");
        }
        if (key.contains("..")) {
            throw new PortalSubSystemException("key 非法：" + key);
        }
    }

    private static boolean isUnauthorized(PortalSubSystemException e) {
        String message = e.getMessage();
        return message != null && (message.contains("401") || message.contains("未授权") || message.contains("Unauthorized"));
    }

    private static String encodePath(String key) {
        return key.replace(" ", "%20");
    }

}
