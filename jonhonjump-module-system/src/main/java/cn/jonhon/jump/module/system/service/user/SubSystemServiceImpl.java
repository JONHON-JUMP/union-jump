package cn.jonhon.jump.module.system.service.user;



import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.ObjectUtil;

import cn.hutool.core.util.StrUtil;

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

    @Resource

    private UserPortalDefaultMapper userPortalDefaultMapper;

    @Resource

    private SubSystemPermissionContextService subSystemPermissionContextService;



    @Override

    public PageResult<SubSystemRespVO> getSubSystemPage(SubSystemPageReqVO pageReqVO) {

        resolveOauth2ClientIdFilter(pageReqVO);

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

        validateOAuth2ClientExists(createReqVO.getOauth2ClientId());

        validateOauth2ClientDuplicate(createReqVO.getOauth2ClientId(), null);



        SubSystemDO subSystem = BeanUtils.toBean(createReqVO, SubSystemDO.class);

        subSystemMapper.insert(subSystem);

        return subSystem.getId();

    }



    @Override

    public void updateSubSystem(SubSystemSaveReqVO updateReqVO) {

        validateSubSystemExists(updateReqVO.getId());

        validateOAuth2ClientExists(updateReqVO.getOauth2ClientId());

        validateOauth2ClientDuplicate(updateReqVO.getOauth2ClientId(), updateReqVO.getId());



        SubSystemDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemDO.class);

        subSystemMapper.updateById(updateObj);

        // 停用或换绑 OAuth 客户端时，清权限包，避免子系统继续用旧缓存
        if (updateReqVO.getStatus() != null || updateReqVO.getOauth2ClientId() != null) {
            subSystemPermissionContextService.evictBySubSystemId(updateReqVO.getId());
        }

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

        Set<Long> boundOauth2ClientIds = convertSet(subSystemMapper.selectList(), SubSystemDO::getOauth2ClientId);

        if (excludeSubSystemId != null) {

            SubSystemDO exclude = subSystemMapper.selectById(excludeSubSystemId);

            if (exclude != null) {

                boundOauth2ClientIds.remove(exclude.getOauth2ClientId());

            }

        }

        return clients.stream().map(client -> {

            SubSystemOAuth2ClientSimpleRespVO vo = new SubSystemOAuth2ClientSimpleRespVO();

            vo.setId(client.getId());

            vo.setClientId(client.getClientId());

            vo.setName(client.getName());

            vo.setLogo(client.getLogo());

            vo.setDescription(client.getDescription());

            vo.setStatus(client.getStatus());

            vo.setBound(boundOauth2ClientIds.contains(client.getId()));

            return vo;

        }).collect(Collectors.toList());

    }



    private void resolveOauth2ClientIdFilter(SubSystemPageReqVO pageReqVO) {

        if (StrUtil.isBlank(pageReqVO.getClientId())) {

            return;

        }

        OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(pageReqVO.getClientId());

        pageReqVO.setOauth2ClientId(client != null ? client.getId() : -1L);

    }



    private List<SubSystemRespVO> buildRespList(List<SubSystemDO> list) {

        if (CollUtil.isEmpty(list)) {

            return Collections.emptyList();

        }

        Map<Long, OAuth2ClientDO> clientMap = convertMap(

                oauth2ClientMapper.selectList(OAuth2ClientDO::getId,

                        convertSet(list, SubSystemDO::getOauth2ClientId)),

                OAuth2ClientDO::getId);

        return list.stream()

                .map(subSystem -> convertToRespVO(subSystem, clientMap.get(subSystem.getOauth2ClientId())))

                .collect(Collectors.toList());

    }



    private SubSystemRespVO buildResp(SubSystemDO subSystem) {

        OAuth2ClientDO client = oauth2ClientMapper.selectById(subSystem.getOauth2ClientId());

        return convertToRespVO(subSystem, client);

    }



    private SubSystemRespVO convertToRespVO(SubSystemDO subSystem, OAuth2ClientDO client) {

        SubSystemRespVO vo = BeanUtils.toBean(subSystem, SubSystemRespVO.class);

        if (client != null) {

            vo.setClientId(client.getClientId());

            vo.setClientName(client.getName());

            vo.setClientLogo(client.getLogo());

            vo.setClientDescription(client.getDescription());

            vo.setClientStatus(client.getStatus());

        }

        return vo;

    }



    private OAuth2ClientDO validateOAuth2ClientExists(Long oauth2ClientId) {

        OAuth2ClientDO client = oauth2ClientMapper.selectById(oauth2ClientId);

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



    private void validateOauth2ClientDuplicate(Long oauth2ClientId, Long id) {

        SubSystemDO subSystem = subSystemMapper.selectByOauth2ClientId(oauth2ClientId);

        if (subSystem != null && !ObjectUtil.equal(subSystem.getId(), id)) {

            OAuth2ClientDO client = oauth2ClientMapper.selectById(oauth2ClientId);

            String clientId = client != null ? client.getClientId() : String.valueOf(oauth2ClientId);

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

                || countIfPresent(subSystemTeamMapper.selectCountBySubSystemId(subSystemId))

                || countIfPresent(userPortalDefaultMapper.selectCountBySubSystemId(subSystemId));

    }



    private boolean countIfPresent(Long count) {

        return count != null && count > 0;

    }



}

