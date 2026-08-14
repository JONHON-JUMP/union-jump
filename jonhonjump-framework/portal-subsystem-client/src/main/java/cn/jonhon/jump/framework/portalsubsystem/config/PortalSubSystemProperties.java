package cn.jonhon.jump.framework.portalsubsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 门户子系统客户端配置。
 *
 * <pre>
 * portal:
 *   subsystem:
 *     enabled: true
 *     client-id: scada
 *     sub-system-id: 1
 *     tenant-id: 1
 *     api-url: http://10.17.65.11:48080/admin-api
 *     portal-url: http://10.17.65.11:48080
 *     read-mode: redis
 *     write-mode: http
 *     oauth:
 *       client-secret: xxx
 *       redirect-uri: http://scada/callback
 *       scope: user.read subsystem.cache.read subsystem.cache.write
 *       state: scada
 *     redis:
 *       host: 10.1.19.35
 *       port: 6379
 *       database: 2
 *       password:
 *     cache:
 *       allowed-namespaces: device,session,alarm
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "portal.subsystem")
public class PortalSubSystemProperties {

    /** 是否启用 */
    private boolean enabled = true;

    /** 子系统 OAuth client_id，如 scada、mes */
    private String clientId;

    /** 子系统在主系统的 sub_system.id */
    private Long subSystemId;

    /** 租户编号 */
    private Long tenantId = 1L;

    /** 主系统后端 API 根地址，如 http://host:48080/admin-api */
    private String apiUrl;

    /** 主系统前端地址，用于构建 SSO 授权页 */
    private String portalUrl;

    /** 读模式：redis（只读直连）或 http */
    private String readMode = "redis";

    /** 写模式：固定 http */
    private String writeMode = "http";

    /** API 版本路径段，如 v1 */
    private String apiVersion = "v1";

    private OAuth oauth = new OAuth();
    private Redis redis = new Redis();
    private Cache cache = new Cache();
    private Http http = new Http();

    @Data
    public static class OAuth {
        private String clientSecret;
        private String redirectUri;
        private String scope = "user.read";
        private String state = "subsystem";
    }

    @Data
    public static class Redis {
        /** 是否启用 Redis 只读（read-mode=redis 时需要） */
        private boolean enabled = true;
        private String host;
        private int port = 6379;
        private int database = 0;
        private String password;
    }

    @Data
    public static class Cache {
        /** 本子系统允许使用的扩展缓存 namespace（客户端侧校验，最终以主系统为准） */
        private List<String> allowedNamespaces = new ArrayList<>();
    }

    @Data
    public static class Http {
        private long connectTimeoutMs = 10_000;
        private long readTimeoutMs = 30_000;
        private long writeTimeoutMs = 30_000;
    }

}
