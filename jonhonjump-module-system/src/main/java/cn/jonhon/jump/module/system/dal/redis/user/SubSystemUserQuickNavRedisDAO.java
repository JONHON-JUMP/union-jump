package cn.jonhon.jump.module.system.dal.redis.user;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.SUB_SYSTEM_USER_QUICK_NAV;

/**
 * 用户外部子系统快捷导航 Redis DAO（按车间叶子隔离）
 */
@Repository
@Slf4j
public class SubSystemUserQuickNavRedisDAO {

    /** 菜单改名后卡片里的 name/path 不能永久停在旧值；主动失效之外再兜底过期 */
    private static final Duration CACHE_TTL = Duration.ofHours(2);

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
                formatKey(userId, subSystemId, workshopCode), JsonUtils.toJsonString(quickNav), CACHE_TTL);
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

    /**
     * 一次 SCAN 清掉该系统下所有用户、所有车间的快捷导航。
     * userIds 只为兼容旧调用，不再按人各扫一遍全库。
     */
    public void deleteBySubSystemId(Long subSystemId, Collection<Long> userIds) {
        if (subSystemId == null) {
            return;
        }
        scanAndDelete(formatSubSystemPattern(subSystemId));
    }

    private void scanAndDelete(String pattern) {
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
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("[quick-nav] SCAN failed pattern={}: {}", pattern, ex.toString());
        }
    }

    /** sub_system_user_quick_nav_v3:*:{subSystemId}:* ，前缀来自 RedisKeyConstants */
    public static String formatSubSystemPattern(Long subSystemId) {
        int marker = SUB_SYSTEM_USER_QUICK_NAV.indexOf("%s");
        String prefix = marker > 0 ? SUB_SYSTEM_USER_QUICK_NAV.substring(0, marker) : "sub_system_user_quick_nav_v3:";
        return prefix + "*:" + subSystemId + ":*";
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
