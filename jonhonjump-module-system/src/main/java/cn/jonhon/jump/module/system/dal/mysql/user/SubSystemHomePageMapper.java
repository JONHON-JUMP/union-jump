package cn.jonhon.jump.module.system.dal.mysql.user;



import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;

import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;

import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemHomePageDO;

import org.apache.ibatis.annotations.Mapper;



import java.util.List;



@Mapper

public interface SubSystemHomePageMapper extends BaseMapperX<SubSystemHomePageDO> {



    default List<SubSystemHomePageDO> selectListOrderBySubSystemId() {

        return selectList(new LambdaQueryWrapperX<SubSystemHomePageDO>()

                .orderByAsc(SubSystemHomePageDO::getSubSystemId));

    }



    default SubSystemHomePageDO selectBySubSystemId(Long subSystemId) {

        return selectOne(SubSystemHomePageDO::getSubSystemId, subSystemId);

    }



}

