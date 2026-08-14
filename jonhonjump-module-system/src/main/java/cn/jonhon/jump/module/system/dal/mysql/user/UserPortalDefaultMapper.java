package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserPortalDefaultDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPortalDefaultMapper extends BaseMapperX<UserPortalDefaultDO> {

    default UserPortalDefaultDO selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<UserPortalDefaultDO>()
                .eq(UserPortalDefaultDO::getUserId, userId));
    }

    default void deleteByUserId(Long userId) {
        delete(UserPortalDefaultDO::getUserId, userId);
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(UserPortalDefaultDO::getSubSystemId, subSystemId);
    }

}
