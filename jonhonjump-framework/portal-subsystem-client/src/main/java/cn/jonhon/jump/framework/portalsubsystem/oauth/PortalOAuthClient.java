package cn.jonhon.jump.framework.portalsubsystem.oauth;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.http.PortalSubSystemHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 门户 OAuth2 单点登录客户端。
 * <p>
 * 负责授权页地址构建、授权码换 token、获取用户信息、token 刷新。
 */
@Slf4j
public class PortalOAuthClient {

    private final PortalSubSystemProperties properties;
    private final PortalSubSystemHttpClient httpClient;

    /** 当前会话 access_token（用于扩展缓存写等 API 调用） */
    private volatile String currentAccessToken;
    private volatile String currentRefreshToken;

    public PortalOAuthClient(PortalSubSystemProperties properties, PortalSubSystemHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    /**
     * 构建门户 SSO 授权页地址。
     */
    public String buildAuthorizeUrl() {
        String portalUrl = trimTrailingSlash(properties.getPortalUrl());
        PortalSubSystemProperties.OAuth oauth = properties.getOauth();
        return portalUrl + "/sso?client_id=" + properties.getClientId()
                + "&redirect_uri=" + urlEncode(oauth.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + urlEncode(oauth.getScope())
                + "&state=" + urlEncode(oauth.getState());
    }

    /**
     * 授权码换取 access_token，并缓存到当前客户端实例。
     */
    public PortalOAuthToken exchangeCodeForToken(String code, String state) {
        validateState(state);
        Map<String, String> form = new HashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", properties.getOauth().getRedirectUri());
        form.put("state", normalizeState(state));
        String responseBody = httpClient.postForm("/system/oauth2/token", form, true);
        PortalOAuthToken token = parseTokenResponse(responseBody, "授权码换 token");
        cacheToken(token);
        return token;
    }

    /**
     * 授权码登录：换 token 并返回 username（SSO 最常用入口）。
     */
    public String exchangeCodeForUsername(String code, String state) {
        PortalOAuthToken token = exchangeCodeForToken(code, state);
        PortalUserInfo userInfo = getUserInfo(token.getAccessToken());
        if (userInfo == null || userInfo.getUsername() == null) {
            throw new PortalSubSystemException("门户未返回 username");
        }
        return userInfo.getUsername();
    }

    /**
     * 获取主系统用户基本信息。
     */
    public PortalUserInfo getUserInfo(String accessToken) {
        return httpClient.getForData("/system/oauth2/user/get", accessToken, PortalUserInfo.class);
    }

    /**
     * 刷新 access_token。
     */
    public PortalOAuthToken refreshToken(String refreshToken) {
        Map<String, String> form = new HashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("refresh_token", refreshToken);
        String responseBody = httpClient.postForm("/system/oauth2/token", form, true);
        PortalOAuthToken token = parseTokenResponse(responseBody, "刷新 token");
        cacheToken(token);
        return token;
    }

    /**
     * 获取当前缓存的 access_token；若即将过期可先 refresh。
     */
    public String getCurrentAccessToken() {
        return currentAccessToken;
    }

    public void cacheToken(PortalOAuthToken token) {
        if (token != null) {
            this.currentAccessToken = token.getAccessToken();
            this.currentRefreshToken = token.getRefreshToken();
        }
    }

    /**
     * 确保有可用 token：若无则抛异常；若 API 返回 401 可调用方触发 refresh 后重试。
     */
    public String ensureAccessToken() {
        if (currentAccessToken == null) {
            throw new PortalSubSystemException("未登录门户 OAuth，请先完成 SSO 或设置 access_token");
        }
        return currentAccessToken;
    }

    public PortalOAuthToken refreshCurrentToken() {
        if (currentRefreshToken == null) {
            throw new PortalSubSystemException("无 refresh_token，请重新 SSO 登录");
        }
        return refreshToken(currentRefreshToken);
    }

    private PortalOAuthToken parseTokenResponse(String responseBody, String action) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                String msg = root.path("msg").asText();
                if (msg != null && msg.contains("redirect_uri")) {
                    throw new PortalSubSystemException(action + "失败：" + msg
                            + "。请在门户 OAuth2 应用管理中为 client_id=" + properties.getClientId()
                            + " 添加回调地址：" + properties.getOauth().getRedirectUri());
                }
                throw new PortalSubSystemException(action + "失败：" + msg);
            }
            JsonNode data = root.get("data");
            if (data == null || data.get("access_token") == null) {
                throw new PortalSubSystemException(action + "失败：未返回 access_token");
            }
            PortalOAuthToken token = new PortalOAuthToken();
            token.setAccessToken(data.get("access_token").asText());
            if (data.has("refresh_token")) {
                token.setRefreshToken(data.get("refresh_token").asText());
            }
            if (data.has("expires_in")) {
                token.setExpiresIn(data.get("expires_in").asLong());
            }
            if (data.has("token_type")) {
                token.setTokenType(data.get("token_type").asText());
            }
            return token;
        } catch (PortalSubSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new PortalSubSystemException(action + "失败：响应解析异常", e);
        }
    }

    private void validateState(String state) {
        String expected = properties.getOauth().getState();
        String actual = normalizeState(state);
        if (expected == null || expected.isEmpty() || actual.isEmpty()) {
            return;
        }
        if (!expected.equals(actual)) {
            log.warn("SSO state 不匹配, expected={}, received={}", expected, actual);
            throw new PortalSubSystemException("SSO state 校验失败");
        }
    }

    private static String normalizeState(String state) {
        return state == null ? "" : state;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

}
