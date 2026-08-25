package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemCommonMenuRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemCommonMenuSaveReqVO;
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

    // ========== 通用菜单（模板 + 多子系统副本同步） ==========

    /** 通用菜单模板列表（含已挂载子系统） */
    List<SubSystemCommonMenuRespVO> getCommonMenuList();

    /** 创建通用菜单模板，并向选中的子系统复制副本 */
    Long createCommonMenu(SubSystemCommonMenuSaveReqVO createReqVO);

    /** 更新通用菜单模板：同步所有副本内容字段，并按挂载列表增/删副本 */
    void updateCommonMenu(SubSystemCommonMenuSaveReqVO updateReqVO);

    /** 删除通用菜单模板及其全部副本（被角色/快捷导航引用的副本会阻断并提示） */
    void deleteCommonMenu(Long id);

}
