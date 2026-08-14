package cn.jonhon.jump.module.system.dal.redis.user;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;
import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.USER_QUICK_NAV;

/**
 * 用户主系统快捷导航 Redis DAO
 */
@Repository
public class UserQuickNavRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public UserQuickNavRespVO get(Long userId) {
        String redisKey = formatKey(userId);
        return JsonUtils.parseObject(stringRedisTemplate.opsForValue().get(redisKey), UserQuickNavRespVO.class);
    }

    public void set(Long userId, UserQuickNavRespVO quickNav) {
        String redisKey = formatKey(userId);
        stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonString(quickNav));
    }

    public void delete(Long userId) {
        stringRedisTemplate.delete(formatKey(userId));
    }

    public void deleteList(Collection<Long> userIds) {
        List<String> redisKeys = convertList(userIds, UserQuickNavRedisDAO::formatKey);
        if (!redisKeys.isEmpty()) {
            stringRedisTemplate.delete(redisKeys);
        }
    }

    private static String formatKey(Long userId) {
        return String.format(USER_QUICK_NAV, userId);
    }

}
