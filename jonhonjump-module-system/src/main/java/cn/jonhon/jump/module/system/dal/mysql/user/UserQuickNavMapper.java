package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserQuickNavDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 物理删除：快捷导航每次全量重写，软删会不断积压历史行
     */
    @Delete("DELETE FROM system_user_quick_nav WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    default List<UserQuickNavDO> selectListByMenuId(Long menuId) {
        return selectList(UserQuickNavDO::getMenuId, menuId);
    }

    default List<UserQuickNavDO> selectListByMenuIds(Collection<Long> menuIds) {
        return selectList(new LambdaQueryWrapperX<UserQuickNavDO>()
                .in(UserQuickNavDO::getMenuId, menuIds));
    }

    @Delete("DELETE FROM system_user_quick_nav WHERE menu_id = #{menuId}")
    void deleteByMenuId(@Param("menuId") Long menuId);

    @Delete("<script>DELETE FROM system_user_quick_nav WHERE menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByMenuIds(@Param("menuIds") Collection<Long> menuIds);

    @Delete("<script>DELETE FROM system_user_quick_nav WHERE user_id IN "
            + "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>"
            + " AND menu_id IN "
            + "<foreach collection='menuIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    void deleteByUserIdsAndMenuIds(@Param("userIds") Collection<Long> userIds,
                                   @Param("menuIds") Collection<Long> menuIds);

}
