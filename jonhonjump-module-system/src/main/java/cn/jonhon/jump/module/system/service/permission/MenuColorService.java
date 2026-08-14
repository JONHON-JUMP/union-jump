package cn.jonhon.jump.module.system.service.permission;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MenuColorService {

    Long createMenuColor(MenuColorSaveReqVO createReqVO);

    void updateMenuColor(MenuColorSaveReqVO updateReqVO);

    void deleteMenuColor(Long id);

    void deleteMenuColorList(List<Long> ids);

    MenuColorDO getMenuColor(Long id);

    PageResult<MenuColorDO> getMenuColorPage(MenuColorPageReqVO pageReqVO);

    List<MenuColorDO> getMenuColorSimpleList();

    Map<Long, MenuColorDO> getMenuColorMap(Collection<Long> ids);

    void validateMenuColorExists(Long styleId);

    /**
     * 获取通用默认菜单样式（优先数据库 id=10，否则内置默认值）
     */
    MenuColorDO getDefaultMenuStyle();

}
