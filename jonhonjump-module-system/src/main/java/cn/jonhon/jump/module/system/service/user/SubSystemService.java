package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemOAuth2ClientSimpleRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemSaveReqVO;

import java.util.List;

public interface SubSystemService {

    PageResult<SubSystemRespVO> getSubSystemPage(SubSystemPageReqVO pageReqVO);

    SubSystemRespVO getSubSystem(Long id);

    Long createSubSystem(SubSystemSaveReqVO createReqVO);

    void updateSubSystem(SubSystemSaveReqVO updateReqVO);

    void deleteSubSystem(Long id);

    void deleteSubSystemList(List<Long> ids);

    List<SubSystemOAuth2ClientSimpleRespVO> getOAuth2ClientSimpleList(Long excludeSubSystemId);

}
