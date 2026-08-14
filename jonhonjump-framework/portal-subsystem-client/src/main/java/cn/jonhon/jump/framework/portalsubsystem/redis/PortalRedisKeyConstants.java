package cn.jonhon.jump.framework.portalsubsystem.redis;

/**
 * 门户 Redis Key 常量（与主系统保持一致）。
 */
public final class PortalRedisKeyConstants {

    private PortalRedisKeyConstants() {
    }

    /** 权限包：portal:perm:context:{tenantId}:{userId}:{subSystemId} */
    public static final String PERM_CONTEXT = "portal:perm:context:%s:%s:%s";

    /** RBAC 版本：portal_rbac_version:{subSystemId}（与主系统 RedisKeyConstants 一致） */
    public static final String RBAC_VERSION = "portal_rbac_version:%s";

    /** 扩展缓存：portal:ext:{clientId}:{namespace}:{key} */
    public static final String EXT_CACHE = "portal:ext:%s:%s:%s";

    public static String buildPermContextKey(Long tenantId, Long userId, Long subSystemId) {
        return String.format(PERM_CONTEXT, tenantId, userId, subSystemId);
    }

    public static String buildRbacVersionKey(Long subSystemId) {
        return String.format(RBAC_VERSION, subSystemId);
    }

    public static String buildExtCacheKey(String clientId, String namespace, String key) {
        return String.format(EXT_CACHE, clientId, namespace, key);
    }

}
