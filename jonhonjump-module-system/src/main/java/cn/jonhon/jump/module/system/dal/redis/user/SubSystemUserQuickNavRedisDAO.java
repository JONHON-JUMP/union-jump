package cn.jonhon.jump.module.system.dal.redis.user;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.SUB_SYSTEM_USER_QUICK_NAV;

/**
 * 用户外部子系统快捷导航 Redis DAO
 */
@Repository
public class SubSystemUserQuickNavRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public SubSystemUserQuickNavRespVO get(Long userId, Long subSystemId) {
        String redisKey = formatKey(userId, subSystemId);
        return JsonUtils.parseObject(stringRedisTemplate.opsForValue().get(redisKey), SubSystemUserQuickNavRespVO.class);
    }

    public void set(Long userId, Long subSystemId, SubSystemUserQuickNavRespVO quickNav) {
        String redisKey = formatKey(userId, subSystemId);
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(quickNav));
    }

    public void delete(Long userId, Long subSystemId) {
        stringRedisTemplate.delete(formatKey(userId, subSystemId));
    }

    public void deleteList(Collection<SubSystemQuickNavCacheKey> cacheKeys) {
        List<String> redisKeys = cacheKeys.stream()
                .map(key -> formatKey(key.getUserId(), key.getSubSystemId()))
                .collect(Collectors.toList());
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    /** 按子系统 + 已知用户精确删除快捷导航缓存（菜单/权限变更时与 my-menus 一并失效） */
    public void deleteBySubSystemId(Long subSystemId, Collection<Long> userIds) {
        if (subSystemId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> redisKeys = userIds.stream()
                .filter(Objects::nonNull)
                .map(userId -> formatKey(userId, subSystemId))
                .collect(Collectors.toList());
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    private static String formatKey(Long userId, Long subSystemId) {
        return String.format(SUB_SYSTEM_USER_QUICK_NAV, userId, subSystemId);
    }

    public static SubSystemQuickNavCacheKey cacheKey(Long userId, Long subSystemId) {
        return new SubSystemQuickNavCacheKey(userId, subSystemId);
    }

    public static final class SubSystemQuickNavCacheKey {

        private final Long userId;
        private final Long subSystemId;

        public SubSystemQuickNavCacheKey(Long userId, Long subSystemId) {
            this.userId = userId;
            this.subSystemId = subSystemId;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getSubSystemId() {
            return subSystemId;
        }

    }

}
