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

                // subSystemId=0 是通用菜单模板保留区，不得混入常规菜单查询（ne 返回父类型，须放在 X 特有方法之后）

                .ne(SubSystemMenuDO::getSubSystemId, 0L)

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

    /** 通用菜单模板列表（subSystemId=0 保留为模板区） */
    default List<SubSystemMenuDO> selectCommonTemplateList() {
        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()
                .eq(SubSystemMenuDO::getSubSystemId, 0L)
                .orderByAsc(SubSystemMenuDO::getOrderNum)
                .orderByAsc(SubSystemMenuDO::getId));
    }

    /** 某模板在各子系统下的全部副本 */
    default List<SubSystemMenuDO> selectListBySharedSourceId(Long sharedSourceId) {
        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()
                .eq(SubSystemMenuDO::getSharedSourceId, sharedSourceId));
    }

    /** 全部通用模板副本（按模板分组用） */
    default List<SubSystemMenuDO> selectAllSharedCopies() {
        return selectList(new LambdaQueryWrapperX<SubSystemMenuDO>()
                .isNotNull(SubSystemMenuDO::getSharedSourceId));
    }

}

