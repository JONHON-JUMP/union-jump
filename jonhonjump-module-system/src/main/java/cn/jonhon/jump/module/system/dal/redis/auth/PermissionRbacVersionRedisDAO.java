package cn.jonhon.jump.module.system.dal.redis.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.USER_PERMISSION_RBAC_VERSION;

/**
 * 主系统用户权限 RBAC 版本：菜单/角色/授权/数据权限变更时递增。
 */
@Repository
public class PermissionRbacVersionRedisDAO {

    /** 版本号 TTL：与权限包（user_permission_info）对齐，避免权限包过期后版本号残留导致误判变更 */
    private static final Duration TTL = Duration.ofHours(3);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long get(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String raw = stringRedisTemplate.opsForValue().get(formatKey(userId));
        if (raw == null || raw.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public long bump(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String key = formatKey(userId);
        Long next = stringRedisTemplate.opsForValue().increment(key);
        // 同步刷新过期：版本号必须与权限包同生命周期，否则权限包过期删除后版本号仍存活，
        // 前端/子系统探测到权限包 miss 但版本号还在 → 误判权限变更 → 误提示重登。
        stringRedisTemplate.expire(key, TTL);
        return next == null ? 0L : next;
    }

    public void bumpList(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            bump(userId);
        }
    }

    private static String formatKey(Long userId) {
        return String.format(USER_PERMISSION_RBAC_VERSION, userId);
    }

}
