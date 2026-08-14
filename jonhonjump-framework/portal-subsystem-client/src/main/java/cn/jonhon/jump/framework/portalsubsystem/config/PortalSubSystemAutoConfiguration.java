package cn.jonhon.jump.framework.portalsubsystem.config;

import cn.jonhon.jump.framework.portalsubsystem.auth.PortalMachineAuthClient;
import cn.jonhon.jump.framework.portalsubsystem.cache.PortalCacheReader;
import cn.jonhon.jump.framework.portalsubsystem.cache.PortalCacheWriter;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import cn.jonhon.jump.framework.portalsubsystem.oauth.PortalOAuthClient;
import cn.jonhon.jump.framework.portalsubsystem.permission.PortalPermissionReader;
import cn.jonhon.jump.framework.portalsubsystem.redis.PortalRedisReadTemplate;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 门户子系统客户端自动配置。
 * <p>
 * 子系统引入依赖后配置 {@code portal.subsystem.*} 即可使用 OAuth、权限读取、扩展缓存等能力。
 * <p>
 * 同时兼容 Spring Boot 2（spring.factories）与 Boot 3（AutoConfiguration.imports）。
 */
@Configuration
@ConditionalOnProperty(prefix = "portal.subsystem", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PortalSubSystemProperties.class)
public class PortalSubSystemAutoConfiguration {

    @Bean(name = "portalSubSystemObjectMapper")
    @ConditionalOnMissingBean(name = "portalSubSystemObjectMapper")
    public ObjectMapper portalSubSystemObjectMapper() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean(name = "portalSubSystemOkHttpClient")
    @ConditionalOnMissingBean(name = "portalSubSystemOkHttpClient")
    public OkHttpClient portalSubSystemOkHttpClient(PortalSubSystemProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getHttp().getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getHttp().getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getHttp().getWriteTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalSubSystemHttpClient portalSubSystemHttpClient(
            PortalSubSystemProperties properties,
            @Qualifier("portalSubSystemOkHttpClient") OkHttpClient portalSubSystemOkHttpClient,
            @Qualifier("portalSubSystemObjectMapper") ObjectMapper portalSubSystemObjectMapper) {
        return new PortalSubSystemHttpClient(properties, portalSubSystemOkHttpClient, portalSubSystemObjectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalOAuthClient portalOAuthClient(PortalSubSystemProperties properties,
                                               PortalSubSystemHttpClient portalSubSystemHttpClient) {
        return new PortalOAuthClient(properties, portalSubSystemHttpClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalMachineAuthClient portalMachineAuthClient(PortalSubSystemProperties properties,
                                                           PortalSubSystemHttpClient portalSubSystemHttpClient) {
        return new PortalMachineAuthClient(properties, portalSubSystemHttpClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalRedisReadTemplate portalRedisReadTemplate(
            PortalSubSystemProperties properties,
            @Qualifier("portalSubSystemObjectMapper") ObjectMapper portalSubSystemObjectMapper,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
        return new PortalRedisReadTemplate(properties, portalSubSystemObjectMapper,
                stringRedisTemplateProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalPermissionReader portalPermissionReader(PortalSubSystemProperties properties,
                                                         PortalSubSystemHttpClient portalSubSystemHttpClient,
                                                         PortalRedisReadTemplate portalRedisReadTemplate) {
        return new PortalPermissionReader(properties, portalSubSystemHttpClient, portalRedisReadTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalCacheReader portalCacheReader(PortalSubSystemProperties properties,
                                               PortalSubSystemHttpClient portalSubSystemHttpClient,
                                               PortalRedisReadTemplate portalRedisReadTemplate) {
        return new PortalCacheReader(properties, portalSubSystemHttpClient, portalRedisReadTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public PortalCacheWriter portalCacheWriter(PortalSubSystemProperties properties,
                                               PortalSubSystemHttpClient portalSubSystemHttpClient,
                                               PortalOAuthClient portalOAuthClient) {
        return new PortalCacheWriter(properties, portalSubSystemHttpClient, portalOAuthClient);
    }

}
