package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemHangableNodeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;

import java.util.List;

public interface SubSystemWorkshopService {

    /** 子表叶子列表（启用态字典/辅助筛选） */
    List<SubSystemWorkshopRespVO> getEnabledWorkshopList();

    /**
     * 人员可挂外部树：仅子表叶子可选；一对一一条叶子，多叶子挂在父系统下。
     */
    List<SubSystemHangableNodeRespVO> getHangableTree();

    PageResult<SubSystemWorkshopRespVO> getWorkshopPage(SubSystemWorkshopPageReqVO pageReqVO);

    SubSystemWorkshopRespVO getWorkshop(Long id);

    Long createWorkshop(SubSystemWorkshopSaveReqVO createReqVO);

    void updateWorkshop(SubSystemWorkshopSaveReqVO updateReqVO);

    void deleteWorkshop(Long id);

    void deleteWorkshopList(List<Long> ids);

}
