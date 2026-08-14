package cn.jonhon.jump.framework.portalsubsystem.permission;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 子系统权限上下文（roles + permissions）。
 */
@Data
public class PortalPermissionContext {

    private String username;
    private String clientId;
    private Long userId;
    private Long tenantId;
    private Long subSystemId;
    private List<PortalPermissionRole> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    public List<String> getRoleCodes() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(PortalPermissionRole::getCode).collect(Collectors.toList());
    }

}
