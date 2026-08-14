package cn.jonhon.jump.module.system.controller.admin.oauth2;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.framework.security.config.SecurityProperties;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.framework.tenant.core.context.TenantContextHolder;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalExtCacheExpireReqVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.redis.portal.PortalExtCacheRedisDAO;
import cn.jonhon.jump.module.system.framework.portal.PortalExtCacheProperties;
import cn.jonhon.jump.module.system.service.oauth2.OAuth2ClientService;
import cn.jonhon.jump.module.system.service.oauth2.OAuth2TokenService;
import cn.jonhon.jump.module.system.service.user.SubSystemPermissionContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

/**
 * 子系统 Redis Gateway：权限 context + 扩展缓存代写。
 * <p>
 * 路径与 portal-subsystem-client 约定一致：/system/oauth2/subsystem/redis/v1/**
 */
@Tag(name = "管理后台 - OAuth2 子系统 Redis Gateway")
@RestController
@RequestMapping("/system/oauth2/subsystem/redis/v1")
@Validated
@Slf4j
public class OAuth2SubSystemRedisController {

    private static final long DEFAULT_TTL_SECONDS = 3600L;

    @Resource
    private SubSystemPermissionContextService permissionContextService;
    @Resource
    private PortalExtCacheRedisDAO portalExtCacheRedisDAO;
    @Resource
    private PortalExtCacheProperties portalExtCacheProperties;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SecurityProperties securityProperties;

    @GetMapping("/context")
    @Operation(summary = "读取子系统权限包（roles + permissions）")
    @PreAuthorize("@ss.hasScope('user.read')")
    public CommonResult<PortalPermContextRespVO> getContext(@RequestParam("clientId") String clientId,
                                                            HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        assertClientMatch(tokenClientId, clientId);
        SubSystemDO subSystem = requireSubSystemByClientId(clientId);
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        PortalPermContextRespVO context = permissionContextService.getOrRebuild(
                tenantId, getLoginUserId(), subSystem.getId());
        return success(context);
    }

    @PostMapping("/context/rebuild")
    @Operation(summary = "强制重建权限包（管理用；子系统也可在 miss 后间接触发）")
    @PreAuthorize("@ss.hasScope('user.read')")
    public CommonResult<PortalPermContextRespVO> rebuildContext(@RequestParam("clientId") String clientId,
                                                                HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        assertClientMatch(tokenClientId, clientId);
        SubSystemDO subSystem = requireSubSystemByClientId(clientId);
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        return success(permissionContextService.rebuildAndCache(tenantId, getLoginUserId(), subSystem.getId()));
    }

    @DeleteMapping("/context/evict")
    @Operation(summary = "失效权限包（管理用）")
    @PreAuthorize("@ss.hasPermission('system:sub-system:update')")
    public CommonResult<Boolean> evictContext(@RequestParam("mainUserId") Long mainUserId,
                                              @RequestParam("subSystemId") Long subSystemId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        permissionContextService.evict(tenantId, mainUserId, subSystemId);
        return success(true);
    }

    @GetMapping("/cache/{namespace}/{key}")
    @Operation(summary = "读扩展缓存")
    @PreAuthorize("@ss.hasAnyScopes('user.read', 'subsystem.cache.read')")
    public CommonResult<Object> getCache(@PathVariable("namespace") String namespace,
                                         @PathVariable("key") String key,
                                         @RequestParam(value = "clientId", required = false) String clientId,
                                         HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        String resolvedClientId = resolveClientId(tokenClientId, clientId);
        validateNamespaceAndKey(resolvedClientId, namespace, key);
        String json = portalExtCacheRedisDAO.get(resolvedClientId, namespace, key);
        if (json == null) {
            return success(null);
        }
        return success(JsonUtils.parseObject(json, Object.class));
    }

    @PutMapping("/cache/{namespace}/{key}")
    @Operation(summary = "写扩展缓存（主系统代写）")
    @PreAuthorize("@ss.hasAnyScopes('user.read', 'subsystem.cache.write')")
    public CommonResult<Boolean> putCache(@PathVariable("namespace") String namespace,
                                          @PathVariable("key") String key,
                                          @RequestParam(value = "clientId", required = false) String clientId,
                                          @RequestBody String rawBody,
                                          HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        String resolvedClientId = resolveClientId(tokenClientId, clientId);
        validateNamespaceAndKey(resolvedClientId, namespace, key);
        ParsedCacheBody parsed = parseCacheBody(rawBody);
        validateTtl(parsed.ttlSeconds);
        validateValueSize(parsed.jsonValue);
        portalExtCacheRedisDAO.set(resolvedClientId, namespace, key, parsed.jsonValue, parsed.ttlSeconds);
        return success(true);
    }

