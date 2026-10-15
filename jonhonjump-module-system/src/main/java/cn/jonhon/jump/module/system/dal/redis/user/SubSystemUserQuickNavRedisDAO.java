package cn.jonhon.jump.module.system.dal.redis.user;

import cn.hutool.core.util.StrUtil;
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
 * 用户外部子系统快捷导航 Redis DAO（按车间叶子隔离）
 */
@Repository
public class SubSystemUserQuickNavRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public SubSystemUserQuickNavRespVO get(Long userId, Long subSystemId) {
        return get(userId, subSystemId, null);
    }

    public SubSystemUserQuickNavRespVO get(Long userId, Long subSystemId, String workshopCode) {
        return JsonUtils.parseObject(
                stringRedisTemplate.opsForValue().get(formatKey(userId, subSystemId, workshopCode)),
                SubSystemUserQuickNavRespVO.class);
    }

    public void set(Long userId, Long subSystemId, SubSystemUserQuickNavRespVO quickNav) {
        set(userId, subSystemId, null, quickNav);
    }

    public void set(Long userId, Long subSystemId, String workshopCode, SubSystemUserQuickNavRespVO quickNav) {
        stringRedisTemplate.opsForValue().set(
                formatKey(userId, subSystemId, workshopCode), JsonUtils.toJsonString(quickNav));
    }

    public void delete(Long userId, Long subSystemId) {
        delete(userId, subSystemId, null);
    }

    public void delete(Long userId, Long subSystemId, String workshopCode) {
        stringRedisTemplate.delete(formatKey(userId, subSystemId, workshopCode));
    }

    public void deleteList(Collection<SubSystemQuickNavCacheKey> cacheKeys) {
        List<String> redisKeys = cacheKeys.stream()
                .map(key -> formatKey(key.getUserId(), key.getSubSystemId(), key.getWorkshopCode()))
                .collect(Collectors.toList());
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    /** 按子系统 + 已知用户精确删除快捷导航缓存（各叶子 key） */
    public void deleteBySubSystemId(Long subSystemId, Collection<Long> userIds) {
        if (subSystemId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        // 兼容：清系统级 key；叶子 key 依赖业务侧按 workshop 精确删或自然过期/重建
        List<String> redisKeys = userIds.stream()
                .filter(Objects::nonNull)
                .map(userId -> formatKey(userId, subSystemId, null))
                .collect(Collectors.toList());
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    public static String formatKey(Long userId, Long subSystemId, String workshopCode) {
        String workshop = StrUtil.blankToDefault(StrUtil.trim(workshopCode), "-");
        return String.format(SUB_SYSTEM_USER_QUICK_NAV, userId, subSystemId, workshop);
    }

    private static String formatKey(Long userId, Long subSystemId) {
        return formatKey(userId, subSystemId, null);
    }

    public static SubSystemQuickNavCacheKey cacheKey(Long userId, Long subSystemId) {
        return new SubSystemQuickNavCacheKey(userId, subSystemId, null);
    }

    public static SubSystemQuickNavCacheKey cacheKey(Long userId, Long subSystemId, String workshopCode) {
        return new SubSystemQuickNavCacheKey(userId, subSystemId, workshopCode);
    }

    public static final class SubSystemQuickNavCacheKey {

        private final Long userId;
        private final Long subSystemId;
        private final String workshopCode;

        public SubSystemQuickNavCacheKey(Long userId, Long subSystemId, String workshopCode) {
            this.userId = userId;
            this.subSystemId = subSystemId;
            this.workshopCode = workshopCode;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getSubSystemId() {
            return subSystemId;
        }

        public String getWorkshopCode() {
            return workshopCode;
        }

    }

}
