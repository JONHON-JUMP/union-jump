package cn.jonhon.jump.framework.portalsubsystem.http;

import cn.jonhon.jump.framework.portalsubsystem.config.PortalSubSystemProperties;
import cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException;
import cn.jonhon.jump.framework.portalsubsystem.model.PortalApiResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 调用主系统门户 API 的 HTTP 客户端。
 */
@Slf4j
public class PortalSubSystemHttpClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final PortalSubSystemProperties properties;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public PortalSubSystemHttpClient(PortalSubSystemProperties properties,
                                     OkHttpClient okHttpClient,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    public String get(String path, String accessToken) {
        return execute(buildRequest(path, "GET", null, accessToken, true));
    }

    public String postJson(String path, String jsonBody, String accessToken) {
        RequestBody body = RequestBody.create(JSON, jsonBody == null ? "{}" : jsonBody);
        return execute(buildRequest(path, "POST", body, accessToken, true));
    }

    /**
     * POST JSON，不带 Bearer / Basic（用于机端刷卡等白名单接口，鉴权信息在 Body）。
     */
    public String postJsonNoAuth(String path, String jsonBody) {
        RequestBody body = RequestBody.create(JSON, jsonBody == null ? "{}" : jsonBody);
        return execute(buildRequest(path, "POST", body, null, false, false));
    }

    public <T> T postJsonForDataNoAuth(String path, Object requestBody, Class<T> dataType) {
        try {
            String json = objectMapper.writeValueAsString(requestBody == null ? new Object() : requestBody);
            return parseData(postJsonNoAuth(path, json), dataType, "POST " + path);
        } catch (PortalSubSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new PortalSubSystemException("POST " + path + " 失败：请求序列化异常", e);
        }
    }

    public String putJson(String path, String jsonBody, String accessToken) {
        RequestBody body = RequestBody.create(JSON, jsonBody == null ? "" : jsonBody);
        return execute(buildRequest(path, "PUT", body, accessToken, true));
    }

    public String delete(String path, String accessToken) {
        return execute(buildRequest(path, "DELETE", null, accessToken, true));
    }

    public void putJsonRequireSuccess(String path, String jsonBody, String accessToken) {
        parseData(putJson(path, jsonBody, accessToken), Boolean.class, "PUT " + path);
    }

    public void deleteRequireSuccess(String path, String accessToken) {
        parseData(delete(path, accessToken), Boolean.class, "DELETE " + path);
    }

    public void postJsonRequireSuccess(String path, String jsonBody, String accessToken) {
        parseData(postJson(path, jsonBody, accessToken), Boolean.class, "POST " + path);
    }

    public String postForm(String path, Map<String, String> form, boolean basicAuth) {
        okhttp3.FormBody.Builder builder = new okhttp3.FormBody.Builder();
        if (form != null) {
            form.forEach(builder::add);
        }
        return execute(buildRequest(path, "POST", builder.build(), null, !basicAuth));
    }

    public <T> T getForData(String path, String accessToken, Class<T> dataType) {
        return parseData(get(path, accessToken), dataType, "GET " + path);
    }

    public <T> T getForData(String path, String accessToken, TypeReference<PortalApiResult<T>> typeRef) {
        return parseData(get(path, accessToken), typeRef, "GET " + path);
    }

    public String buildApiUrl(String path) {
        String base = trimTrailingSlash(properties.getApiUrl());
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    public String buildSubsystemApiPath(String suffix) {
        String version = properties.getApiVersion();
        String normalized = suffix.startsWith("/") ? suffix : "/" + suffix;
        return "/system/oauth2/subsystem/redis/" + version + normalized;
    }

    private Request buildRequest(String path, String method, RequestBody body,
                                 String accessToken, boolean bearerAuth) {
        return buildRequest(path, method, body, accessToken, bearerAuth, true);
    }

    /**
     * @param sendAuth false 时不发 Authorization（白名单机端接口）
     */
    private Request buildRequest(String path, String method, RequestBody body,
                                 String accessToken, boolean bearerAuth, boolean sendAuth) {
        Request.Builder builder = new Request.Builder().url(buildApiUrl(path));
        if (properties.getTenantId() != null) {
            builder.addHeader("tenant-id", String.valueOf(properties.getTenantId()));
        }
        if (sendAuth) {
            if (bearerAuth && accessToken != null) {
                builder.addHeader("Authorization", "Bearer " + accessToken);
            } else if (!bearerAuth) {
                String credentials = properties.getClientId() + ":" + properties.getOauth().getClientSecret();
                String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                builder.addHeader("Authorization", "Basic " + basicAuth);
            }
        }
        switch (method) {
            case "GET":
                builder.get();
                break;
            case "POST":
                builder.post(body != null ? body : RequestBody.create((MediaType) null, new byte[0]));
                break;
            case "PUT":
                builder.put(body != null ? body : RequestBody.create((MediaType) null, new byte[0]));
                break;
            case "DELETE":
                builder.delete();
                break;
            default:
                throw new PortalSubSystemException("不支持的 HTTP 方法：" + method);
        }
        return builder.build();
    }

    private String execute(Request request) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.body() == null) {
                throw new PortalSubSystemException("门户接口无响应体：" + request.url());
            }
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new PortalSubSystemException("门户接口 HTTP " + response.code()
                        + "：" + request.url() + " body=" + truncate(body));
            }
            return body;
        } catch (PortalSubSystemException e) {
            throw e;
        } catch (IOException e) {
            log.error("调用门户接口异常：{}", request.url(), e);
            throw new PortalSubSystemException("调用门户接口异常：" + e.getMessage(), e);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    private <T> T parseData(String responseBody, Class<T> dataType, String action) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            int code = root.path("code").asInt(-1);
            if (code != 0) {
                throw new PortalSubSystemException(action + "失败：" + root.path("msg").asText());
            }
            JsonNode dataNode = root.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return null;
            }
            return objectMapper.treeToValue(dataNode, dataType);
        } catch (PortalSubSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new PortalSubSystemException(action + "失败：响应解析异常", e);
        }
    }

    private <T> T parseData(String responseBody, TypeReference<PortalApiResult<T>> typeRef, String action) {
        try {
            PortalApiResult<T> result = objectMapper.readValue(responseBody, typeRef);
            return PortalApiResult.extractData(result, action);
        } catch (PortalSubSystemException e) {
            throw e;
        } catch (Exception e) {
            throw new PortalSubSystemException(action + "失败：响应解析异常", e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
