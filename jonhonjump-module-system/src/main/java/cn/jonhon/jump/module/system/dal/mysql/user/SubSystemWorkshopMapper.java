package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemWorkshopMapper extends BaseMapperX<SubSystemWorkshopDO> {

    default PageResult<SubSystemWorkshopDO> selectPage(SubSystemWorkshopPageReqVO reqVO,
                                                       Collection<Long> allowedSubSystemIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eqIfPresent(SubSystemWorkshopDO::getSubSystemId, reqVO.getSubSystemId())
                .inIfPresent(SubSystemWorkshopDO::getSubSystemId, allowedSubSystemIds)
                .likeIfPresent(SubSystemWorkshopDO::getWorkshopCode, reqVO.getWorkshopCode())
                .likeIfPresent(SubSystemWorkshopDO::getWorkshopName, reqVO.getWorkshopName())
                .eqIfPresent(SubSystemWorkshopDO::getStatus, reqVO.getStatus())
                .orderByAsc(SubSystemWorkshopDO::getSort)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode)
                .orderByDesc(SubSystemWorkshopDO::getId));
    }

    default List<SubSystemWorkshopDO> selectEnabledBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .eq(SubSystemWorkshopDO::getStatus, 0)
                .orderByAsc(SubSystemWorkshopDO::getSort)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode));
    }

    default List<SubSystemWorkshopDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getStatus, 0)
                .orderByAsc(SubSystemWorkshopDO::getSort)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode));
    }

    default SubSystemWorkshopDO selectByWorkshopCode(String workshopCode) {
        return selectOne(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getWorkshopCode, workshopCode));
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(SubSystemWorkshopDO::getSubSystemId, subSystemId);
    }

}
