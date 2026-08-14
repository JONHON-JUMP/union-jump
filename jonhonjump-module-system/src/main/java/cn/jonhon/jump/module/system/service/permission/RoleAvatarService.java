package cn.jonhon.jump.module.system.service.permission;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleAvatarDO;

import javax.validation.Valid;
import java.util.List;

public interface RoleAvatarService {

    Long createRoleAvatar(@Valid RoleAvatarSaveReqVO createReqVO);

    void updateRoleAvatar(@Valid RoleAvatarSaveReqVO updateReqVO);

    void deleteRoleAvatar(Long id);

    void deleteRoleAvatarList(List<Long> ids);

    RoleAvatarDO getRoleAvatar(Long id);

    PageResult<RoleAvatarDO> getRoleAvatarPage(RoleAvatarPageReqVO pageReqVO);

    List<RoleAvatarDO> getRoleAvatarSimpleList();

}
