package cn.jonhon.jump.module.system.service.permission;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleAvatarDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleAvatarMapper;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class RoleAvatarServiceImpl implements RoleAvatarService {

    @Resource
    private RoleAvatarMapper roleAvatarMapper;
    @Resource
    private RoleMapper roleMapper;

    @Override
    public Long createRoleAvatar(RoleAvatarSaveReqVO createReqVO) {
        validateRoleExists(createReqVO.getRoleCode());
        validateRoleCodeUnique(null, createReqVO.getRoleCode());
        RoleAvatarDO roleAvatar = BeanUtils.toBean(createReqVO, RoleAvatarDO.class);
        roleAvatarMapper.insert(roleAvatar);
        return roleAvatar.getId();
    }

    @Override
    public void updateRoleAvatar(RoleAvatarSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateRoleExists(updateReqVO.getRoleCode());
        validateRoleCodeUnique(updateReqVO.getId(), updateReqVO.getRoleCode());
        RoleAvatarDO updateObj = BeanUtils.toBean(updateReqVO, RoleAvatarDO.class);
        roleAvatarMapper.updateById(updateObj);
    }

    @Override
    public void deleteRoleAvatar(Long id) {
        validateExists(id);
        roleAvatarMapper.deleteById(id);
    }

    @Override
    public void deleteRoleAvatarList(List<Long> ids) {
        ids.forEach(this::validateExists);
        roleAvatarMapper.deleteByIds(ids);
    }

    @Override
    public RoleAvatarDO getRoleAvatar(Long id) {
        return roleAvatarMapper.selectById(id);
    }

    @Override
    public PageResult<RoleAvatarDO> getRoleAvatarPage(RoleAvatarPageReqVO pageReqVO) {
        return roleAvatarMapper.selectPage(pageReqVO);
    }

    @Override
    public List<RoleAvatarDO> getRoleAvatarSimpleList() {
        return roleAvatarMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private void validateExists(Long id) {
        if (roleAvatarMapper.selectById(id) == null) {
            throw exception(ROLE_AVATAR_NOT_EXISTS);
        }
    }

    private void validateRoleCodeUnique(Long id, String roleCode) {
        RoleAvatarDO roleAvatar = roleAvatarMapper.selectByRoleCode(roleCode);
        if (roleAvatar == null) {
            return;
        }
        if (id == null || !roleAvatar.getId().equals(id)) {
            throw exception(ROLE_AVATAR_ROLE_CODE_DUPLICATE);
        }
    }

    private void validateRoleExists(String roleCode) {
        RoleDO role = roleMapper.selectByCode(roleCode);
        if (role == null) {
            throw exception(ROLE_NOT_EXISTS);
        }
    }

}
