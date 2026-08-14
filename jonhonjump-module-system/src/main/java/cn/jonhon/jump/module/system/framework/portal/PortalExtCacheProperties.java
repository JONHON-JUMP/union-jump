package cn.jonhon.jump.module.system.framework.portal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 子系统扩展缓存白名单与限制（主系统代写校验）
 *
 * <pre>
 * jonhon:
 *   portal:
 *     ext-cache:
 *       max-value-bytes: 65536
 *       max-ttl-seconds: 604800
 *       clients:
 *         scada: device,session,alarm
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "jonhon.portal.ext-cache")
@Data
public class PortalExtCacheProperties {

    /** 单条 value 最大字节数，默认 64KB */
    private int maxValueBytes = 64 * 1024;

    /** TTL 上限秒数，默认 7 天 */
    private long maxTtlSeconds = 7 * 24 * 3600L;

    /** clientId → 允许的 namespace 列表（逗号分隔字符串或 YAML list） */
    private Map<String, Object> clients = new HashMap<>();

    public Set<String> getAllowedNamespaces(String clientId) {
        if (clientId == null || clients == null) {
            return Collections.emptySet();
        }
        Object raw = clients.get(clientId);
        if (raw == null) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        if (raw instanceof String) {
            for (String part : ((String) raw).split(",")) {
                if (part != null && !part.trim().isEmpty()) {
                    result.add(part.trim());
                }
            }
        } else if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    result.add(String.valueOf(item).trim());
                }
            }
        }
        return result;
    }

}
