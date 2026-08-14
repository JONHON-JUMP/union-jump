package cn.jonhon.jump.framework.portalsubsystem.permission;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.enums.ReadMode;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import cn.jonhon.jump.framework.portalsubsystem.redis.PortalRedisKeyConstants;
import cn.jonhon.jump.framework.portalsubsystem.redis.PortalRedisReadTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 读取主系统下发的子系统权限（roles + permissions）。
 * <p>
 * 本地短缓存（默认 30s）避免同一页面并发 API 反复打 Redis/HTTP。
 */
@Slf4j
public class PortalPermissionReader {

    private static final long LOCAL_CACHE_TTL_MS = 30_000L;

    private final PortalSubSystemProperties properties;
    private final PortalSubSystemHttpClient httpClient;
    private final PortalRedisReadTemplate redisReadTemplate;
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    public PortalPermissionReader(PortalSubSystemProperties properties,
                                  PortalSubSystemHttpClient httpClient,
                                  PortalRedisReadTemplate redisReadTemplate) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.redisReadTemplate = redisReadTemplate;
    }

    /**
     * 仅读主系统 Redis 权限包，不走 HTTP fallback。
     * miss（返回 null）表示主系统已失效该用户权限包，子系统应提示重登或重建。
     */
    public PortalPermissionContext peekRedis(Long mainUserId, Long subSystemId) {
        if (mainUserId == null || subSystemId == null) {
            return null;
        }
        if (!redisReadTemplate.isAvailable()) {
            return null;
        }
        // 绕过本地短缓存，避免主系统已 evict 后仍命中本地假阳性
        localCache.remove(localCacheKey(mainUserId, subSystemId));
        return getFromRedis(mainUserId, subSystemId);
    }

    /**
     * 主系统 Redis 中是否仍存在该用户权限包。
     */
    public boolean existsInRedis(Long mainUserId, Long subSystemId) {
        return peekRedis(mainUserId, subSystemId) != null;
    }

    /**
     * 读子系统 RBAC 版本（主系统改菜单/角色/数据权限时递增）。
     * Redis 不可用或 key 不存在时返回 0。
     */
    public long getRbacVersion(Long subSystemId) {
        if (subSystemId == null || !redisReadTemplate.isAvailable()) {
            return 0L;
        }
        String raw = redisReadTemplate.get(PortalRedisKeyConstants.buildRbacVersionKey(subSystemId));
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    /**
     * 按主系统用户 ID + 子系统 ID 读取权限上下文。
     */
    public PortalPermissionContext get(Long mainUserId, Long subSystemId) {
        return get(mainUserId, subSystemId, null);
    }

    /**
     * 按主系统用户 ID + 子系统 ID 读取权限上下文。
     *
     * @param accessToken HTTP 模式或 Redis miss fallback 时使用，可为 null（仅 redis hit）
     */
    public PortalPermissionContext get(Long mainUserId, Long subSystemId, String accessToken) {
        if (mainUserId == null || subSystemId == null) {
            throw new PortalSubSystemException("mainUserId 与 subSystemId 不能为空");
        }
        String cacheKey = localCacheKey(mainUserId, subSystemId);
        CacheEntry cached = localCache.get(cacheKey);
        if (cached != null && !cached.expired()) {
            return cached.context;
        }

        PortalPermissionContext context;
        ReadMode readMode = ReadMode.from(properties.getReadMode());
        if (readMode == ReadMode.REDIS && redisReadTemplate.isAvailable()) {
            context = getFromRedis(mainUserId, subSystemId);
            if (context == null) {
                log.debug("权限 Redis miss，fallback HTTP，userId={}, subSystemId={}", mainUserId, subSystemId);
                context = getFromHttp(accessToken);
            }
        } else {
            context = getFromHttp(accessToken);
        }
        if (context != null) {
            localCache.put(cacheKey, new CacheEntry(context));
            // 防止异常流量下本地缓存无限膨胀
            if (localCache.size() > 10_000) {
                localCache.clear();
            }
        }
        return context;
    }

    private String localCacheKey(Long mainUserId, Long subSystemId) {
        Long tenantId = properties.getTenantId() != null ? properties.getTenantId() : 1L;
        return tenantId + ":" + mainUserId + ":" + subSystemId;
    }

    private static final class CacheEntry {
        private final PortalPermissionContext context;
        private final long expireAt;

        private CacheEntry(PortalPermissionContext context) {
            this.context = context;
            this.expireAt = System.currentTimeMillis() + LOCAL_CACHE_TTL_MS;
        }

        private boolean expired() {
            return System.currentTimeMillis() >= expireAt;
        }
    }

    private PortalPermissionContext getFromRedis(Long mainUserId, Long subSystemId) {
        Long tenantId = properties.getTenantId() != null ? properties.getTenantId() : 1L;
        String key = PortalRedisKeyConstants.buildPermContextKey(tenantId, mainUserId, subSystemId);
        return redisReadTemplate.getObject(key, PortalPermissionContext.class);
    }

    private PortalPermissionContext getFromHttp(String accessToken) {
        if (accessToken == null) {
            throw new PortalSubSystemException("权限 HTTP 读取需要 access_token，请传入或先完成 SSO");
        }
        String path = httpClient.buildSubsystemApiPath("/context?clientId=" + properties.getClientId());
        return httpClient.getForData(path, accessToken, PortalPermissionContext.class);
    }

}
