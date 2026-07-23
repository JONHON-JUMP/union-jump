package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SubSystemMapper extends BaseMapperX<SubSystemDO> {

    default PageResult<SubSystemDO> selectPage(SubSystemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SubSystemDO>()
                .likeIfPresent(SubSystemDO::getSystemName, reqVO.getSystemName())
                .eqIfPresent(SubSystemDO::getOauth2ClientId, reqVO.getOauth2ClientId())
                .eqIfPresent(SubSystemDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(SubSystemDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(SubSystemDO::getOauth2ClientId)
                .orderByDesc(SubSystemDO::getId));
    }

    default List<SubSystemDO> selectListOrderByOauth2ClientId() {
        return selectList(new LambdaQueryWrapperX<SubSystemDO>()
                .orderByAsc(SubSystemDO::getOauth2ClientId));
    }

    default SubSystemDO selectByOauth2ClientId(Long oauth2ClientId) {
        return selectOne(SubSystemDO::getOauth2ClientId, oauth2ClientId);
    }

    default List<SubSystemDO> selectListByIds(Collection<Long> ids) {
        return selectList(SubSystemDO::getId, ids);
    }

}
