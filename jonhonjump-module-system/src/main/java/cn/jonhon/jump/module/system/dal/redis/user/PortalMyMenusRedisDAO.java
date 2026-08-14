package cn.jonhon.jump.module.system.dal.redis.user;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPortalMenuRespVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.PORTAL_MY_MENUS;

/**
 * 门户 my-menus 缓存：按主用户 + 子系统。
 * <p>
 * 失效策略（按优先级）：
 * 1) 按已知 mainUserId 精确 delete（不依赖 KEYS，生产可用）；
 * 2) SCAN pattern 兜底（KEYS 在部分环境禁用）；
 * 3) 写入带 TTL。
 */
@Repository
@Slf4j
public class PortalMyMenusRedisDAO {

    private static final TypeReference<List<SubSystemPortalMenuRespVO>> TYPE =
            new TypeReference<List<SubSystemPortalMenuRespVO>>() {};

    /** 兜底 TTL：即便主动失效漏掉（孤儿缓存），最多存在 30 分钟后自动过期 */
    private static final long TTL_MINUTES = 30L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public List<SubSystemPortalMenuRespVO> get(Long userId, Long subSystemId) {
        String json = stringRedisTemplate.opsForValue().get(formatKey(userId, subSystemId));
        if (json == null) {
            return null;
        }
        return JsonUtils.parseObject(json, TYPE);
    }

    public void set(Long userId, Long subSystemId, List<SubSystemPortalMenuRespVO> menus) {
        stringRedisTemplate.opsForValue().set(formatKey(userId, subSystemId),
                JsonUtils.toJsonString(menus), TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void delete(Long userId, Long subSystemId) {
        stringRedisTemplate.delete(formatKey(userId, subSystemId));
    }

    public void deleteList(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(keys);
    }

    /**
     * 按子系统清除 my-menus：先精确删已知用户 key，再用 SCAN 扫孤儿。
     *
     * @param mainUserIds 该子系统下主用户 ID（可为 null/空，则只走 SCAN）
     * @return 实际删除的 key 数（估算）
     */
    public int deleteBySubSystemId(Long subSystemId, Collection<Long> mainUserIds) {
        if (subSystemId == null) {
            return 0;
        }
        int deleted = 0;
        if (mainUserIds != null && !mainUserIds.isEmpty()) {
            List<String> exactKeys = new ArrayList<>(mainUserIds.size());
            for (Long userId : mainUserIds) {
                if (userId != null) {
                    exactKeys.add(formatKey(userId, subSystemId));
                }
            }
            if (!exactKeys.isEmpty()) {
                Long n = stringRedisTemplate.delete(exactKeys);
                deleted += n == null ? 0 : n.intValue();
            }
        }
        // SCAN 兜底（不用 KEYS：生产常禁用或阻塞）
        deleted += scanAndDelete(formatSubSystemPattern(subSystemId));
        if (deleted == 0) {
            log.debug("[portal-my-menus] deleteBySubSystemId subSystemId={} deleted=0 (no keys or already empty)",
                    subSystemId);
        } else {
            log.info("[portal-my-menus] deleteBySubSystemId subSystemId={} deleted≈{}", subSystemId, deleted);
        }
        return deleted;
    }

    /** 兼容旧调用：无用户列表时仅 SCAN */
    public int deleteBySubSystemId(Long subSystemId) {
        return deleteBySubSystemId(subSystemId, null);
    }

    /**
     * 按主用户清除该用户在所有子系统下的 my-menus 缓存。
     */
    public int deleteByMainUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        return scanAndDelete(formatUserPattern(userId));
    }

    private int scanAndDelete(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                    if (keys.size() >= 500) {
                        stringRedisTemplate.delete(keys);
                        keys.clear();
                    }
                }
            }
            if (!keys.isEmpty()) {
                Long n = stringRedisTemplate.delete(keys);
                return n == null ? 0 : n.intValue();
            }
        } catch (Exception ex) {
            // 部分环境无 scan 权限时降级尝试 keys（可能仍失败，仅打日志）
            log.warn("[portal-my-menus] SCAN failed pattern={}, fallback KEYS: {}", pattern, ex.toString());
            try {
                Set<String> legacy = stringRedisTemplate.keys(pattern);
                if (legacy != null && !legacy.isEmpty()) {
                    Long n = stringRedisTemplate.delete(legacy);
                    return n == null ? 0 : n.intValue();
                }
            } catch (Exception ex2) {
                log.warn("[portal-my-menus] KEYS fallback also failed pattern={}: {}", pattern, ex2.toString());
            }
        }
        return 0;
    }

    public static String formatKey(Long userId, Long subSystemId) {
        return String.format(PORTAL_MY_MENUS, userId, subSystemId);
    }

    /** portal_my_menus:*:{subSystemId} —— 匹配该子系统下所有用户 */
    public static String formatSubSystemPattern(Long subSystemId) {
        return "portal_my_menus:*:" + subSystemId;
    }

    /** portal_my_menus:{userId}:* —— 匹配该用户在所有子系统下的缓存 */
    public static String formatUserPattern(Long userId) {
        return "portal_my_menus:" + userId + ":*";
    }
}
