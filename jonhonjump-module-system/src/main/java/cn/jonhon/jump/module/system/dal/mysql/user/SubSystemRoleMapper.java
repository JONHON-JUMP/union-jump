package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRolePageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemRoleMapper extends BaseMapperX<SubSystemRoleDO> {

    default PageResult<SubSystemRoleDO> selectPage(SubSystemRolePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemRoleDO>()
                .eqIfPresent(SubSystemRoleDO::getSubSystemId, reqVO.getSubSystemId())
                .likeIfPresent(SubSystemRoleDO::getName, reqVO.getName())
                .likeIfPresent(SubSystemRoleDO::getCode, reqVO.getCode())
                .eqIfPresent(SubSystemRoleDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SubSystemRoleDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(SubSystemRoleDO::getSort)
                .orderByDesc(SubSystemRoleDO::getId));
    }

    default SubSystemRoleDO selectBySubSystemIdAndName(Long subSystemId, String name) {
        return selectOne(new LambdaQueryWrapperX<SubSystemRoleDO>()
                .eq(SubSystemRoleDO::getSubSystemId, subSystemId)
                .eq(SubSystemRoleDO::getName, name));
    }

    default SubSystemRoleDO selectBySubSystemIdAndCode(Long subSystemId, String code) {
        return selectOne(new LambdaQueryWrapperX<SubSystemRoleDO>()
                .eq(SubSystemRoleDO::getSubSystemId, subSystemId)
                .eq(SubSystemRoleDO::getCode, code));
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(SubSystemRoleDO::getSubSystemId, subSystemId);
    }

    default List<SubSystemRoleDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemRoleDO>()
                .eq(SubSystemRoleDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemRoleDO::getSort)
                .orderByDesc(SubSystemRoleDO::getId));
    }

    default List<SubSystemRoleDO> selectListByIds(Collection<Long> ids) {
        return selectList(SubSystemRoleDO::getId, ids);
    }

}
