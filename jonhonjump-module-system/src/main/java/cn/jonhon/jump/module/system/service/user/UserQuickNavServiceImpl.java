package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavCandidateRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.UserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.UserQuickNavDO;
import cn.jonhon.jump.module.system.dal.mysql.user.UserQuickNavMapper;
import cn.jonhon.jump.module.system.enums.permission.MenuTypeEnum;
import cn.jonhon.jump.module.system.service.permission.MenuService;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import cn.jonhon.jump.module.system.service.permission.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.filterList;
import static cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO.ID_ROOT;

/**
 * 用户快捷导航 Service 实现（主系统）
 */
@Service
@Validated
public class UserQuickNavServiceImpl implements UserQuickNavService {

    @Resource
    private UserQuickNavMapper userQuickNavMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;

    @Override
    public UserQuickNavRespVO getUserQuickNav(Long userId) {
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);
        List<UserQuickNavDO> savedList = userQuickNavMapper.selectListByUserId(userId);
        boolean configured = CollUtil.isNotEmpty(savedList);
        List<Long> menuIds = savedList.stream()
                .map(UserQuickNavDO::getMenuId)
                .filter(allowedMenuIds::contains)
                .collect(Collectors.toList());
        return new UserQuickNavRespVO(menuIds, configured);
    }

    @Override
    public List<UserQuickNavCandidateRespVO> getCandidateList(Long userId) {
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);
        if (CollUtil.isEmpty(allowedMenuIds)) {
            return Collections.emptyList();
        }
        Map<Long, MenuDO> menuMap = convertMap(menuService.getMenuList(), MenuDO::getId);
        Set<Long> treeMenuIds = new HashSet<>();
        for (Long menuId : allowedMenuIds) {
            treeMenuIds.add(menuId);
            Long parentId = menuMap.get(menuId).getParentId();
            while (parentId != null && !ID_ROOT.equals(parentId)) {
                MenuDO parent = menuMap.get(parentId);
                if (parent == null) {
                    break;
                }
                if (!Boolean.FALSE.equals(parent.getVisible())) {
                    treeMenuIds.add(parentId);
                }
                parentId = parent.getParentId();
            }
        }
        List<MenuDO> treeMenus = menuService.filterDisableMenus(
                menuService.getMenuList(treeMenuIds).stream()
                        .filter(menu -> !MenuTypeEnum.BUTTON.getType().equals(menu.getType()))
                        .filter(menu -> isMenuShownInSidebar(menu, menuMap))
                        .collect(Collectors.toList()));
        return buildCandidateTree(treeMenus, menuMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserQuickNav(Long userId, List<Long> menuIds) {
        Set<Long> allowedMenuIds = getAllowedMenuIds(userId);
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());

        userQuickNavMapper.deleteByUserId(userId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            UserQuickNavDO record = new UserQuickNavDO();
            record.setUserId(userId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            userQuickNavMapper.insert(record);
        }
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        userQuickNavMapper.deleteByMenuId(menuId);
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        userQuickNavMapper.deleteByMenuIds(menuIds);
    }

    private List<UserQuickNavCandidateRespVO> buildCandidateTree(List<MenuDO> menuList, Map<Long, MenuDO> menuMap) {
        if (CollUtil.isEmpty(menuList)) {
            return Collections.emptyList();
        }
        menuList.sort(Comparator.comparing(MenuDO::getSort).thenComparing(MenuDO::getId));
        Map<Long, UserQuickNavCandidateRespVO> treeNodeMap = new LinkedHashMap<>();
        menuList.forEach(menu -> {
            UserQuickNavCandidateRespVO node = new UserQuickNavCandidateRespVO();
            node.setId(menu.getId());
            node.setParentId(resolveVisibleParentId(menu.getParentId(), menuMap));
            node.setName(menu.getName());
            node.setType(menu.getType());
            node.setIcon(menu.getIcon());
            treeNodeMap.put(menu.getId(), node);
        });
        treeNodeMap.values().stream()
                .filter(node -> !ID_ROOT.equals(node.getParentId()))
                .forEach(childNode -> {
                    UserQuickNavCandidateRespVO parentNode = treeNodeMap.get(childNode.getParentId());
                    if (parentNode == null) {
                        return;
                    }
                    if (parentNode.getChildren() == null) {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(childNode);
                });
        return filterList(treeNodeMap.values(), node -> ID_ROOT.equals(node.getParentId()));
    }

    /**
     * 与侧边栏一致：菜单自身及全部祖先均为「显示」时，才可在快捷导航中展示。
     */
    private boolean isMenuShownInSidebar(MenuDO menu, Map<Long, MenuDO> menuMap) {
        if (Boolean.FALSE.equals(menu.getVisible())) {
            return false;
        }
        Long parentId = menu.getParentId();
        if (parentId == null || ID_ROOT.equals(parentId)) {
            return true;
        }
        MenuDO parent = menuMap.get(parentId);
        if (parent == null) {
            return true;
        }
        return isMenuShownInSidebar(parent, menuMap);
    }

    private Long resolveVisibleParentId(Long parentId, Map<Long, MenuDO> menuMap) {
        while (parentId != null && !ID_ROOT.equals(parentId)) {
            MenuDO parent = menuMap.get(parentId);
            if (parent == null) {
                return ID_ROOT;
            }
            if (!Boolean.FALSE.equals(parent.getVisible())) {
                return parentId;
            }
            parentId = parent.getParentId();
        }
        return ID_ROOT;
    }

    private Set<Long> getAllowedMenuIds(Long userId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        if (CollUtil.isEmpty(roles)) {
            return Collections.emptySet();
        }

        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        Map<Long, MenuDO> menuMap = convertMap(menuService.getMenuList(), MenuDO::getId);
        List<MenuDO> menus = menuService.filterDisableMenus(menuService.getMenuList(menuIds));
        return menus.stream()
                .filter(menu -> MenuTypeEnum.MENU.getType().equals(menu.getType()))
                .filter(menu -> isMenuShownInSidebar(menu, menuMap))
                .map(MenuDO::getId)
                .collect(Collectors.toSet());
    }

}
