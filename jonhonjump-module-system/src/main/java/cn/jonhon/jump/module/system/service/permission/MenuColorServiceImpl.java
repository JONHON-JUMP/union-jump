package cn.jonhon.jump.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.MenuColorMapper;
import cn.jonhon.jump.module.system.dal.mysql.permission.MenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import cn.jonhon.jump.module.system.util.MenuStyleHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class MenuColorServiceImpl implements MenuColorService {

    @Resource
    private MenuColorMapper menuColorMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;

    @Override
    public Long createMenuColor(MenuColorSaveReqVO createReqVO) {
        validateNameUnique(null, createReqVO.getName());
        MenuColorDO color = BeanUtils.toBean(createReqVO, MenuColorDO.class);
        color.setShape(defaultShape(createReqVO.getShape()));
        menuColorMapper.insert(color);
        return color.getId();
    }

    @Override
    public void updateMenuColor(MenuColorSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateNameUnique(updateReqVO.getId(), updateReqVO.getName());
        MenuColorDO updateObj = BeanUtils.toBean(updateReqVO, MenuColorDO.class);
        updateObj.setShape(defaultShape(updateReqVO.getShape()));
        menuColorMapper.updateById(updateObj);
    }

    @Override
    public void deleteMenuColor(Long id) {
        validateExists(id);
        validateNotInUse(id);
        menuColorMapper.deleteById(id);
    }

    @Override
    public void deleteMenuColorList(List<Long> ids) {
        ids.forEach(id -> {
            validateExists(id);
            validateNotInUse(id);
        });
        menuColorMapper.deleteByIds(ids);
    }

    @Override
    public MenuColorDO getMenuColor(Long id) {
        return menuColorMapper.selectById(id);
    }

    @Override
    public PageResult<MenuColorDO> getMenuColorPage(MenuColorPageReqVO pageReqVO) {
        return menuColorMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MenuColorDO> getMenuColorSimpleList() {
        return menuColorMapper.selectListByStatus(0);
    }

    @Override
    public Map<Long, MenuColorDO> getMenuColorMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return convertMap(menuColorMapper.selectListByIds(ids), MenuColorDO::getId);
    }

    @Override
    public void validateMenuColorExists(Long styleId) {
        if (styleId == null) {
            return;
        }
        validateExists(styleId);
    }

    @Override
    public MenuColorDO getDefaultMenuStyle() {
        MenuColorDO style = menuColorMapper.selectById(MenuStyleHelper.DEFAULT_STYLE_ID);
        if (style != null && CommonStatusEnum.ENABLE.getStatus().equals(style.getStatus())) {
            return style;
        }
        return MenuStyleHelper.defaultStyle();
    }

    private void validateExists(Long id) {
        if (menuColorMapper.selectById(id) == null) {
            throw exception(MENU_STYLE_NOT_EXISTS);
        }
    }

    private void validateNameUnique(Long id, String name) {
        MenuColorDO color = menuColorMapper.selectByName(name);
        if (color == null) {
            return;
        }
        if (id == null || !color.getId().equals(id)) {
            throw exception(MENU_STYLE_NAME_DUPLICATE);
        }
    }

    private void validateNotInUse(Long styleId) {
        Long mainCount = menuMapper.selectCount(MenuDO::getStyleId, styleId);
        if (mainCount != null && mainCount > 0) {
            throw exception(MENU_STYLE_IN_USE);
        }
        Long subCount = subSystemMenuMapper.selectCount(SubSystemMenuDO::getStyleId, styleId);
        if (subCount != null && subCount > 0) {
            throw exception(MENU_STYLE_IN_USE);
        }
    }

    private String defaultShape(String shape) {
        return StrUtil.blankToDefault(shape, MenuStyleHelper.DEFAULT_SHAPE);
    }

}
