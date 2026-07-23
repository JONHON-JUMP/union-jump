package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserRoleDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemUserRoleMapper extends BaseMapperX<SubSystemUserRoleDO> {

    default List<SubSystemUserRoleDO> selectListByUserId(Long userId) {
        return selectList(SubSystemUserRoleDO::getUserId, userId);
    }

    default List<SubSystemUserRoleDO> selectListByUserIds(Collection<Long> userIds) {
        return selectList(SubSystemUserRoleDO::getUserId, userIds);
    }

    default void deleteListByUserId(Long userId) {
        delete(new LambdaQueryWrapper<SubSystemUserRoleDO>().eq(SubSystemUserRoleDO::getUserId, userId));
    }

    default List<SubSystemUserRoleDO> selectListByRoleId(Long roleId) {
        return selectList(SubSystemUserRoleDO::getRoleId, roleId);
    }

    default Long selectCountByRoleId(Long roleId) {
        return selectCount(SubSystemUserRoleDO::getRoleId, roleId);
    }

}
