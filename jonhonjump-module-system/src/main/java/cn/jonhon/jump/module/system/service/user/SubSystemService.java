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

    /**
     * 仅用于人员接口接入：创建不绑定 OAuth2 的接口目标系统（可后续再在外部系统管理中补绑）
     */
    Long createApiOnlySubSystem(String systemName);

    /**
     * 仅改显示名称（接口接入树重命名、纠正 MES4200 等误名）
     */
    void updateSystemName(Long id, String systemName);

    void updateSubSystem(SubSystemSaveReqVO updateReqVO);

    void deleteSubSystem(Long id);

    void deleteSubSystemList(List<Long> ids);

    List<SubSystemOAuth2ClientSimpleRespVO> getOAuth2ClientSimpleList(Long excludeSubSystemId);

}
