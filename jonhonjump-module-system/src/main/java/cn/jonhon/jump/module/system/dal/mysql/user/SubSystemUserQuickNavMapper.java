package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserQuickNavDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 物理删除：快捷导航每次全量重写，软删会不断积压历史行
     */
    @Delete("DELETE FROM sub_system_user_quick_nav WHERE user_id = #{userId} AND sub_system_id = #{subSystemId}")
    void deleteByUserIdAndSubSystemId(@Param("userId") Long userId, @Param("subSystemId") Long subSystemId);

    default List<SubSystemUserQuickNavDO> selectListByMenuId(Long menuId) {
        return selectList(SubSystemUserQuickNavDO::getMenuId, menuId);
    }

    default List<SubSystemUserQuickNavDO> selectListByMenuIds(Collection<Long> menuIds) {
        return selectList(new LambdaQueryWrapperX<SubSystemUserQuickNavDO>()
                .in(SubSystemUserQuickNavDO::getMenuId, menuIds));
    }

    @Delete("DELETE FROM sub_system_user_quick_nav WHERE menu_id = #{menuId}")
    void deleteByMenuId(@Param("menuId") Long menuId);

    @Delete("<script>DELETE FROM sub_system_user_quick_nav WHERE menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByMenuIds(@Param("menuIds") Collection<Long> menuIds);

    @Delete("<script>DELETE FROM sub_system_user_quick_nav WHERE sub_system_id = #{subSystemId}"
            + " AND user_id IN "
            + "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
            + " AND menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByUserIdsAndSubSystemIdAndMenuIds(@Param("userIds") Collection<Long> userIds,
                                                 @Param("subSystemId") Long subSystemId,
                                                 @Param("menuIds") Collection<Long> menuIds);

}
