package cn.jonhon.jump.module.system.dal.redis.portal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 主系统写入的权限包 key 必须和子系统客户端 PortalRedisKeyConstants 一致。
 * 客户端现行 key：portal:perm:context:{username}:{clientId}
 * 客户端过渡 key：portal:perm:context:{tenantId}:{userId}:{subSystemId}
 */
public class PortalPermContextRedisDAOKeyTest {

    @Test
    public void canonicalKeyMatchesSubsystemClient() {
        assertEquals("portal:perm:context:zhangsan:mes-5500",
                PortalPermContextRedisDAO.formatKey("zhangsan", "mes-5500"));
    }

    @Test
    public void legacyKeyMatchesDeployedSubsystemClient() {
        assertEquals("portal:perm:context:1:10:20",
                PortalPermContextRedisDAO.formatLegacyKey(1L, 10L, 20L));
        assertEquals("portal:perm:context:1:10:20",
                PortalPermContextRedisDAO.formatLegacyKey(null, 10L, 20L));
    }

}
