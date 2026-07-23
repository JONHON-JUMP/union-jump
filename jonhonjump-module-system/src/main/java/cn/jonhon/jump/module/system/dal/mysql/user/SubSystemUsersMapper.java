package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemUsersMapper extends BaseMapperX<SubSystemUsersDO> {

    default List<SubSystemUsersDO> selectListByMainUserId(Long mainUserId) {
        return selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getMainUserId, mainUserId)
                .orderByDesc(SubSystemUsersDO::getId));
    }

    default List<SubSystemUsersDO> selectListByMainUserIds(Collection<Long> mainUserIds) {
        return selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .in(SubSystemUsersDO::getMainUserId, mainUserIds));
    }

    default SubSystemUsersDO selectBySubSystemIdAndMainUserId(Long subSystemId, Long mainUserId) {
        return selectOne(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getMainUserId, mainUserId));
    }

    default SubSystemUsersDO selectBySubSystemIdAndUsername(Long subSystemId, String username) {
        return selectOne(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getUsername, username));
    }

    default PageResult<SubSystemUsersDO> selectPage(SubSystemUsersPageReqVO reqVO, Collection<String> teamCodes) {
        LambdaQueryWrapperX<SubSystemUsersDO> wrapper = new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eqIfPresent(SubSystemUsersDO::getSubSystemId, reqVO.getSubSystemId())
                .eqIfPresent(SubSystemUsersDO::getMainUserId, reqVO.getMainUserId())
                .likeIfPresent(SubSystemUsersDO::getUsername, reqVO.getUsername())
                .likeIfPresent(SubSystemUsersDO::getNickname, reqVO.getNickname())
                .likeIfPresent(SubSystemUsersDO::getWorkshopId, reqVO.getWorkshopId())
                .inIfPresent(SubSystemUsersDO::getTeamId, teamCodes)
                .betweenIfPresent(SubSystemUsersDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SubSystemUsersDO::getId);
        // 展示状态：unlinked=未关联；0=已关联且正常；1=已关联且禁用
        String status = reqVO.getStatus();
        if ("unlinked".equals(status) || "2".equals(status)) {
            wrapper.isNull(SubSystemUsersDO::getMainUserId);
        } else if ("0".equals(status)) {
            wrapper.isNotNull(SubSystemUsersDO::getMainUserId);
            wrapper.eq(SubSystemUsersDO::getStatus, "0");
        } else if ("1".equals(status)) {
            wrapper.isNotNull(SubSystemUsersDO::getMainUserId);
            wrapper.eq(SubSystemUsersDO::getStatus, "1");
        }
        return selectPage(reqVO, wrapper);
    }

    default Long selectCountBySubSystemId(Long subSystemId) {
        return selectCount(SubSystemUsersDO::getSubSystemId, subSystemId);
    }

    default List<SubSystemUsersDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .orderByDesc(SubSystemUsersDO::getId));
    }

    default Long selectCountBySubSystemIdAndTeamId(Long subSystemId, String teamId) {
        return selectCount(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getTeamId, teamId));
    }

}
