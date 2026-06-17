package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuListReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuSaveReqVO;

import java.util.List;

public interface SubSystemMenuService {

    List<SubSystemMenuRespVO> getSubSystemMenuList(SubSystemMenuListReqVO reqVO);

    SubSystemMenuRespVO getSubSystemMenu(Long id);

    Long createSubSystemMenu(SubSystemMenuSaveReqVO createReqVO);

    void updateSubSystemMenu(SubSystemMenuSaveReqVO updateReqVO);

    void deleteSubSystemMenu(Long id);

    void deleteSubSystemMenuList(List<Long> ids);

}
