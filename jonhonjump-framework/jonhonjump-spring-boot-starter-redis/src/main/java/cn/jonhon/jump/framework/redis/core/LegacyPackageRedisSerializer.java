package cn.jonhon.jump.framework.redis.core;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

/**
 * 兼容历史 Redis 缓存中的旧包名（cn.iocoder.yudao -> cn.jonhon.jump）
 */
public class LegacyPackageRedisSerializer implements RedisSerializer<Object> {

    private static final String LEGACY_PACKAGE = "cn.iocoder.yudao";
    private static final String CURRENT_PACKAGE = "cn.jonhon.jump";

    private final RedisSerializer<Object> delegate;

    public LegacyPackageRedisSerializer(RedisSerializer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        return delegate.serialize(value);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return delegate.deserialize(bytes);
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        if (json.contains(LEGACY_PACKAGE)) {
            json = json.replace(LEGACY_PACKAGE, CURRENT_PACKAGE);
            bytes = json.getBytes(StandardCharsets.UTF_8);
        }
        return delegate.deserialize(bytes);
    }

}
