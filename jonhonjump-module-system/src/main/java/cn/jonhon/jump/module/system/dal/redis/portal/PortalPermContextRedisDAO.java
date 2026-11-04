package cn.jonhon.jump.module.system.dal.redis.portal;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.PORTAL_PERM_CONTEXT;

/**
 * 子系统权限包 Redis DAO（主系统独占写）
 * <p>
 * 现行 key：{@code portal:perm:context:{username}:{clientId}}。
 * 已部署的子系统客户端仍读 {@code portal:perm:context:{tenantId}:{userId}:{subSystemId}}，
 * 写入和失效时镜像一份，避免 read-mode=redis 时永远 miss、被当成权限已失效。
 */
@Repository
@Slf4j
public class PortalPermContextRedisDAO {

    /** 与客户端 PortalRedisKeyConstants.PERM_CONTEXT_LEGACY 保持同一格式 */
    public static final String LEGACY_PERM_CONTEXT = "portal:perm:context:%s:%s:%s";

    /** 权限包 TTL：2～4 小时兜底，主动失效为主 */
    private static final Duration CACHE_TTL = Duration.ofHours(3);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public PortalPermContextRespVO get(String username, String clientId) {
        String json = stringRedisTemplate.opsForValue().get(formatKey(username, clientId));
        return JsonUtils.parseObject(json, PortalPermContextRespVO.class);
    }

    public void set(String username, String clientId, PortalPermContextRespVO context) {
        String json = JsonUtils.toJsonString(context);
        stringRedisTemplate.opsForValue().set(formatKey(username, clientId), json, CACHE_TTL);
        String legacy = legacyKeyOf(context);
        if (legacy != null) {
            stringRedisTemplate.opsForValue().set(legacy, json, CACHE_TTL);
        }
    }

    /**
     * 续期现行 key，并用内存里的权限包补镜像 key。一次 pipeline 发出，避免进子系统时多次往返。
     * 镜像 key 已在则续期；被单独淘汰时 SET NX 补上，再 EXPIRE 一次无害。
     *
     * @return true 表示现行 key 仍在（pipeline 不逐条看返回值，有 key 名即视为已提交）
     */
    public boolean refreshTtl(String username, String clientId, PortalPermContextRespVO context) {
        if (username == null || username.isEmpty() || clientId == null || clientId.isEmpty()) {
            return false;
        }
        String canonical = formatKey(username, clientId);
        String legacy = legacyKeyOf(context);
        String legacyJson = legacy == null ? null : JsonUtils.toJsonString(context);
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Object execute(RedisOperations operations) {
                operations.expire(canonical, CACHE_TTL);
                if (legacy != null) {
                    operations.opsForValue().setIfAbsent(legacy, legacyJson, CACHE_TTL);
                    operations.expire(legacy, CACHE_TTL);
                }
                return null;
            }
        });
        return true;
    }

    public void delete(String username, String clientId) {
        stringRedisTemplate.delete(formatKey(username, clientId));
    }

    /** 按租户 + 主用户 + 业务系统删镜像 key。现行 key 已经不在时也要删，避免旧包残留。 */
    public void deleteLegacy(Long tenantId, Long userId, Long subSystemId) {
        String legacy = formatLegacyKey(tenantId, userId, subSystemId);
        if (legacy != null) {
            stringRedisTemplate.delete(legacy);
        }
    }

    public void deleteLegacyBatch(Long tenantId, Long subSystemId, Collection<Long> userIds) {
        if (subSystemId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Long userId : userIds) {
            String legacy = formatLegacyKey(tenantId, userId, subSystemId);
            if (legacy != null) {
                keys.add(legacy);
            }
        }
        deleteKeys(keys);
    }

    /**
     * 清某业务系统下全部镜像 key（三段：tenant、userId、subSystemId）。
     */
    public int deleteLegacyBySubSystemId(Long subSystemId) {
        if (subSystemId == null) {
            return 0;
        }
        return scanAndDelete("portal:perm:context:*:*:" + subSystemId);
    }

    /**
     * 批量精确删：给定 clientId + 一批 username，只删这些用户的权限包。
     */
    public int deleteBatch(String clientId, Collection<String> usernames) {
        if (clientId == null || clientId.isEmpty() || usernames == null || usernames.isEmpty()) {
            return 0;
        }
        Set<String> keys = new LinkedHashSet<>();
        for (String username : usernames) {
            if (username != null && !username.isEmpty()) {
                keys.add(formatKey(username, clientId));
            }
        }
        return deleteKeys(keys);
    }

    /**
     * 清某用户在所有子系统下的权限包（SCAN）。
     */
    public int deleteByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return 0;
        }
        return scanAndDelete(formatUsernamePattern(username));
    }

    /**
     * 清某子系统下所有用户的权限包（SCAN）。
     */
    public int deleteByClientId(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return 0;
        }
        return scanAndDelete(formatClientIdPattern(clientId));
    }

    private static String legacyKeyOf(PortalPermContextRespVO context) {
        if (context == null) {
            return null;
        }
        return formatLegacyKey(context.getTenantId(), context.getUserId(), context.getSubSystemId());
    }

    private int deleteKeys(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long n = stringRedisTemplate.delete(keys);
        return n == null ? keys.size() : n.intValue();
    }

    private int scanAndDelete(String pattern) {
        int deleted = 0;
        try {
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            Set<String> keys = new HashSet<>();
            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                    if (keys.size() >= 500) {
                        deleted += deleteKeys(keys);
                        keys.clear();
                    }
                }
            }
            deleted += deleteKeys(keys);
        } catch (Exception ex) {
            log.warn("[portal-perm] SCAN failed pattern={}, fallback KEYS: {}", pattern, ex.toString());
            try {
                Set<String> found = stringRedisTemplate.keys(pattern);
                if (found != null && !found.isEmpty()) {
                    deleted += deleteKeys(found);
                }
            } catch (Exception ex2) {
                log.warn("[portal-perm] KEYS fallback also failed pattern={}: {}", pattern, ex2.toString());
            }
        }
        return deleted;
    }

    public static String formatKey(String username, String clientId) {
        return String.format(PORTAL_PERM_CONTEXT, username, clientId);
    }

    /**
     * 过渡 key，与客户端 {@code buildPermContextKey(tenantId, userId, subSystemId)} 一致。
     * tenant 为空时按 1，和客户端默认租户相同。
     */
    public static String formatLegacyKey(Long tenantId, Long userId, Long subSystemId) {
        if (userId == null || subSystemId == null) {
            return null;
        }
        long tenant = tenantId != null ? tenantId : 1L;
        return String.format(LEGACY_PERM_CONTEXT, tenant, userId, subSystemId);
    }

    /** portal:perm:context:{username}:* —— 匹配该用户在所有子系统下的权限包 */
    public static String formatUsernamePattern(String username) {
        return "portal:perm:context:" + username + ":*";
    }

    /** portal:perm:context:*:{clientId} —— 匹配该子系统下所有用户的权限包 */
    public static String formatClientIdPattern(String clientId) {
        return "portal:perm:context:*:" + clientId;
    }

}
