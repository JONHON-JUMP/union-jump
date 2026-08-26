package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

/**
 * 子系统人员接口配置 Service 实现
 */
@Service
@Validated
public class SubSystemApiConfigServiceImpl implements SubSystemApiConfigService {

    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemEmployeeApiFactory subSystemEmployeeApiFactory;

    @Override
    public List<SubSystemApiConfigRespVO> getApiConfigList() {
        List<SubSystemApiConfigDO> list = subSystemApiConfigMapper.selectList();
        return buildRespList(list);
    }

    @Override
    public SubSystemApiConfigRespVO getApiConfig(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        return buildResp(config);
    }

    @Override
    public Long createApiConfig(SubSystemApiConfigSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validateDuplicate(createReqVO.getSubSystemId(), null);
        validateJsonFields(createReqVO);

        SubSystemApiConfigDO config = BeanUtils.toBean(createReqVO, SubSystemApiConfigDO.class);
        subSystemApiConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateApiConfig(SubSystemApiConfigSaveReqVO updateReqVO) {
        SubSystemApiConfigDO config = validateApiConfigExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getId());
        validateJsonFields(updateReqVO);

        SubSystemApiConfigDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemApiConfigDO.class);
        updateObj.setSubSystemId(config.getSubSystemId());
        subSystemApiConfigMapper.updateById(updateObj);
        // 配置变更：重建适配器（含旧 Cookie 会话作废）
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    public void deleteApiConfig(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        subSystemApiConfigMapper.deleteById(id);
        subSystemEmployeeApiFactory.invalidate(config.getSubSystemId());
    }

    @Override
    public SubSystemApiConfigDO getEnabledConfigBySubSystemId(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (config == null || (config.getStatus() != null && config.getStatus() == 1)) {
            return null;
        }
        return config;
    }

    @Override
    public List<Long> getEnabledSubSystemIds() {
        return subSystemApiConfigMapper.selectEnabledList().stream()
                .map(SubSystemApiConfigDO::getSubSystemId)
                .collect(Collectors.toList());
    }

    @Override
    public String testConnection(Long id) {
        SubSystemApiConfigDO config = validateApiConfigExists(id);
        try {
            return subSystemEmployeeApiFactory.getApi(config.getSubSystemId()).ping();
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        } catch (Exception e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    // ===================== 私有方法 =====================

    private List<SubSystemApiConfigRespVO> buildRespList(List<SubSystemApiConfigDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemApiConfigDO::getSubSystemId)),
                SubSystemDO::getId);
        return list.stream().map(config -> {
            SubSystemApiConfigRespVO vo = BeanUtils.toBean(config, SubSystemApiConfigRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(config.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private SubSystemApiConfigRespVO buildResp(SubSystemApiConfigDO config) {
        List<SubSystemApiConfigRespVO> list = buildRespList(Collections.singletonList(config));
        return list.isEmpty() ? BeanUtils.toBean(config, SubSystemApiConfigRespVO.class) : list.get(0);
    }

    private SubSystemApiConfigDO validateApiConfigExists(Long id) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectById(id);
        if (config == null) {
            throw exception(SUB_SYSTEM_API_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateSubSystemExists(Long subSystemId) {
        if (subSystemMapper.selectById(subSystemId) == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
    }

    private void validateDuplicate(Long subSystemId, Long excludeId) {
        SubSystemApiConfigDO existing = subSystemApiConfigMapper.selectBySubSystemId(subSystemId);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(SUB_SYSTEM_API_CONFIG_DUPLICATE);
        }
    }

    private void validateJsonFields(SubSystemApiConfigSaveReqVO reqVO) {
        String[] jsonFields = {reqVO.getAuthConfig(), reqVO.getApiQuery(), reqVO.getApiCreate(),
                reqVO.getApiUpdate(), reqVO.getApiDelete(), reqVO.getApiTeamCombo(),
                reqVO.getParamMapping(), reqVO.getResponseMapping()};
        for (String json : jsonFields) {
            if (StrUtil.isBlank(json)) {
                continue;
            }
            try {
                JsonUtils.parseObject(json, Object.class);
            } catch (Exception e) {
                throw exception(SUB_SYSTEM_API_CONFIG_INVALID_JSON, json);
            }
        }
    }

}
