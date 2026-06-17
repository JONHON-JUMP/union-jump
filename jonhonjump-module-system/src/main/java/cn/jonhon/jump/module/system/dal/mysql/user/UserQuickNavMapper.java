package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserQuickNavDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserQuickNavMapper extends BaseMapperX<UserQuickNavDO> {

    default List<UserQuickNavDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<UserQuickNavDO>()
                .eq(UserQuickNavDO::getUserId, userId)
                .orderByAsc(UserQuickNavDO::getSort)
                .orderByAsc(UserQuickNavDO::getId));
    }

    default void deleteByUserId(Long userId) {
        delete(UserQuickNavDO::getUserId, userId);
    }

    default void deleteByMenuId(Long menuId) {
        delete(UserQuickNavDO::getMenuId, menuId);
    }

    default void deleteByMenuIds(Collection<Long> menuIds) {
        delete(new LambdaQueryWrapperX<UserQuickNavDO>()
                .in(UserQuickNavDO::getMenuId, menuIds));
    }

}
