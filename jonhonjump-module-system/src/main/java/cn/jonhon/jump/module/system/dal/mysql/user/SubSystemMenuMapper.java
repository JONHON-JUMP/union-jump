package cn.jonhon.jump.module.system.dal.mysql.user;



import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;

import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuListReqVO;

import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;

import org.apache.ibatis.annotations.Mapper;



import java.util.Collection;
import java.util.List;



@Mapper

public interface SubSystemMenuMapper extends BaseMapperX<SubSystemMenuDO> {



    default List<SubSystemMenuDO> selectList(SubSystemMenuListReqVO reqVO) {

        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()

                .eqIfPresent(SubSystemMenuDO::getSubSystemId, reqVO.getSubSystemId())

                .likeIfPresent(SubSystemMenuDO::getMenuName, reqVO.getName())

                .eqIfPresent(SubSystemMenuDO::getStatus, reqVO.getStatus())

                .orderByAsc(SubSystemMenuDO::getOrderNum)

                .orderByAsc(SubSystemMenuDO::getId));

    }



    default List<SubSystemMenuDO> selectListBySubSystemId(Long subSystemId) {

        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()

                .eq(SubSystemMenuDO::getSubSystemId, subSystemId)

                .orderByAsc(SubSystemMenuDO::getOrderNum)

                .orderByAsc(SubSystemMenuDO::getId));

    }



    default Long selectCountBySubSystemId(Long subSystemId) {

        return selectCount(SubSystemMenuDO::getSubSystemId, subSystemId);

    }



    default Long selectCountByParentId(Long parentId) {

        return selectCount(SubSystemMenuDO::getParentId, parentId);

    }



    default SubSystemMenuDO selectBySubSystemIdAndParentIdAndName(Long subSystemId, Long parentId, String name) {

        return selectOne(new LambdaQueryWrapperX<SubSystemMenuDO>()

                .eq(SubSystemMenuDO::getSubSystemId, subSystemId)

                .eq(SubSystemMenuDO::getParentId, parentId)

                .eq(SubSystemMenuDO::getMenuName, name));

    }

    default List<SubSystemMenuDO> selectListBySubSystemIdAndType(Long subSystemId, String type) {
        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()
                .eq(SubSystemMenuDO::getSubSystemId, subSystemId)
                .eq(SubSystemMenuDO::getType, type)
                .orderByAsc(SubSystemMenuDO::getOrderNum)
                .orderByAsc(SubSystemMenuDO::getId));
    }

    default List<SubSystemMenuDO> selectListByIds(Collection<Long> ids) {
        return selectList(SubSystemMenuDO::getId, ids);
    }

}

