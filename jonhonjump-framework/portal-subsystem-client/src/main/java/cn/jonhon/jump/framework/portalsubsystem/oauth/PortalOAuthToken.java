package cn.jonhon.jump.framework.portalsubsystem.oauth;

import lombok.Data;

/**
 * OAuth2 令牌。
 */
@Data
public class PortalOAuthToken {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;

}
