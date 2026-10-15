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

    /**
     * 同系统多叶子时可能多行：优先系统级（车间空）挂接，否则取最新一条，避免 selectOne 炸。
     * 需要按叶子精确查请用 {@link #selectBySubSystemIdAndMainUserIdAndWorkshopId}。
     */
    default SubSystemUsersDO selectBySubSystemIdAndMainUserId(Long subSystemId, Long mainUserId) {
        List<SubSystemUsersDO> list = selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getMainUserId, mainUserId)
                .orderByDesc(SubSystemUsersDO::getId));
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        for (SubSystemUsersDO row : list) {
            String w = row.getWorkshopId();
            if (w == null || w.trim().isEmpty()) {
                return row;
            }
        }
        return list.get(0);
    }

    /** 同一 JUMP 人在同一业务系统下按叶子区分（workshopId 空表示系统级挂接） */
    default SubSystemUsersDO selectBySubSystemIdAndMainUserIdAndWorkshopId(
            Long subSystemId, Long mainUserId, String workshopId) {
        LambdaQueryWrapperX<SubSystemUsersDO> wrapper = new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getMainUserId, mainUserId);
        if (workshopId == null || workshopId.trim().isEmpty()) {
            wrapper.and(w -> w.isNull(SubSystemUsersDO::getWorkshopId)
                    .or()
                    .eq(SubSystemUsersDO::getWorkshopId, ""));
        } else {
            wrapper.eq(SubSystemUsersDO::getWorkshopId, workshopId.trim());
        }
        wrapper.orderByDesc(SubSystemUsersDO::getId);
        List<SubSystemUsersDO> list = selectList(wrapper);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default SubSystemUsersDO selectBySubSystemIdAndUsernameAndWorkshopId(
            Long subSystemId, String username, String workshopId) {
        LambdaQueryWrapperX<SubSystemUsersDO> wrapper = new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getUsername, username);
        if (workshopId == null || workshopId.trim().isEmpty()) {
            wrapper.and(w -> w.isNull(SubSystemUsersDO::getWorkshopId)
                    .or()
                    .eq(SubSystemUsersDO::getWorkshopId, ""));
        } else {
            wrapper.eq(SubSystemUsersDO::getWorkshopId, workshopId.trim());
        }
        wrapper.orderByDesc(SubSystemUsersDO::getId);
        List<SubSystemUsersDO> list = selectList(wrapper);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    default List<SubSystemUsersDO> selectListByUsername(String username) {
        return selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getUsername, username));
    }

    /**
     * 同工号可挂多车间：优先车间空的行，否则最新一条。按叶子精确查请用
     * {@link #selectBySubSystemIdAndUsernameAndWorkshopId}。
     */
    default SubSystemUsersDO selectBySubSystemIdAndUsername(Long subSystemId, String username) {
        List<SubSystemUsersDO> list = selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getUsername, username)
                .orderByDesc(SubSystemUsersDO::getId));
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        for (SubSystemUsersDO row : list) {
            String w = row.getWorkshopId();
            if (w == null || w.trim().isEmpty()) {
                return row;
            }
        }
        return list.get(0);
    }

    default List<SubSystemUsersDO> selectListByUsernameAndUsernameWithWorkshop(String username) {
        return selectList(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getUsername, username)
                .eq(SubSystemUsersDO::getUsernameWithWorkshop, "1")
                .orderByDesc(SubSystemUsersDO::getId));
    }

    default PageResult<SubSystemUsersDO> selectPage(SubSystemUsersPageReqVO reqVO, Collection<String> teamCodes,
                                                    Collection<Long> allowedSubSystemIds) {
        LambdaQueryWrapperX<SubSystemUsersDO> wrapper = new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eqIfPresent(SubSystemUsersDO::getSubSystemId, reqVO.getSubSystemId())
                .inIfPresent(SubSystemUsersDO::getSubSystemId, allowedSubSystemIds)
                .eqIfPresent(SubSystemUsersDO::getMainUserId, reqVO.getMainUserId())
                .eqIfPresent(SubSystemUsersDO::getEmployeeRegistered, reqVO.getEmployeeRegistered())
                .likeIfPresent(SubSystemUsersDO::getUsername, reqVO.getUsername())
                .likeIfPresent(SubSystemUsersDO::getNickname, reqVO.getNickname())
                .likeIfPresent(SubSystemUsersDO::getWorkshopId, reqVO.getWorkshopId())
                .inIfPresent(SubSystemUsersDO::getTeamId, teamCodes)
                .betweenIfPresent(SubSystemUsersDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SubSystemUsersDO::getId);
        // 展示状态：unlinked=未关联（空或 <=0）；0=已挂真实 JUMP 用户且正常；1=已挂且禁用
        String status = reqVO.getStatus();
        if ("unlinked".equals(status) || "2".equals(status)) {
            wrapper.and(w -> w.isNull(SubSystemUsersDO::getMainUserId)
                    .or()
                    .le(SubSystemUsersDO::getMainUserId, 0));
        } else if ("0".equals(status)) {
            wrapper.gt(SubSystemUsersDO::getMainUserId, 0);
            wrapper.eq(SubSystemUsersDO::getStatus, "0");
        } else if ("1".equals(status)) {
            wrapper.gt(SubSystemUsersDO::getMainUserId, 0);
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

    default Long selectCountBySubSystemIdAndWorkshopId(Long subSystemId, String workshopId) {
        return selectCount(new LambdaQueryWrapperX<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getSubSystemId, subSystemId)
                .eq(SubSystemUsersDO::getWorkshopId, workshopId));
    }

}
