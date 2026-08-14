package cn.jonhon.jump.framework.portalsubsystem.auth;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 机端 / 独立登录：凭 clientId + clientSecret 调主系统刷卡校验与权限查询（无需用户 Token）。
 */
public class PortalMachineAuthClient {

    private static final String CARD_LOGIN_PATH = "/system/oauth2/subsystem/v1/card-login";
    private static final String PERMISSION_INFO_PATH = "/system/oauth2/subsystem/v1/permission-info";

    private final PortalSubSystemProperties properties;
    private final PortalSubSystemHttpClient httpClient;

    public PortalMachineAuthClient(PortalSubSystemProperties properties,
                                   PortalSubSystemHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    /**
     * 刷卡花名册校验。
     */
    public PortalCardLoginResult cardLogin(String username) {
        return httpClient.postJsonForDataNoAuth(CARD_LOGIN_PATH, buildBody(username), PortalCardLoginResult.class);
    }

    /**
     * 按用户名拉取角色、权限标识、菜单树（独立登录 / 门户登录后灌会话用）。
     */
    public PortalPermissionInfoResult getPermissionInfo(String username) {
        return httpClient.postJsonForDataNoAuth(PERMISSION_INFO_PATH, buildBody(username),
                PortalPermissionInfoResult.class);
    }

    private Map<String, String> buildBody(String username) {
        if (!StringUtils.hasText(username)) {
            throw new PortalSubSystemException("username 不能为空");
        }
        if (!StringUtils.hasText(properties.getClientId())) {
            throw new PortalSubSystemException("portal.subsystem.client-id 未配置");
        }
        String secret = properties.getOauth() != null ? properties.getOauth().getClientSecret() : null;
        if (!StringUtils.hasText(secret)) {
            throw new PortalSubSystemException("portal.subsystem.oauth.client-secret 未配置");
        }
        Map<String, String> body = new HashMap<>();
        body.put("clientId", properties.getClientId());
        body.put("clientSecret", secret);
        body.put("username", username.trim());
        return body;
    }

}
