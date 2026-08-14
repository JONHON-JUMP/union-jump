package cn.jonhon.jump.framework.portalsubsystem.cache;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.enums.ReadMode;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import cn.jonhon.jump.framework.portalsubsystem.redis.PortalRedisKeyConstants;
import cn.jonhon.jump.framework.portalsubsystem.redis.PortalRedisReadTemplate;
import org.springframework.util.StringUtils;

/**
 * 读取子系统扩展缓存（portal:ext:*）。
 */
public class PortalCacheReader {

    private final PortalSubSystemProperties properties;
    private final PortalSubSystemHttpClient httpClient;
    private final PortalRedisReadTemplate redisReadTemplate;

    public PortalCacheReader(PortalSubSystemProperties properties,
                             PortalSubSystemHttpClient httpClient,
                             PortalRedisReadTemplate redisReadTemplate) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.redisReadTemplate = redisReadTemplate;
    }

    public String get(String namespace, String key) {
        return get(namespace, key, null);
    }

    public String get(String namespace, String key, String accessToken) {
        validateNamespace(namespace);
        validateKey(key);
        ReadMode readMode = ReadMode.from(properties.getReadMode());
        if (readMode == ReadMode.REDIS && redisReadTemplate.isAvailable()) {
            String redisKey = PortalRedisKeyConstants.buildExtCacheKey(properties.getClientId(), namespace, key);
            String value = redisReadTemplate.get(redisKey);
            if (value != null) {
                return value;
            }
        }
        if (accessToken == null) {
            throw new PortalSubSystemException("扩展缓存 HTTP 读取需要 access_token");
        }
        String path = httpClient.buildSubsystemApiPath("/cache/" + namespace + "/" + encodePath(key));
        return httpClient.getForData(path, accessToken, String.class);
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

    private static String encodePath(String key) {
        return key.replace(" ", "%20");
    }

}
