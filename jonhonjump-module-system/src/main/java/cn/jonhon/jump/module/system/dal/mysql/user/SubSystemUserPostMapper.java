package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserPostDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemUserPostMapper extends BaseMapperX<SubSystemUserPostDO> {

    default List<SubSystemUserPostDO> selectListByUserId(Long userId) {
        return selectList(SubSystemUserPostDO::getUserId, userId);
    }

    default List<SubSystemUserPostDO> selectListByUserIds(Collection<Long> userIds) {
        return selectList(SubSystemUserPostDO::getUserId, userIds);
    }

    default void deleteListByUserId(Long userId) {
        delete(new LambdaQueryWrapper<SubSystemUserPostDO>().eq(SubSystemUserPostDO::getUserId, userId));
    }

    default Long selectCountByPostId(Long postId) {
        return selectCount(SubSystemUserPostDO::getPostId, postId);
    }

    default void deleteListByPostId(Long postId) {
        delete(new LambdaQueryWrapper<SubSystemUserPostDO>().eq(SubSystemUserPostDO::getPostId, postId));
    }

    default void deleteListByPostIds(Collection<Long> postIds) {
        delete(new LambdaQueryWrapper<SubSystemUserPostDO>().in(SubSystemUserPostDO::getPostId, postIds));
    }

}
