package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleMenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemRoleMenuMapper extends BaseMapperX<SubSystemRoleMenuDO> {

    default List<SubSystemRoleMenuDO> selectListByRoleId(Long roleId) {
        return selectList(SubSystemRoleMenuDO::getRoleId, roleId);
    }

    default List<SubSystemRoleMenuDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapperX<SubSystemRoleMenuDO>()
                .in(SubSystemRoleMenuDO::getRoleId, roleIds));
    }

    default void deleteListByRoleId(Long roleId) {
        delete(new LambdaQueryWrapperX<SubSystemRoleMenuDO>().eq(SubSystemRoleMenuDO::getRoleId, roleId));
    }

    default void deleteListByRoleIdAndMenuIds(Long roleId, Collection<Long> menuIds) {
        delete(new LambdaQueryWrapperX<SubSystemRoleMenuDO>()
                .eq(SubSystemRoleMenuDO::getRoleId, roleId)
                .in(SubSystemRoleMenuDO::getMenuId, menuIds));
    }

    default Long selectCountByMenuId(Long menuId) {
        return selectCount(SubSystemRoleMenuDO::getMenuId, menuId);
    }

    default List<SubSystemRoleMenuDO> selectListByMenuId(Long menuId) {
        return selectList(SubSystemRoleMenuDO::getMenuId, menuId);
    }

    default void deleteListByMenuId(Long menuId) {
        delete(new LambdaQueryWrapperX<SubSystemRoleMenuDO>().eq(SubSystemRoleMenuDO::getMenuId, menuId));
    }

    default void deleteListByMenuIds(Collection<Long> menuIds) {
        delete(new LambdaQueryWrapperX<SubSystemRoleMenuDO>().in(SubSystemRoleMenuDO::getMenuId, menuIds));
    }

}
