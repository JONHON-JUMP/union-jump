package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserQuickNavDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemUserQuickNavMapper extends BaseMapperX<SubSystemUserQuickNavDO> {

    default List<SubSystemUserQuickNavDO> selectListByUserIdAndSubSystemId(Long userId, Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemUserQuickNavDO>()
                .eq(SubSystemUserQuickNavDO::getUserId, userId)
                .eq(SubSystemUserQuickNavDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemUserQuickNavDO::getSort)
                .orderByAsc(SubSystemUserQuickNavDO::getId));
    }

    default void deleteByUserIdAndSubSystemId(Long userId, Long subSystemId) {
        delete(new LambdaQueryWrapperX<SubSystemUserQuickNavDO>()
                .eq(SubSystemUserQuickNavDO::getUserId, userId)
                .eq(SubSystemUserQuickNavDO::getSubSystemId, subSystemId));
    }

    default void deleteByMenuId(Long menuId) {
        delete(SubSystemUserQuickNavDO::getMenuId, menuId);
    }

    default void deleteByMenuIds(Collection<Long> menuIds) {
        delete(new LambdaQueryWrapperX<SubSystemUserQuickNavDO>()
                .in(SubSystemUserQuickNavDO::getMenuId, menuIds));
    }

}
