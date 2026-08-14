package cn.jonhon.jump.module.system.dal.redis.user;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.PORTAL_RBAC_VERSION;

/**
 * 子系统 RBAC 版本：菜单/角色变更时递增，供门户轻量判断是否需要重拉 my-menus。
 */
@Repository
public class PortalRbacVersionRedisDAO {

    /** 版本号 TTL：与权限包（portal:perm:context，3 小时）对齐，避免权限包过期后版本号残留导致子系统误判变更 */
    private static final Duration TTL = Duration.ofHours(3);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long get(Long subSystemId) {
        if (subSystemId == null) {
            return 0L;
        }
        String raw = stringRedisTemplate.opsForValue().get(formatKey(subSystemId));
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public long bump(Long subSystemId) {
        if (subSystemId == null) {
            return 0L;
        }
        String key = formatKey(subSystemId);
        Long next = stringRedisTemplate.opsForValue().increment(key);
        // 同步刷新过期：版本号必须与权限包同生命周期，否则权限包过期删除后版本号仍存活，
        // 子系统探测到 existsInRedis=false 但版本号还在 → 误判权限变更 → 误提示重登。
        stringRedisTemplate.expire(key, TTL);
        return next == null ? 0L : next;
    }

    /**
     * 刷新版本号过期时间（权限包重建时调用，保证版本号不会比权限包先过期）。
     */
    public void refreshTtl(Long subSystemId) {
        if (subSystemId == null) {
            return;
        }
        stringRedisTemplate.expire(formatKey(subSystemId), TTL);
    }

    public static String formatKey(Long subSystemId) {
        return String.format(PORTAL_RBAC_VERSION, subSystemId);
    }
}
