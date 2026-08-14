package cn.jonhon.jump.framework.portalsubsystem.redis;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * Redis 只读模板：仅提供 GET，不提供写操作。
 */
public class PortalRedisReadTemplate {

    private final PortalSubSystemProperties properties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate delegate;

    public PortalRedisReadTemplate(PortalSubSystemProperties properties,
                                   ObjectMapper objectMapper,
                                   StringRedisTemplate stringRedisTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        // 优先使用 portal.subsystem.redis.host（主系统 Redis），避免误用子系统自身 Redis
        if (properties.getRedis().isEnabled() && StringUtils.hasText(properties.getRedis().getHost())) {
            this.delegate = createStandaloneTemplate(properties);
        } else if (stringRedisTemplate != null) {
            this.delegate = stringRedisTemplate;
        } else {
            this.delegate = null;
        }
    }

    public boolean isAvailable() {
        return delegate != null;
    }

    public String get(String key) {
        if (delegate == null) {
            throw new PortalSubSystemException("Redis 只读未配置，请配置 portal.subsystem.redis 或引入 spring-boot-starter-data-redis");
        }
        return delegate.opsForValue().get(key);
    }

    public <T> T getObject(String key, Class<T> type) {
        String json = get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new PortalSubSystemException("Redis 值解析失败，key=" + key, e);
        }
    }

    private static StringRedisTemplate createStandaloneTemplate(PortalSubSystemProperties properties) {
        PortalSubSystemProperties.Redis redis = properties.getRedis();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getPort());
        config.setDatabase(redis.getDatabase());
        if (StringUtils.hasText(redis.getPassword())) {
            config.setPassword(redis.getPassword());
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

}
