package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.portal.UserPortalDefaultRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.UserExternalSystemRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserPortalDefaultDO;
import cn.jonhon.jump.module.system.dal.mysql.user.UserPortalDefaultMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.USER_PORTAL_DEFAULT_SYSTEM_INVALID;

/**
 * 用户门户默认打开系统 Service 实现
 */
@Service
@Validated
public class UserPortalDefaultServiceImpl implements UserPortalDefaultService {

    public static final String DEFAULT_SYSTEM_MAIN = "main";

    @Resource
    private UserPortalDefaultMapper userPortalDefaultMapper;
    @Resource
    private SubSystemUsersService subSystemUsersService;

    @Override
    public UserPortalDefaultRespVO getUserPortalDefault(Long userId) {
        List<UserExternalSystemRespVO> systems = subSystemUsersService.getMyExternalSystemList(userId);
        UserPortalDefaultRespVO ruleDefault = buildRuleDefault(systems);

        UserPortalDefaultDO config = userPortalDefaultMapper.selectByUserId(userId);
        if (config != null) {
            if (config.getSubSystemId() == null) {
                return new UserPortalDefaultRespVO(null, DEFAULT_SYSTEM_MAIN, true);
            }
            UserExternalSystemRespVO matched = findSystemBySubSystemId(systems, config.getSubSystemId());
            if (matched != null) {
                return new UserPortalDefaultRespVO(matched.getSubSystemId(), matched.getClientId(), true);
            }
        }
        return ruleDefault;
    }

    @Override
    public void saveUserPortalDefault(Long userId, Long subSystemId) {
        if (subSystemId != null) {
            List<UserExternalSystemRespVO> systems = subSystemUsersService.getMyExternalSystemList(userId);
            if (findSystemBySubSystemId(systems, subSystemId) == null) {
                throw exception(USER_PORTAL_DEFAULT_SYSTEM_INVALID);
            }
        }

        UserPortalDefaultDO existing = userPortalDefaultMapper.selectByUserId(userId);
        if (existing == null) {
            UserPortalDefaultDO created = new UserPortalDefaultDO();
            created.setUserId(userId);
            created.setSubSystemId(subSystemId);
            userPortalDefaultMapper.insert(created);
            return;
        }

        // 显式 set（含 null=统一门户），避免 updateById 跳过 null 字段
        userPortalDefaultMapper.update(null, new LambdaUpdateWrapper<UserPortalDefaultDO>()
                .eq(UserPortalDefaultDO::getId, existing.getId())
                .set(UserPortalDefaultDO::getSubSystemId, subSystemId));
    }

    @Override
    public void clearUserPortalDefault(Long userId) {
        userPortalDefaultMapper.deleteByUserId(userId);
    }

    /**
     * 规则默认：仅关联 1 个外部系统（门户 + 子系统共 2 个入口）时默认子系统，否则默认统一门户。
     */
    static UserPortalDefaultRespVO buildRuleDefault(List<UserExternalSystemRespVO> systems) {
        if (systems != null && systems.size() == 1) {
            UserExternalSystemRespVO system = systems.get(0);
            return new UserPortalDefaultRespVO(system.getSubSystemId(), system.getClientId(), false);
        }
        return new UserPortalDefaultRespVO(null, DEFAULT_SYSTEM_MAIN, false);
    }

    private UserExternalSystemRespVO findSystemBySubSystemId(List<UserExternalSystemRespVO> systems, Long subSystemId) {
        if (systems == null || subSystemId == null) {
            return null;
        }
        return systems.stream()
                .filter(item -> Objects.equals(item.getSubSystemId(), subSystemId))
                .findFirst()
                .orElse(null);
    }

}
