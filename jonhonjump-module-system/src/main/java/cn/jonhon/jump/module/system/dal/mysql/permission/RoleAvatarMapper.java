package cn.jonhon.jump.module.system.dal.mysql.permission;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleAvatarDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleAvatarMapper extends BaseMapperX<RoleAvatarDO> {

    default PageResult<RoleAvatarDO> selectPage(RoleAvatarPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RoleAvatarDO>()
                .likeIfPresent(RoleAvatarDO::getRoleCode, reqVO.getRoleCode())
                .eqIfPresent(RoleAvatarDO::getStatus, reqVO.getStatus())
                .orderByAsc(RoleAvatarDO::getSort)
                .orderByDesc(RoleAvatarDO::getId));
    }

    default List<RoleAvatarDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<RoleAvatarDO>()
                .eqIfPresent(RoleAvatarDO::getStatus, status)
                .orderByAsc(RoleAvatarDO::getSort)
                .orderByDesc(RoleAvatarDO::getId));
    }

    default RoleAvatarDO selectByRoleCode(String roleCode) {
        return selectOne(RoleAvatarDO::getRoleCode, roleCode);
    }

}
