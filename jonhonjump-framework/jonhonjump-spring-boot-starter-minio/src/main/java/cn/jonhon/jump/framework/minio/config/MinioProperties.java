package cn.jonhon.jump.framework.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

/**
 * MinIO properties.
 */
@ConfigurationProperties(prefix = "jonhonjump.minio")
@Data
@Validated
public class MinioProperties {

    /**
     * Whether MinIO is enabled.
     */
    private Boolean enabled = false;

    /**
     * MinIO endpoint, for example: http://127.0.0.1:9000.
     */
    @NotEmpty(message = "MinIO endpoint must not be empty")
    private String endpoint;

    /**
     * Access key.
     */
    @NotEmpty(message = "MinIO accessKey must not be empty")
    private String accessKey;

    /**
     * Secret key.
     */
    @NotEmpty(message = "MinIO secretKey must not be empty")
    private String secretKey;

    /**
     * Default bucket.
     */
    private String bucketName;

    /**
     * Optional region.
     */
    private String region;

    /**
     * Connect timeout in milliseconds.
     */
    private Long connectTimeout;

    /**
     * Write timeout in milliseconds.
     */
    private Long writeTimeout;

    /**
     * Read timeout in milliseconds.
     */
    private Long readTimeout;

}
