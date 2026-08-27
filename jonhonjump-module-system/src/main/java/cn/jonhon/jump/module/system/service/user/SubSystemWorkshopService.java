package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;

import java.util.List;

/**
 * 外部系统车间 Service 接口（JUMP 部门 ↔ 子系统车间映射）
 */
public interface SubSystemWorkshopService {

    PageResult<SubSystemWorkshopRespVO> getSubSystemWorkshopPage(SubSystemWorkshopPageReqVO pageReqVO);

    SubSystemWorkshopRespVO getSubSystemWorkshop(Long id);

    Long createSubSystemWorkshop(SubSystemWorkshopSaveReqVO createReqVO);

    void updateSubSystemWorkshop(SubSystemWorkshopSaveReqVO updateReqVO);

    void deleteSubSystemWorkshop(Long id);

    void deleteSubSystemWorkshopList(List<Long> ids);

    /**
     * 车间精简列表（用户新增下拉）
     * 优先：系统 + 部门映射；没有则该系统全部；再没有则全部车间（去重）
     */
    List<SubSystemWorkshopSimpleRespVO> getWorkshopSimpleList(Long subSystemId, Long deptId);

    /**
     * 按 JUMP 部门查映射车间（用户创建联动用，未映射返回 null）
     */
    SubSystemWorkshopSimpleRespVO getWorkshopByDept(Long subSystemId, Long deptId);

}
