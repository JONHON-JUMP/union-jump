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
     * 车间精简列表（人员/用户表单下拉用）
     *
     * @param subSystemId 外部系统 ID
     * @param deptId      可选：按 JUMP 部门过滤（用户创建联动时传部门，未映射返回空）
     */
    List<SubSystemWorkshopSimpleRespVO> getWorkshopSimpleList(Long subSystemId, Long deptId);

    /**
     * 按 JUMP 部门查映射车间（用户创建联动用，未映射返回 null）
     */
    SubSystemWorkshopSimpleRespVO getWorkshopByDept(Long subSystemId, Long deptId);

}
