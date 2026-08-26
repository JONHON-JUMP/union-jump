package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemCommonMenuRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemCommonMenuSaveReqVO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** 通用菜单模板的保留 subSystemId */
    private static final Long COMMON_TEMPLATE_SUB_SYSTEM_ID = 0L;

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

    // ==================== 通用菜单（模板 + 多子系统副本同步） ====================

    @Override
    public List<SubSystemCommonMenuRespVO> getCommonMenuList() {
        List<SubSystemMenuDO> templates = subSystemMenuMapper.selectCommonTemplateList();
        if (CollUtil.isEmpty(templates)) {
            return Collections.emptyList();
        }
        List<SubSystemMenuDO> copies = subSystemMenuMapper.selectAllSharedCopies();
        Map<Long, List<SubSystemMenuDO>> copiesBySource = copies.stream()
                .collect(Collectors.groupingBy(SubSystemMenuDO::getSharedSourceId));
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(copies, SubSystemMenuDO::getSubSystemId)),
                SubSystemDO::getId);
        List<SubSystemCommonMenuRespVO> result = new ArrayList<>();
        for (SubSystemMenuDO template : templates) {
            SubSystemCommonMenuRespVO vo = new SubSystemCommonMenuRespVO();
            vo.setId(template.getId());
            vo.setName(template.getMenuName());
            vo.setType(convertTypeFromDb(template.getType()));
            vo.setPath(template.getPath());
            vo.setPermission(template.getPerms());
            vo.setIcon(template.getIcon());
            vo.setSort(template.getOrderNum());
            vo.setStatus(template.getStatus());
            vo.setManualUrl(template.getManualUrl());
            vo.setCreateTime(template.getCreateTime());
            List<SubSystemMenuDO> mounts = copiesBySource.getOrDefault(template.getId(), Collections.emptyList());
            vo.setSubSystemIds(mounts.stream().map(SubSystemMenuDO::getSubSystemId).collect(Collectors.toList()));
            vo.setSubSystemNames(mounts.stream()
                    .map(copy -> {
                        SubSystemDO sys = subSystemMap.get(copy.getSubSystemId());
                        return sys != null ? sys.getSystemName() : String.valueOf(copy.getSubSystemId());
                    })
                    .collect(Collectors.toList()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCommonMenu(SubSystemCommonMenuSaveReqVO createReqVO) {
        SubSystemMenuDO template = buildCommonTemplateFromReqVO(createReqVO, null);
        subSystemMenuMapper.insert(template);
        // 向选中的子系统复制副本
        Set<Long> subSystemIds = normalizeMountIds(createReqVO.getSubSystemIds());
        for (Long subSystemId : subSystemIds) {
            validateSubSystemExists(subSystemId);
            subSystemMenuMapper.insert(buildCommonCopy(template, subSystemId));
            subSystemPermissionContextService.evictBySubSystemId(subSystemId);
        }
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCommonMenu(SubSystemCommonMenuSaveReqVO updateReqVO) {
        SubSystemMenuDO template = validateCommonTemplateExists(updateReqVO.getId());
        // 1. 更新模板
        SubSystemMenuDO updateObj = buildCommonTemplateFromReqVO(updateReqVO, template.getId());
        subSystemMenuMapper.updateById(updateObj);
        // 2. 同步所有副本内容字段（不动 parentId / orderNum，位置由各子系统自行调整）
        List<SubSystemMenuDO> copies = subSystemMenuMapper.selectListBySharedSourceId(template.getId());
        Set<Long> mountedIds = copies.stream().map(SubSystemMenuDO::getSubSystemId).collect(Collectors.toSet());
        for (SubSystemMenuDO copy : copies) {
            SubSystemMenuDO copyUpdate = buildCommonCopy(updateObj, copy.getSubSystemId());
            copyUpdate.setId(copy.getId());
            copyUpdate.setParentId(copy.getParentId());
            copyUpdate.setOrderNum(copy.getOrderNum());
            // 显示/停用归各子系统自主管理：模板同步不得覆盖子系统单独设置的开关
            copyUpdate.setStatus(copy.getStatus());
            subSystemMenuMapper.updateById(copyUpdate);
            subSystemPermissionContextService.evictBySubSystemId(copy.getSubSystemId());
        }
        // 3. 对齐挂载：新增的子系统补副本；取消的删副本
        Set<Long> targetIds = normalizeMountIds(updateReqVO.getSubSystemIds());
        for (Long subSystemId : targetIds) {
            if (mountedIds.contains(subSystemId)) {
                continue;
            }
            validateSubSystemExists(subSystemId);
            subSystemMenuMapper.insert(buildCommonCopy(updateObj, subSystemId));
            subSystemPermissionContextService.evictBySubSystemId(subSystemId);
        }
        for (SubSystemMenuDO copy : copies) {
            if (!targetIds.contains(copy.getSubSystemId())) {
                validateCommonCopyDeletable(copy);
                deleteCommonCopyQuietly(copy);
                subSystemPermissionContextService.evictBySubSystemId(copy.getSubSystemId());
            }
        }
    }

    /** 取子系统名称用于错误提示定位 */
    private String subSystemMapName(Long subSystemId) {
        SubSystemDO sys = subSystemMapper.selectById(subSystemId);
        return sys != null ? sys.getSystemName() : String.valueOf(subSystemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommonMenu(Long id) {
        SubSystemMenuDO template = validateCommonTemplateExists(id);
        // 副本被角色/快捷导航引用、或有子菜单挂在其下时阻断，提示先到对应子系统处理
        for (SubSystemMenuDO copy : subSystemMenuMapper.selectListBySharedSourceId(id)) {
            validateCommonCopyDeletable(copy);
        }
        for (SubSystemMenuDO copy : subSystemMenuMapper.selectListBySharedSourceId(id)) {
            deleteCommonCopyQuietly(copy);
            subSystemPermissionContextService.evictBySubSystemId(copy.getSubSystemId());
        }
        subSystemMenuMapper.deleteById(template.getId());
    }

    /** 副本删除前置校验：已分配角色 / 已加快捷导航 / 存在子菜单挂在其下 均阻断 */
    private void validateCommonCopyDeletable(SubSystemMenuDO copy) {
        validateMenuNotAssigned(copy.getId());
        validateMenuNotInQuickNav(copy.getId());
        if (subSystemMenuMapper.selectCountByParentId(copy.getId()) > 0) {
            throw exception(SUB_SYSTEM_COMMON_MENU_COPY_HAS_CHILDREN,
                    subSystemMapName(copy.getSubSystemId()));
        }
    }

    private SubSystemMenuDO validateCommonTemplateExists(Long id) {
        SubSystemMenuDO template = subSystemMenuMapper.selectById(id);
        if (template == null || !COMMON_TEMPLATE_SUB_SYSTEM_ID.equals(template.getSubSystemId())) {
            throw exception(SUB_SYSTEM_MENU_NOT_EXISTS);
        }
        return template;
    }

    /** 通用菜单模板/副本内容统一从 SaveReqVO 构建 */
    private SubSystemMenuDO buildCommonTemplateFromReqVO(SubSystemCommonMenuSaveReqVO reqVO, Long id) {
        SubSystemMenuDO menu = new SubSystemMenuDO();
        menu.setId(id);
        menu.setSubSystemId(COMMON_TEMPLATE_SUB_SYSTEM_ID);
        menu.setSharedSourceId(null);
        menu.setParentId(ID_ROOT);
        applyCommonMenuContent(menu, reqVO);
        return menu;
    }

    private SubSystemMenuDO buildCommonCopy(SubSystemMenuDO template, Long subSystemId) {
        SubSystemMenuDO copy = new SubSystemMenuDO();
        copy.setSubSystemId(subSystemId);
        copy.setSharedSourceId(template.getId());
        copy.setParentId(ID_ROOT);
        copy.setOrderNum(template.getOrderNum());
        copy.setMenuName(template.getMenuName());
        copy.setType(template.getType());
        copy.setPath(template.getPath());
        copy.setPerms(template.getPerms());
        copy.setIcon(template.getIcon());
        copy.setStyleId(template.getStyleId());
        copy.setComponent(template.getComponent());
        copy.setComponentName(template.getComponentName());
        copy.setStatus(template.getStatus());
        copy.setVisible(template.getVisible());
        copy.setIsCache(template.getIsCache());
        copy.setAlwaysShow(template.getAlwaysShow());
        copy.setManualUrl(template.getManualUrl());
        copy.setIsFrame(template.getIsFrame());
        copy.setQuery(template.getQuery());
        copy.setRemark(template.getRemark());
        return copy;
    }

    private void applyCommonMenuContent(SubSystemMenuDO menu, SubSystemCommonMenuSaveReqVO reqVO) {
        menu.setMenuName(reqVO.getName());
        menu.setPerms(reqVO.getPermission());
        menu.setType(convertTypeToDb(reqVO.getType()));
        menu.setOrderNum(reqVO.getSort() != null ? reqVO.getSort() : 0);
        menu.setPath(reqVO.getPath());
        menu.setIcon(reqVO.getIcon());
        // 状态缺省为开启，避免 null 状态在各子系统菜单管理/门户树展示异常
        menu.setStatus(reqVO.getStatus() != null ? reqVO.getStatus() : 0);
        menu.setManualUrl(reqVO.getManualUrl());
        menu.setVisible(0);
        menu.setIsCache(0);
        menu.setAlwaysShow(0);
        menu.setIsFrame(isExternalLink(reqVO.getPath()) ? 0 : 1);
    }

    private void deleteCommonCopyQuietly(SubSystemMenuDO copy) {
        subSystemMenuMapper.deleteById(copy.getId());
        subSystemRoleMenuMapper.deleteListByMenuId(copy.getId());
    }

    private Set<Long> normalizeMountIds(List<Long> subSystemIds) {
        if (CollUtil.isEmpty(subSystemIds)) {
            return new HashSet<>();
        }
        return new HashSet<>(subSystemIds);
    }

}
