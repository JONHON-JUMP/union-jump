package cn.jonhon.jump.module.system.dal.mysql.permission;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleQuickNavDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleQuickNavMapper extends BaseMapperX<RoleQuickNavDO> {

    default List<RoleQuickNavDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapperX<RoleQuickNavDO>()
                .eq(RoleQuickNavDO::getRoleId, roleId)
                .orderByAsc(RoleQuickNavDO::getSort)
                .orderByAsc(RoleQuickNavDO::getId));
    }

    default List<RoleQuickNavDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapperX<RoleQuickNavDO>()
                .in(RoleQuickNavDO::getRoleId, roleIds)
                .orderByAsc(RoleQuickNavDO::getRoleId)
                .orderByAsc(RoleQuickNavDO::getSort)
                .orderByAsc(RoleQuickNavDO::getId));
    }

    @Delete("DELETE FROM system_role_quick_nav WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("<script>DELETE FROM system_role_quick_nav WHERE role_id IN "
            + "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    default List<RoleQuickNavDO> selectListByMenuId(Long menuId) {
        return selectList(RoleQuickNavDO::getMenuId, menuId);
    }

    @Delete("DELETE FROM system_role_quick_nav WHERE menu_id = #{menuId}")
    void deleteByMenuId(@Param("menuId") Long menuId);

    @Delete("<script>DELETE FROM system_role_quick_nav WHERE menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByMenuIds(@Param("menuIds") Collection<Long> menuIds);

}
