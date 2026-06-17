package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersSimpleRespVO;

import java.util.List;

public interface SubSystemTeamService {

    PageResult<SubSystemTeamRespVO> getSubSystemTeamPage(SubSystemTeamPageReqVO pageReqVO);

    SubSystemTeamRespVO getSubSystemTeam(Long id);

    Long createSubSystemTeam(SubSystemTeamSaveReqVO createReqVO);

    void updateSubSystemTeam(SubSystemTeamSaveReqVO updateReqVO);

    void deleteSubSystemTeam(Long id);

    void deleteSubSystemTeamList(List<Long> ids);

    List<SubSystemUsersSimpleRespVO> getUserSimpleList(Long subSystemId);

}
