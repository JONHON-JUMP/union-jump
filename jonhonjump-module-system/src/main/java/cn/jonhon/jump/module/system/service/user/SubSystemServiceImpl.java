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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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

        PageResult<SubSystemDO> pageResult = subSystemMapper.selectPage(pageReqVO);

        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());

    }



    @Override

    public SubSystemRespVO getSubSystem(Long id) {

        SubSystemDO subSystem = validateSubSystemExists(id);

        return buildResp(subSystem);

    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSubSystem(SubSystemSaveReqVO createReqVO) {
        String clientId = requireValidClientId(createReqVO.getClientId());
        validateClientIdDuplicate(clientId, null);
        validateSystemNameDuplicate(createReqVO.getSystemName(), null);

        SubSystemDO subSystem = BeanUtils.toBean(createReqVO, SubSystemDO.class);
        subSystem.setClientId(clientId);
        // 打开页面已不走 OAuth，登记只写业务系统本身；图标可空。
        subSystem.setOauth2ClientId(null);
        try {
            subSystemMapper.insert(subSystem);
        } catch (DuplicateKeyException ex) {
            throw exception(SUB_SYSTEM_CLIENT_ID_EXISTS, clientId);
        } catch (DataIntegrityViolationException ex) {
            throw exception(SUB_SYSTEM_SAVE_FAILED);
        }
        return subSystem.getId();
    }



    @Override

    public Long createApiOnlySubSystem(String systemName) {

        String name = StrUtil.trim(systemName);

        if (StrUtil.isBlank(name)) {

            throw exception(SUB_SYSTEM_API_CONFIG_TARGET_REQUIRED);

        }

        validateSystemNameDuplicate(name, null);

        SubSystemDO subSystem = new SubSystemDO();

        subSystem.setOauth2ClientId(null);

        subSystem.setSystemName(name);

        subSystem.setDescription("Camstar/人员接口目标（非 JUMP 门户业务系统，不出现在外部用户管理）");

        subSystem.setStatus(0);

        subSystemMapper.insert(subSystem);

        return subSystem.getId();

    }



    @Override

    public void updateSystemName(Long id, String systemName) {

        validateSubSystemExists(id);

        String name = StrUtil.trim(systemName);

        if (StrUtil.isBlank(name)) {

            throw exception(SUB_SYSTEM_API_CONFIG_TARGET_REQUIRED);

        }

        validateSystemNameDuplicate(name, id);

        SubSystemDO updateObj = new SubSystemDO();

        updateObj.setId(id);

        updateObj.setSystemName(name);

        subSystemMapper.updateById(updateObj);

    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubSystem(SubSystemSaveReqVO updateReqVO) {
        validateSubSystemExists(updateReqVO.getId());
        String clientId = requireValidClientId(updateReqVO.getClientId());
        validateClientIdDuplicate(clientId, updateReqVO.getId());
        validateSystemNameDuplicate(updateReqVO.getSystemName(), updateReqVO.getId());

        SubSystemDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemDO.class);
        updateObj.setClientId(clientId);
        updateObj.setOauth2ClientId(null);
        try {
            subSystemMapper.updateById(updateObj);
        } catch (DuplicateKeyException ex) {
            throw exception(SUB_SYSTEM_CLIENT_ID_EXISTS, clientId);
        } catch (DataIntegrityViolationException ex) {
            throw exception(SUB_SYSTEM_SAVE_FAILED);
        }
        subSystemPermissionContextService.evictBySubSystemId(updateReqVO.getId());
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

        Set<Long> boundOauth2ClientIds = subSystemMapper.selectList().stream()

                .map(SubSystemDO::getOauth2ClientId)

                .filter(java.util.Objects::nonNull)

                .collect(Collectors.toSet());

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



    private List<SubSystemRespVO> buildRespList(List<SubSystemDO> list) {

        if (CollUtil.isEmpty(list)) {

            return Collections.emptyList();

        }

        Set<Long> oauth2ClientIds = list.stream()

                .map(SubSystemDO::getOauth2ClientId)

                .filter(java.util.Objects::nonNull)

                .collect(Collectors.toSet());

        Map<Long, OAuth2ClientDO> clientMap = CollUtil.isEmpty(oauth2ClientIds)

                ? Collections.emptyMap()

                : convertMap(oauth2ClientMapper.selectList(OAuth2ClientDO::getId, oauth2ClientIds),

                        OAuth2ClientDO::getId);

        return list.stream()

                .map(subSystem -> convertToRespVO(subSystem, clientMap.get(subSystem.getOauth2ClientId())))

                .collect(Collectors.toList());

    }



    private SubSystemRespVO buildResp(SubSystemDO subSystem) {

        OAuth2ClientDO client = subSystem.getOauth2ClientId() != null

                ? oauth2ClientMapper.selectById(subSystem.getOauth2ClientId()) : null;

        return convertToRespVO(subSystem, client);

    }



    private SubSystemRespVO convertToRespVO(SubSystemDO subSystem, OAuth2ClientDO client) {

        SubSystemRespVO vo = BeanUtils.toBean(subSystem, SubSystemRespVO.class);
        vo.setClientId(subSystem.resolvePortalClientId(client != null ? client.getClientId() : null));

        if (client != null) {

            vo.setClientName(client.getName());

            vo.setClientLogo(client.getLogo());

            vo.setClientDescription(client.getDescription());

            vo.setClientStatus(client.getStatus());

        }

        return vo;

    }



    private String requireValidClientId(String rawClientId) {
        String clientId = StrUtil.trim(rawClientId);
        if (StrUtil.isBlank(clientId)) {
            throw exception(SUB_SYSTEM_CLIENT_ID_REQUIRED);
        }
        if (!clientId.matches("^[a-zA-Z][a-zA-Z0-9_-]*$")) {
            throw exception(SUB_SYSTEM_CLIENT_ID_INVALID);
        }
        return clientId;
    }

    private void validateClientIdDuplicate(String clientId, Long id) {
        SubSystemDO exists = subSystemMapper.selectByClientId(clientId);
        if (exists != null && !ObjectUtil.equal(exists.getId(), id)) {
            throw exception(SUB_SYSTEM_CLIENT_ID_EXISTS, clientId);
        }
    }

    private SubSystemDO validateSubSystemExists(Long id) {

        SubSystemDO subSystem = subSystemMapper.selectById(id);

        if (subSystem == null) {

            throw exception(SUB_SYSTEM_NOT_EXISTS);

        }

        return subSystem;

    }



    private void validateSystemNameDuplicate(String systemName, Long id) {

        SubSystemDO exists = subSystemMapper.selectOne(SubSystemDO::getSystemName, systemName);

        if (exists != null && !ObjectUtil.equal(exists.getId(), id)) {

            throw exception(SUB_SYSTEM_NAME_DUPLICATE, systemName);

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

