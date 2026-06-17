package cn.jonhon.jump.module.system.dal.mysql.user;



import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemTeamDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SubSystemTeamMapper extends BaseMapperX<SubSystemTeamDO> {

    default PageResult<SubSystemTeamDO> selectPage(SubSystemTeamPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemTeamDO>()
                .eqIfPresent(SubSystemTeamDO::getSubSystemId, reqVO.getSubSystemId())
                .likeIfPresent(SubSystemTeamDO::getTeamCode, reqVO.getTeamCode())
                .likeIfPresent(SubSystemTeamDO::getTeamName, reqVO.getTeamName())
                .betweenIfPresent(SubSystemTeamDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(SubSystemTeamDO::getTeamCode)
                .orderByDesc(SubSystemTeamDO::getId));
    }

    default List<SubSystemTeamDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemTeamDO>()
                .eq(SubSystemTeamDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemTeamDO::getTeamCode));
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(SubSystemTeamDO::getSubSystemId, subSystemId);
    }

    default SubSystemTeamDO selectBySubSystemIdAndTeamName(Long subSystemId, String teamName) {
        return selectOne(new LambdaQueryWrapperX<SubSystemTeamDO>()
                .eq(SubSystemTeamDO::getSubSystemId, subSystemId)
                .eq(SubSystemTeamDO::getTeamName, teamName));
    }



    default List<SubSystemTeamDO> selectListBySubSystemIdAndTeamNameLike(Long subSystemId, String teamName) {
        return selectList(new LambdaQueryWrapperX<SubSystemTeamDO>()
                .eq(SubSystemTeamDO::getSubSystemId, subSystemId)
                .like(SubSystemTeamDO::getTeamName, teamName)
                .orderByAsc(SubSystemTeamDO::getTeamCode));
    }

    default List<SubSystemTeamDO> selectListByTeamNameLike(String teamName) {
        return selectList(new LambdaQueryWrapperX<SubSystemTeamDO>()
                .like(SubSystemTeamDO::getTeamName, teamName)
                .orderByAsc(SubSystemTeamDO::getTeamCode));
    }

    default SubSystemTeamDO selectBySubSystemIdAndTeamCode(Long subSystemId, String teamCode) {

        return selectOne(new LambdaQueryWrapperX<SubSystemTeamDO>()

                .eq(SubSystemTeamDO::getSubSystemId, subSystemId)

                .eq(SubSystemTeamDO::getTeamCode, teamCode));

    }



}

