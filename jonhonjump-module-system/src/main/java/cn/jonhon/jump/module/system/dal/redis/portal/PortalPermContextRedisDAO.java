package cn.jonhon.jump.module.system.dal.redis.portal;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.PORTAL_PERM_CONTEXT;

/**
 * 子系统权限包 Redis DAO（主系统独占写）
 * <p>
 * Key 维度：{@code portal:perm:context:{username}:{clientId}}。
 * 批量失效时用 pattern 扫描（按 username 或 clientId），避免回查 mainUserId↔username 映射。
 */
@Repository
public class PortalPermContextRedisDAO {

    /** 权限包 TTL：2～4 小时兜底，主动失效为主 */
    private static final Duration CACHE_TTL = Duration.ofHours(3);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public PortalPermContextRespVO get(String username, String clientId) {
        String json = stringRedisTemplate.opsForValue().get(formatKey(username, clientId));
        return JsonUtils.parseObject(json, PortalPermContextRespVO.class);
    }

    public void set(String username, String clientId, PortalPermContextRespVO context) {
        stringRedisTemplate.opsForValue().set(
                formatKey(username, clientId),
                JsonUtils.toJsonString(context),
                CACHE_TTL);
    }

    /**
     * 续期 TTL（滑动过期）：多机同用户登录时，以最近一次访问为准重置 3 小时。
     * <p>
     * 场景：A 机登录写入权限包（TTL 3h）；B 机同账号登录命中缓存时调用本方法续期，
     * 避免 B 机还在用、权限包却先过期。
     *
     * @return true 表示续期成功（key 存在）
     */
    public boolean refreshTtl(String username, String clientId) {
        if (username == null || username.isEmpty() || clientId == null || clientId.isEmpty()) {
            return false;
        }
        Boolean ok = stringRedisTemplate.expire(formatKey(username, clientId), CACHE_TTL);
        return Boolean.TRUE.equals(ok);
    }

    public void delete(String username, String clientId) {
        stringRedisTemplate.delete(formatKey(username, clientId));
    }

    /**
     * 批量精确删：给定 clientId + 一批 username，只删这些用户的权限包。
     * <p>
     * 用于角色/菜单变更时，只清"绑了该角色/授权了该菜单"的用户，
     * 而不是清整个子系统所有用户。用户不在线（无权限包）时 DELETE 无害。
     *
     * @param clientId  子系统 clientId
     * @param usernames 受影响用户名集合
     * @return 实际删除的 key 数
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
     * 清某用户在所有子系统下的权限包（pattern 扫描）。
     * <p>
     * 用户禁用/删除、跨子系统批量失效时调用，无需逐个解析 clientId。
     *
     * @return 实际删除的 key 数
     */
    public int deleteByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return 0;
        }
        Set<String> keys = stringRedisTemplate.keys(formatUsernamePattern(username));
        return deleteKeys(keys);
    }

    /**
     * 清某子系统下所有用户的权限包（pattern 扫描）。
     * <p>
     * 菜单/角色变更、子系统停用时调用，无需逐个解析 username。
     *
     * @return 实际删除的 key 数
     */
    public int deleteByClientId(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return 0;
        }
        Set<String> keys = stringRedisTemplate.keys(formatClientIdPattern(clientId));
        return deleteKeys(keys);
    }

    private int deleteKeys(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        stringRedisTemplate.delete(keys);
        return keys.size();
    }

    public static String formatKey(String username, String clientId) {
        return String.format(PORTAL_PERM_CONTEXT, username, clientId);
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
