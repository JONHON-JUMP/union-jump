package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPostPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemPostDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemPostMapper extends BaseMapperX<SubSystemPostDO> {

    default PageResult<SubSystemPostDO> selectPage(SubSystemPostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemPostDO>()
                .eqIfPresent(SubSystemPostDO::getSubSystemId, reqVO.getSubSystemId())
                .likeIfPresent(SubSystemPostDO::getName, reqVO.getName())
                .likeIfPresent(SubSystemPostDO::getCode, reqVO.getCode())
                .eqIfPresent(SubSystemPostDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SubSystemPostDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(SubSystemPostDO::getSort)
                .orderByDesc(SubSystemPostDO::getId));
    }

    default List<SubSystemPostDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemPostDO>()
                .eq(SubSystemPostDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemPostDO::getSort)
                .orderByDesc(SubSystemPostDO::getId));
    }

    default List<SubSystemPostDO> selectListByIds(Collection<Long> ids) {
        return selectList(SubSystemPostDO::getId, ids);
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(SubSystemPostDO::getSubSystemId, subSystemId);
    }

    default SubSystemPostDO selectBySubSystemIdAndName(Long subSystemId, String name) {
        return selectOne(new LambdaQueryWrapperX<SubSystemPostDO>()
                .eq(SubSystemPostDO::getSubSystemId, subSystemId)
                .eq(SubSystemPostDO::getName, name));
    }

    default SubSystemPostDO selectBySubSystemIdAndCode(Long subSystemId, String code) {
        return selectOne(new LambdaQueryWrapperX<SubSystemPostDO>()
                .eq(SubSystemPostDO::getSubSystemId, subSystemId)
                .eq(SubSystemPostDO::getCode, code));
    }

}
