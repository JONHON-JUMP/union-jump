package cn.jonhon.jump.framework.minio.config;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.minio.core.MinioTemplate;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MinIO auto configuration.
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "jonhonjump.minio", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MinioProperties.class)
public class JonhonjumpMinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey());
        if (StrUtil.isNotBlank(properties.getRegion())) {
            builder.region(properties.getRegion());
        }
        MinioClient minioClient = builder.build();
        if (properties.getConnectTimeout() != null
                || properties.getWriteTimeout() != null
                || properties.getReadTimeout() != null) {
            minioClient.setTimeout(
                    defaultTimeout(properties.getConnectTimeout()),
                    defaultTimeout(properties.getWriteTimeout()),
                    defaultTimeout(properties.getReadTimeout()));
        }
        return minioClient;
    }

    @Bean
    @ConditionalOnMissingBean
    public MinioTemplate minioTemplate(MinioClient minioClient, MinioProperties properties) {
        return new MinioTemplate(minioClient, properties.getBucketName());
    }

    private static long defaultTimeout(Long timeout) {
        return timeout != null ? timeout : 0L;
    }

}
