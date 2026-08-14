package cn.jonhon.jump.module.system.dal.redis.portal;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.PORTAL_EXT_CACHE;

/**
 * 子系统扩展业务缓存 Redis DAO（仅主系统代写）
 */
@Repository
public class PortalExtCacheRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String get(String clientId, String namespace, String key) {
        return stringRedisTemplate.opsForValue().get(formatKey(clientId, namespace, key));
    }

    public void set(String clientId, String namespace, String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(formatKey(clientId, namespace, key), value,
                Duration.ofSeconds(ttlSeconds));
    }

    public void delete(String clientId, String namespace, String key) {
        stringRedisTemplate.delete(formatKey(clientId, namespace, key));
    }

    public Boolean expire(String clientId, String namespace, String key, long ttlSeconds) {
        return stringRedisTemplate.expire(formatKey(clientId, namespace, key), ttlSeconds, TimeUnit.SECONDS);
    }

    public static String formatKey(String clientId, String namespace, String key) {
        return String.format(PORTAL_EXT_CACHE, clientId, namespace, key);
    }

}
