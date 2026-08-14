package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuListReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemMenuSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMenuMapper;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class SubSystemMenuServiceImpl implements SubSystemMenuService {

    private static final Long ID_ROOT = 0L;

    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemRoleMenuMapper subSystemRoleMenuMapper;
    @Resource
    private SubSystemUserQuickNavService subSystemUserQuickNavService;
    @Resource
    private SubSystemRoleQuickNavService subSystemRoleQuickNavService;
    @Resource
    private cn.jonhon.jump.module.system.service.permission.MenuColorService menuColorService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;

    @Override
    public List<SubSystemMenuRespVO> getSubSystemMenuList(SubSystemMenuListReqVO reqVO) {
        if (reqVO.getSubSystemId() != null) {
            validateSubSystemExists(reqVO.getSubSystemId());
        }
        List<SubSystemMenuDO> list = subSystemMenuMapper.selectList(reqVO);
        return buildRespList(list);
    }

    @Override
    public SubSystemMenuRespVO getSubSystemMenu(Long id) {
        SubSystemMenuDO menu = validateSubSystemMenuExists(id);
        List<SubSystemMenuRespVO> list = buildRespList(Collections.singletonList(menu));
        return list.isEmpty() ? convertToRespVO(menu, null) : list.get(0);
    }

    @Override
    public Long createSubSystemMenu(SubSystemMenuSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validateParentMenu(createReqVO.getSubSystemId(), createReqVO.getParentId(), null);
        validateMenuName(createReqVO.getSubSystemId(), createReqVO.getParentId(), createReqVO.getName(), null);
        normalizeMenuStyle(createReqVO);

        SubSystemMenuDO menu = convertToDO(createReqVO);
        subSystemMenuMapper.insert(menu);
        // 新建后也要失效门户 my-menus / 权限缓存，否则首页卡片仍是旧树
        subSystemPermissionContextService.evictByMenuId(menu.getId());
        return menu.getId();
    }

    @Override
    public void updateSubSystemMenu(SubSystemMenuSaveReqVO updateReqVO) {
        validateSubSystemMenuExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateParentMenu(updateReqVO.getSubSystemId(), updateReqVO.getParentId(), updateReqVO.getId());
        validateMenuName(updateReqVO.getSubSystemId(), updateReqVO.getParentId(), updateReqVO.getName(), updateReqVO.getId());
        normalizeMenuStyle(updateReqVO);

        SubSystemMenuDO updateObj = convertToDO(updateReqVO);
        subSystemMenuMapper.updateById(updateObj);
        subSystemPermissionContextService.evictByMenuId(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemMenu(Long id) {
        if (subSystemMenuMapper.selectCountByParentId(id) > 0) {
            throw exception(SUB_SYSTEM_MENU_EXISTS_CHILDREN);
        }
        validateSubSystemMenuExists(id);
        validateMenuNotAssigned(id);
        validateMenuNotInQuickNav(id);
        subSystemPermissionContextService.evictByMenuId(id);
        subSystemMenuMapper.deleteById(id);
        subSystemRoleMenuMapper.deleteListByMenuId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemMenuList(List<Long> ids) {
        ids.forEach(id -> {
            if (subSystemMenuMapper.selectCountByParentId(id) > 0) {
                throw exception(SUB_SYSTEM_MENU_EXISTS_CHILDREN);
            }
            validateSubSystemMenuExists(id);
            validateMenuNotAssigned(id);
            validateMenuNotInQuickNav(id);
            subSystemPermissionContextService.evictByMenuId(id);
        });
        subSystemMenuMapper.deleteByIds(ids);
        subSystemRoleMenuMapper.deleteListByMenuIds(ids);
    }

    private List<SubSystemMenuRespVO> buildRespList(List<SubSystemMenuDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemMenuDO::getSubSystemId)),
                SubSystemDO::getId);
        return list.stream()
                .map(menu -> convertToRespVO(menu, subSystemMap.get(menu.getSubSystemId())))
                .collect(Collectors.toList());
    }

    private SubSystemMenuRespVO convertToRespVO(SubSystemMenuDO menu, SubSystemDO subSystem) {
        SubSystemMenuRespVO vo = new SubSystemMenuRespVO();
        vo.setId(menu.getId());
        vo.setSubSystemId(menu.getSubSystemId());
        if (subSystem != null) {
            vo.setClientName(subSystem.getSystemName());
        }
        vo.setName(menu.getMenuName());
        vo.setPermission(menu.getPerms());
        vo.setType(convertTypeFromDb(menu.getType()));
        vo.setSort(menu.getOrderNum());
        vo.setParentId(menu.getParentId());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setStyleId(menu.getStyleId());
        vo.setComponent(menu.getComponent());
        vo.setComponentName(menu.getComponentName());
        vo.setStatus(menu.getStatus());
        vo.setVisible(menu.getVisible() != null && menu.getVisible() == 0);
        vo.setKeepAlive(menu.getIsCache() != null && menu.getIsCache() == 0);
        vo.setAlwaysShow(menu.getAlwaysShow() != null && menu.getAlwaysShow() == 1);
        vo.setManualUrl(menu.getManualUrl());
        vo.setCreateTime(menu.getCreateTime());
        return vo;
    }

    private SubSystemMenuDO convertToDO(SubSystemMenuSaveReqVO reqVO) {
        SubSystemMenuDO menu = new SubSystemMenuDO();
        menu.setId(reqVO.getId());
        menu.setSubSystemId(reqVO.getSubSystemId());
        menu.setMenuName(reqVO.getName());
        menu.setPerms(reqVO.getPermission());
        menu.setType(convertTypeToDb(reqVO.getType()));
        menu.setOrderNum(reqVO.getSort());
        menu.setParentId(reqVO.getParentId());
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        menu.setStyleId(reqVO.getStyleId());
        menu.setComponent(reqVO.getComponent());
        menu.setComponentName(reqVO.getComponentName());
        menu.setStatus(reqVO.getStatus());
        menu.setVisible(Boolean.FALSE.equals(reqVO.getVisible()) ? 1 : 0);
        menu.setIsCache(Boolean.FALSE.equals(reqVO.getKeepAlive()) ? 1 : 0);
        menu.setAlwaysShow(Boolean.TRUE.equals(reqVO.getAlwaysShow()) ? 1 : 0);
        menu.setManualUrl(reqVO.getManualUrl());
        menu.setIsFrame(isExternalLink(reqVO.getPath()) ? 0 : 1);
        return menu;
    }

    /** 仅一级菜单可配置颜色，子菜单继承一级菜单颜色 */
    private void normalizeMenuStyle(SubSystemMenuSaveReqVO reqVO) {
        if (!ID_ROOT.equals(reqVO.getParentId())) {
            reqVO.setStyleId(null);
            return;
        }
        menuColorService.validateMenuColorExists(reqVO.getStyleId());
    }

    private boolean isExternalLink(String path) {
        return StrUtil.isNotBlank(path) && (path.startsWith("http://") || path.startsWith("https://"));
    }

    private String convertTypeToDb(Integer type) {
        if (MenuTypeEnum.DIR.getType().equals(type)) {
            return "M";
        }
        if (MenuTypeEnum.MENU.getType().equals(type)) {
            return "C";
        }
        if (MenuTypeEnum.BUTTON.getType().equals(type)) {
            return "F";
        }
        return "M";
    }

    private Integer convertTypeFromDb(String type) {
        if ("M".equals(type)) {
            return MenuTypeEnum.DIR.getType();
        }
        if ("C".equals(type)) {
            return MenuTypeEnum.MENU.getType();
        }
        if ("F".equals(type)) {
            return MenuTypeEnum.BUTTON.getType();
        }
        return MenuTypeEnum.DIR.getType();
    }

    private SubSystemDO validateSubSystemExists(Long subSystemId) {
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        return subSystem;
    }

    private SubSystemMenuDO validateSubSystemMenuExists(Long id) {
        SubSystemMenuDO menu = subSystemMenuMapper.selectById(id);
        if (menu == null) {
            throw exception(SUB_SYSTEM_MENU_NOT_EXISTS);
        }
        return menu;
    }

    private void validateMenuNotAssigned(Long menuId) {
        Long count = subSystemRoleMenuMapper.selectCountByMenuId(menuId);
        if (count != null && count > 0) {
            throw exception(SUB_SYSTEM_MENU_HAS_ROLES);
        }
    }

    private void validateMenuNotInQuickNav(Long menuId) {
        if (subSystemUserQuickNavService.existsByMenuId(menuId)
                || subSystemRoleQuickNavService.existsByMenuId(menuId)) {
            throw exception(SUB_SYSTEM_MENU_HAS_QUICK_NAV);
        }
    }

    private void validateParentMenu(Long subSystemId, Long parentId, Long selfId) {
        if (ObjectUtil.equal(ID_ROOT, parentId)) {
            return;
        }
        if (ObjectUtil.equal(parentId, selfId)) {
            throw exception(SUB_SYSTEM_MENU_PARENT_ERROR);
        }
        SubSystemMenuDO parentMenu = subSystemMenuMapper.selectById(parentId);
        if (parentMenu == null || !ObjectUtil.equal(parentMenu.getSubSystemId(), subSystemId)) {
            throw exception(SUB_SYSTEM_MENU_PARENT_NOT_EXISTS);
        }
    }

    private void validateMenuName(Long subSystemId, Long parentId, String name, Long selfId) {
        if (StrUtil.isBlank(name)) {
            return;
        }
        SubSystemMenuDO menu = subSystemMenuMapper.selectBySubSystemIdAndParentIdAndName(subSystemId, parentId, name);
        if (menu != null && !ObjectUtil.equal(menu.getId(), selfId)) {
            log.warn("[validateMenuName][子系统({}) 菜单名称({}) 在同一父菜单下已存在重名，仅提醒不拦截]",
                    subSystemId, name);
        }
    }

}
