package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleQuickNavDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemRoleQuickNavMapper extends BaseMapperX<SubSystemRoleQuickNavDO> {

    default List<SubSystemRoleQuickNavDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapperX<SubSystemRoleQuickNavDO>()
                .eq(SubSystemRoleQuickNavDO::getRoleId, roleId)
                .orderByAsc(SubSystemRoleQuickNavDO::getSort)
                .orderByAsc(SubSystemRoleQuickNavDO::getId));
    }

    default List<SubSystemRoleQuickNavDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(new LambdaQueryWrapperX<SubSystemRoleQuickNavDO>()
                .in(SubSystemRoleQuickNavDO::getRoleId, roleIds)
                .orderByAsc(SubSystemRoleQuickNavDO::getRoleId)
                .orderByAsc(SubSystemRoleQuickNavDO::getSort)
                .orderByAsc(SubSystemRoleQuickNavDO::getId));
    }

    @Delete("DELETE FROM sub_system_role_quick_nav WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("<script>DELETE FROM sub_system_role_quick_nav WHERE role_id IN "
            + "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    default List<SubSystemRoleQuickNavDO> selectListByMenuId(Long menuId) {
        return selectList(SubSystemRoleQuickNavDO::getMenuId, menuId);
    }

    @Delete("DELETE FROM sub_system_role_quick_nav WHERE menu_id = #{menuId}")
    void deleteByMenuId(@Param("menuId") Long menuId);

    @Delete("<script>DELETE FROM sub_system_role_quick_nav WHERE menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByMenuIds(@Param("menuIds") Collection<Long> menuIds);

}