    @DeleteMapping("/cache/{namespace}/{key}")
    @Operation(summary = "删扩展缓存")
    @PreAuthorize("@ss.hasAnyScopes('user.read', 'subsystem.cache.write')")
    public CommonResult<Boolean> deleteCache(@PathVariable("namespace") String namespace,
                                             @PathVariable("key") String key,
                                             @RequestParam(value = "clientId", required = false) String clientId,
                                             HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        String resolvedClientId = resolveClientId(tokenClientId, clientId);
        validateNamespaceAndKey(resolvedClientId, namespace, key);
        portalExtCacheRedisDAO.delete(resolvedClientId, namespace, key);
        return success(true);
    }

    @PostMapping("/cache/{namespace}/{key}/expire")
    @Operation(summary = "续期扩展缓存")
    @PreAuthorize("@ss.hasAnyScopes('user.read', 'subsystem.cache.write')")
    public CommonResult<Boolean> expireCache(@PathVariable("namespace") String namespace,
                                             @PathVariable("key") String key,
                                             @RequestParam(value = "clientId", required = false) String clientId,
                                             @Valid @RequestBody PortalExtCacheExpireReqVO reqVO,
                                             HttpServletRequest request) {
        String tokenClientId = requireTokenClientId(request);
        String resolvedClientId = resolveClientId(tokenClientId, clientId);
        validateNamespaceAndKey(resolvedClientId, namespace, key);
        validateTtl(reqVO.getTtlSeconds());
        portalExtCacheRedisDAO.expire(resolvedClientId, namespace, key, reqVO.getTtlSeconds());
        return success(true);
    }

    private String resolveClientId(String tokenClientId, String requestClientId) {
        if (StrUtil.isNotBlank(requestClientId)) {
            assertClientMatch(tokenClientId, requestClientId);
            return requestClientId;
        }
        return tokenClientId;
    }

    private ParsedCacheBody parseCacheBody(String rawBody) {
        long ttl = DEFAULT_TTL_SECONDS;
        String jsonValue;
        if (StrUtil.isBlank(rawBody)) {
            throw exception(BAD_REQUEST);
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = JsonUtils.parseTree(rawBody);
            if (root != null && root.isObject() && root.has("value")) {
                if (root.has("ttlSeconds") && !root.get("ttlSeconds").isNull()) {
                    ttl = root.get("ttlSeconds").asLong();
                }
                com.fasterxml.jackson.databind.JsonNode valueNode = root.get("value");
                jsonValue = valueNode == null || valueNode.isNull() ? "null" : valueNode.toString();
            } else {
                jsonValue = rawBody;
            }
        } catch (Exception ex) {
            jsonValue = rawBody;
        }
        return new ParsedCacheBody(jsonValue, ttl);
    }

    private static final class ParsedCacheBody {
        private final String jsonValue;
        private final long ttlSeconds;

        private ParsedCacheBody(String jsonValue, long ttlSeconds) {
            this.jsonValue = jsonValue;
            this.ttlSeconds = ttlSeconds;
        }
    }

    private String requireTokenClientId(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(
                request, securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        OAuth2AccessTokenDO accessToken = oauth2TokenService.checkAccessToken(token);
        return accessToken.getClientId();
    }

    private void assertClientMatch(String tokenClientId, String requestClientId) {
        if (!StrUtil.equals(tokenClientId, requestClientId)) {
            throw exception(PORTAL_PERM_CLIENT_MISMATCH);
        }
    }

    private SubSystemDO requireSubSystemByClientId(String clientId) {
        OAuth2ClientDO client = oauth2ClientService.getOAuth2ClientFromCache(clientId);
        if (client == null) {
            throw exception(SUB_SYSTEM_OAUTH2_CLIENT_NOT_EXISTS);
        }
        SubSystemDO subSystem = subSystemMapper.selectByOauth2ClientId(client.getId());
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        return subSystem;
    }

    private void validateNamespaceAndKey(String clientId, String namespace, String key) {
        Set<String> allowed = portalExtCacheProperties.getAllowedNamespaces(clientId);
        if (allowed.isEmpty() || !allowed.contains(namespace)) {
            throw exception(PORTAL_EXT_NAMESPACE_FORBIDDEN);
        }
        if (StrUtil.isBlank(key) || key.contains("..") || key.contains("/") || key.contains("\\")) {
            throw exception(PORTAL_EXT_KEY_INVALID);
        }
    }

    private void validateValueSize(String json) {
        if (json != null && json.getBytes(StandardCharsets.UTF_8).length > portalExtCacheProperties.getMaxValueBytes()) {
            throw exception(PORTAL_EXT_VALUE_TOO_LARGE);
        }
    }

    private void validateTtl(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0 || ttlSeconds > portalExtCacheProperties.getMaxTtlSeconds()) {
            throw exception(PORTAL_EXT_TTL_INVALID);
        }
    }

}
