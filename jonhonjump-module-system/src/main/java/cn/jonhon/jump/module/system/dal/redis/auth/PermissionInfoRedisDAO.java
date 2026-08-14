package cn.jonhon.jump.module.system.dal.redis.auth;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;
import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.USER_PERMISSION_INFO;

/**
 * 登录用户权限信息 Redis DAO
 */
@Repository
public class PermissionInfoRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public AuthPermissionInfoRespVO get(Long userId) {
        String json = stringRedisTemplate.opsForValue().get(formatKey(userId));
        return JsonUtils.parseObject(json, AuthPermissionInfoRespVO.class);
    }

    public void set(Long userId, AuthPermissionInfoRespVO permissionInfo) {
        // 无 TTL：与快捷导航一致，数据变更走 evict，避免每 10 分钟重建 900+ 菜单
        stringRedisTemplate.opsForValue().set(formatKey(userId),
                JsonUtils.toJsonString(permissionInfo));
    }

    public void delete(Long userId) {
        stringRedisTemplate.delete(formatKey(userId));
    }

    public void deleteList(Collection<Long> userIds) {
        List<String> redisKeys = convertList(userIds, PermissionInfoRedisDAO::formatKey);
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    private static String formatKey(Long userId) {
        return String.format(USER_PERMISSION_INFO, userId);
    }

}
