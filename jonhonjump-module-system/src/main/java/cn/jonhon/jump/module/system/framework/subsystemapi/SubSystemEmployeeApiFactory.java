package cn.jonhon.jump.module.system.framework.subsystemapi;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

/**
 * 子系统人员接口适配器工厂
 *
 * 按 sub_system_api_config.apiType 分发：
 * - camstar → {@link CamstarEmployeeApiAdapter}
 * - http    → {@link GenericHttpEmployeeApiAdapter}
 *
 * 适配器实例按 subSystemId 缓存；配置变更后调用 {@link #invalidate(Long)} 重建。
 */
@Component
@Slf4j
public class SubSystemEmployeeApiFactory {

    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;

    /** 适配器缓存：subSystemId → 适配器实例 */
    private final Map<Long, SubSystemEmployeeApi> cache = new ConcurrentHashMap<>();

    public SubSystemEmployeeApi getApi(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_NOT_CONFIGURED);
        }
        return cache.computeIfAbsent(subSystemId, id -> createAdapter(config));
    }

    /**
     * 配置变更后调用：清掉旧适配器缓存（含旧 Cookie 会话）
     */
    public void invalidate(Long subSystemId) {
        cache.remove(subSystemId);
    }

    /** 是否已接入且「新增」接口启用（用户同步下拉用） */
    public boolean isConfigured(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null || StrUtil.isBlank(config.getApiCreate())) {
            return false;
        }
        try {
            return EndpointSpec.parse(config.getApiCreate(), "新增接口").isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private SubSystemEmployeeApi createAdapter(SubSystemApiConfigDO config) {
        switch (config.getApiType()) {
            case "camstar":
                return new CamstarEmployeeApiAdapter(config);
            case "http":
                return new GenericHttpEmployeeApiAdapter(config);
            default:
                throw exception(SUB_SYSTEM_EMPLOYEE_API_TYPE_NOT_SUPPORT, config.getApiType());
        }
    }

}
