package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleMapper;
import cn.jonhon.jump.module.system.enums.permission.RoleCodeEnum;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_NO_PERMISSION;

/**
 * 业务系统管理页面的"可管系统"范围 Service 实现类
 */
@Service
@Slf4j
public class SubSystemAccessServiceImpl implements SubSystemAccessService {

    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public Set<Long> getAllowedSubSystemIds(Long userId) {
        // 无登录上下文（内部调用，如建主用户联动登记花名册）不受限
        if (userId == null) {
            return null;
        }
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleIds)) {
            // 没有任何角色（正常不会发生，登录即有角色）：从严处理为空集合
            return Collections.emptySet();
        }
        List<RoleDO> roles = roleMapper.selectByIds(roleIds);
        Set<Long> allowed = new HashSet<>();
        boolean hasEnabledRole = false;
        for (RoleDO role : roles) {
            // 停用角色不参与计算
            if (!CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
                continue;
            }
            hasEnabledRole = true;
            // 超级管理员始终不限，避免再挂一个车间角色后被收窄
            if (RoleCodeEnum.isSuperAdmin(role.getCode())) {
                return null;
            }
            // 留空 = 该角色不限制系统，不能把别人已经勾选的范围放大成全部
            if (CollUtil.isNotEmpty(role.getSubSystemIds())) {
                allowed.addAll(role.getSubSystemIds());
            }
        }
        if (!hasEnabledRole) {
            return Collections.emptySet();
        }
        // 所有启用角色都没勾可管系统（普通角色、存量角色）→ 不限
        return allowed.isEmpty() ? null : allowed;
    }

    @Override
    public void checkAccessible(Long subSystemId) {
        if (subSystemId == null) {
            return;
        }
        Set<Long> allowed = getAllowedSubSystemIds(SecurityFrameworkUtils.getLoginUserId());
        if (allowed == null || allowed.contains(subSystemId)) {
            return;
        }
        throw exception(SUB_SYSTEM_NO_PERMISSION);
    }

    @Override
    public boolean isUnrestricted() {
        return getAllowedSubSystemIds(SecurityFrameworkUtils.getLoginUserId()) == null;
    }

}
