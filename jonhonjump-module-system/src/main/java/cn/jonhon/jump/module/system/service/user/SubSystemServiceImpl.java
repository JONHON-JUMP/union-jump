package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemOAuth2ClientSimpleRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SubSystemServiceImpl implements SubSystemService {

    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemPostMapper subSystemPostMapper;
    @Resource
    private SubSystemTeamMapper subSystemTeamMapper;

    @Override
    public PageResult<SubSystemRespVO> getSubSystemPage(SubSystemPageReqVO pageReqVO) {
        PageResult<SubSystemDO> pageResult = subSystemMapper.selectPage(pageReqVO);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SubSystemRespVO getSubSystem(Long id) {
        SubSystemDO subSystem = validateSubSystemExists(id);
        return buildResp(subSystem);
    }

    @Override
    public Long createSubSystem(SubSystemSaveReqVO createReqVO) {
        validateOAuth2ClientExists(createReqVO.getClientId());
        validateClientIdDuplicate(createReqVO.getClientId(), null);

        SubSystemDO subSystem = BeanUtils.toBean(createReqVO, SubSystemDO.class);
        subSystemMapper.insert(subSystem);
        return subSystem.getId();
    }

    @Override
    public void updateSubSystem(SubSystemSaveReqVO updateReqVO) {
        SubSystemDO subSystem = validateSubSystemExists(updateReqVO.getId());
        validateOAuth2ClientExists(subSystem.getClientId());

        SubSystemDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemDO.class);
        updateObj.setClientId(subSystem.getClientId());
        subSystemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystem(Long id) {
        validateSubSystemExists(id);
        validateSubSystemNotUsed(id);
        subSystemMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemList(List<Long> ids) {
        ids.forEach(id -> {
            validateSubSystemExists(id);
            validateSubSystemNotUsed(id);
        });
        subSystemMapper.deleteByIds(ids);
    }

    @Override
    public List<SubSystemOAuth2ClientSimpleRespVO> getOAuth2ClientSimpleList(Long excludeSubSystemId) {
        List<OAuth2ClientDO> clients = oauth2ClientMapper.selectList();
        if (CollUtil.isEmpty(clients)) {
            return Collections.emptyList();
        }
        Set<String> boundClientIds = convertSet(subSystemMapper.selectList(), SubSystemDO::getClientId);
        if (excludeSubSystemId != null) {
            SubSystemDO exclude = subSystemMapper.selectById(excludeSubSystemId);
            if (exclude != null) {
                boundClientIds.remove(exclude.getClientId());
            }
        }
        return clients.stream().map(client -> {
            SubSystemOAuth2ClientSimpleRespVO vo = new SubSystemOAuth2ClientSimpleRespVO();
            vo.setClientId(client.getClientId());
            vo.setName(client.getName());
            vo.setLogo(client.getLogo());
            vo.setDescription(client.getDescription());
            vo.setStatus(client.getStatus());
            vo.setBound(boundClientIds.contains(client.getClientId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SubSystemRespVO> buildRespList(List<SubSystemDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<String, OAuth2ClientDO> clientMap = convertMap(
                oauth2ClientMapper.selectList(OAuth2ClientDO::getClientId,
                        convertSet(list, SubSystemDO::getClientId)),
                OAuth2ClientDO::getClientId);
        return list.stream().map(subSystem -> convertToRespVO(subSystem, clientMap.get(subSystem.getClientId())))
                .collect(Collectors.toList());
    }

    private SubSystemRespVO buildResp(SubSystemDO subSystem) {
        OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(subSystem.getClientId());
        return convertToRespVO(subSystem, client);
    }

    private SubSystemRespVO convertToRespVO(SubSystemDO subSystem, OAuth2ClientDO client) {
        SubSystemRespVO vo = BeanUtils.toBean(subSystem, SubSystemRespVO.class);
        if (client != null) {
            vo.setClientName(client.getName());
            vo.setClientLogo(client.getLogo());
            vo.setClientDescription(client.getDescription());
            vo.setClientStatus(client.getStatus());
        }
        return vo;
    }

    private OAuth2ClientDO validateOAuth2ClientExists(String clientId) {
        OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(clientId);
        if (client == null) {
            throw exception(SUB_SYSTEM_OAUTH2_CLIENT_NOT_EXISTS);
        }
        return client;
    }

    private SubSystemDO validateSubSystemExists(Long id) {
        SubSystemDO subSystem = subSystemMapper.selectById(id);
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        return subSystem;
    }

    private void validateClientIdDuplicate(String clientId, Long id) {
        SubSystemDO subSystem = subSystemMapper.selectByClientId(clientId);
        if (subSystem != null && !ObjectUtil.equal(subSystem.getId(), id)) {
            throw exception(SUB_SYSTEM_CLIENT_ID_DUPLICATE, clientId);
        }
    }

    private void validateSubSystemNotUsed(Long subSystemId) {
        if (hasRelatedData(subSystemId)) {
            throw exception(SUB_SYSTEM_HAS_RELATED_DATA);
        }
    }

    private boolean hasRelatedData(Long subSystemId) {
        return countIfPresent(subSystemUsersMapper.selectCountBySubSystemId(subSystemId))
                || countIfPresent(subSystemRoleMapper.selectCountBySubSystemId(subSystemId))
                || countIfPresent(subSystemMenuMapper.selectCountBySubSystemId(subSystemId))
                || countIfPresent(subSystemPostMapper.selectCountBySubSystemId(subSystemId))
                || countIfPresent(subSystemTeamMapper.selectCountBySubSystemId(subSystemId));
    }

    private boolean countIfPresent(Long count) {
        return count != null && count > 0;
    }

}
