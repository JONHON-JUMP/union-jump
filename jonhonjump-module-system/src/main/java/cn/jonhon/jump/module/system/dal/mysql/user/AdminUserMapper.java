package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByUserUid(String userUid) {
        return selectOne(AdminUserDO::getUserUid, userUid);
    }

    /**
     * 取某秒前缀下最大的 user_uid，用于同秒三位流水递增
     */
    default String selectMaxUserUidByPrefix(String prefix) {
        List<AdminUserDO> list = selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .likeRight(AdminUserDO::getUserUid, prefix)
                .orderByDesc(AdminUserDO::getUserUid)
                .last("LIMIT 1"));
        return list == null || list.isEmpty() ? null : list.get(0).getUserUid();
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default AdminUserDO selectByEmployeeNo(String employeeNo) {
        return selectOne(AdminUserDO::getEmployeeNo, employeeNo);
    }

    default AdminUserDO selectByDomainNo(String domainNo) {
        return selectOne(AdminUserDO::getDomainNo, domainNo);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Collection<Long> deptIds, Collection<Long> userIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getEmployeeNo, reqVO.getEmployeeNo())
                .likeIfPresent(AdminUserDO::getDomainNo, reqVO.getDomainNo())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getDeptId, deptIds)
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByDesc(AdminUserDO::getId));
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByMainUserSearch(String nickname, String employeeNo, String domainNo) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>()
                .likeIfPresent(AdminUserDO::getNickname, nickname)
                .likeIfPresent(AdminUserDO::getEmployeeNo, employeeNo)
                .likeIfPresent(AdminUserDO::getDomainNo, domainNo));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        return selectList(AdminUserDO::getDeptId, deptIds);
    }

}
