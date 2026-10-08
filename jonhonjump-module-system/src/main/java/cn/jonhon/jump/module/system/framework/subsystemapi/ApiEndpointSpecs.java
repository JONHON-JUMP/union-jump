package cn.jonhon.jump.module.system.framework.subsystemapi;

import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口用途常量 + 接口行 → EndpointSpec 转换
 *
 * purpose 与适配器类型无关（camstar/http 通用），新增接口类型只需新增一行接口记录。
 */
public final class ApiEndpointSpecs {

    public static final String QUERY = "query";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String TEAM_COMBO = "team_combo";
    public static final String ROLE_QUERY = "role_query";
    public static final String ROLE_CREATE = "role_create";
    public static final String ROLE_DELETE = "role_delete";

    /** 前端分组：角色类用途 → role，其余非空用途 → person，空用途 → other */
    public static String groupOf(String purpose) {
        if (purpose == null || purpose.trim().isEmpty()) {
            return "other";
        }
        return purpose.startsWith("role_") ? "role" : "person";
    }

    /** 接口行 → EndpointSpec；null 行返回 null（可选接口未配置） */
    public static EndpointSpec toSpec(SubSystemApiEndpointDO endpoint) {
        if (endpoint == null) {
            return null;
        }
        boolean enabled = endpoint.getEnabled() == null || endpoint.getEnabled() == 1;
        boolean withSession = endpoint.getWithSession() == null || endpoint.getWithSession() == 1;
        EndpointSpec spec = EndpointSpec.of(endpoint.getUrl(), endpoint.getMethod(), enabled, withSession);
        spec.setName(endpoint.getName());
        return spec;
    }

    /** 接口行列表 → purpose → EndpointSpec（空 purpose 的自定义行跳过） */
    public static Map<String, EndpointSpec> toSpecMap(List<SubSystemApiEndpointDO> endpoints) {
        Map<String, EndpointSpec> map = new HashMap<>();
        if (endpoints == null) {
            return map;
        }
        for (SubSystemApiEndpointDO endpoint : endpoints) {
            if (endpoint == null || endpoint.getPurpose() == null || endpoint.getPurpose().trim().isEmpty()) {
                continue;
            }
            map.put(endpoint.getPurpose().trim(), toSpec(endpoint));
        }
        return map;
    }

    private ApiEndpointSpecs() {
    }

}
