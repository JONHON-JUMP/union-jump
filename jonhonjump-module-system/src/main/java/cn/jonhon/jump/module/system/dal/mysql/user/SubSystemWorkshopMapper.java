package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SubSystemWorkshopMapper extends BaseMapperX<SubSystemWorkshopDO> {

    default PageResult<SubSystemWorkshopDO> selectPage(SubSystemWorkshopPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eqIfPresent(SubSystemWorkshopDO::getSubSystemId, reqVO.getSubSystemId())
                .eqIfPresent(SubSystemWorkshopDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(SubSystemWorkshopDO::getWorkshopCode, reqVO.getWorkshopCode())
                .likeIfPresent(SubSystemWorkshopDO::getWorkshopName, reqVO.getWorkshopName())
                .betweenIfPresent(SubSystemWorkshopDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode)
                .orderByDesc(SubSystemWorkshopDO::getId));
    }

    default List<SubSystemWorkshopDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode));
    }

    default List<SubSystemWorkshopDO> selectListByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getDeptId, deptId)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode));
    }

    default List<SubSystemWorkshopDO> selectListBySubSystemIdAndDeptId(Long subSystemId, Long deptId) {
        return selectList(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .eq(SubSystemWorkshopDO::getDeptId, deptId)
                .orderByAsc(SubSystemWorkshopDO::getWorkshopCode));
    }

    default SubSystemWorkshopDO selectBySubSystemIdAndWorkshopCode(Long subSystemId, String workshopCode) {
        return selectOne(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .eq(SubSystemWorkshopDO::getWorkshopCode, workshopCode));
    }

    default SubSystemWorkshopDO selectBySubSystemIdAndWorkshopName(Long subSystemId, String workshopName) {
        return selectOne(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .eq(SubSystemWorkshopDO::getWorkshopName, workshopName));
    }

    default Long selectCountByWorkshopCode(Long subSystemId, String workshopCode) {
        return selectCount(new LambdaQueryWrapperX<SubSystemWorkshopDO>()
                .eq(SubSystemWorkshopDO::getSubSystemId, subSystemId)
                .eq(SubSystemWorkshopDO::getWorkshopCode, workshopCode));
    }

}
