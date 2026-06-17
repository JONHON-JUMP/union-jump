package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavCandidateRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.quicknav.SubSystemUserQuickNavRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemMenuDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUserQuickNavDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMenuMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUserQuickNavMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.filterList;

/**
 * 用户外部子系统快捷导航 Service 实现
 */
@Service
@Validated
public class SubSystemUserQuickNavServiceImpl implements SubSystemUserQuickNavService {

    @Resource
    private SubSystemUserQuickNavMapper subSystemUserQuickNavMapper;
    @Resource
    private SubSystemUsersService subSystemUsersService;
    @Resource
    private SubSystemMenuMapper subSystemMenuMapper;

    @Override
    public SubSystemUserQuickNavRespVO getUserQuickNav(Long userId, Long subSystemId) {
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);
        List<SubSystemUserQuickNavDO> savedList = subSystemUserQuickNavMapper
                .selectListByUserIdAndSubSystemId(userId, subSystemId);
        boolean configured = CollUtil.isNotEmpty(savedList);
        List<Long> menuIds = savedList.stream()
                .map(SubSystemUserQuickNavDO::getMenuId)
                .filter(allowedMenuIds::contains)
                .collect(Collectors.toList());
        return new SubSystemUserQuickNavRespVO(menuIds, configured);
    }

    @Override
    public List<SubSystemUserQuickNavCandidateRespVO> getCandidateList(Long userId, Long subSystemId) {
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);
        if (CollUtil.isEmpty(allowedMenuIds)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemMenuDO> menuMap = convertMap(
                subSystemMenuMapper.selectListBySubSystemId(subSystemId), SubSystemMenuDO::getId);
        Set<Long> treeMenuIds = new HashSet<>();
        for (Long menuId : allowedMenuIds) {
            treeMenuIds.add(menuId);
            Long parentId = menuMap.get(menuId).getParentId();
            while (parentId != null && parentId != 0L) {
                treeMenuIds.add(parentId);
                SubSystemMenuDO parent = menuMap.get(parentId);
                if (parent == null) {
                    break;
                }
                parentId = parent.getParentId();
            }
        }
        List<SubSystemMenuDO> treeMenus = treeMenuIds.stream()
                .map(menuMap::get)
                .filter(Objects::nonNull)
                .filter(menu -> menu.getStatus() != null && menu.getStatus() == 0)
                .filter(menu -> !"F".equals(menu.getType()))
                .sorted(Comparator.comparing(SubSystemMenuDO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemMenuDO::getId))
                .collect(Collectors.toList());
        return buildCandidateTree(treeMenus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserQuickNav(Long userId, Long subSystemId, List<Long> menuIds) {
        Set<Long> allowedMenuIds = subSystemUsersService.getAllowedQuickNavMenuIds(userId, subSystemId);
        List<Long> validMenuIds = CollUtil.isEmpty(menuIds) ? Collections.emptyList()
                : menuIds.stream().filter(allowedMenuIds::contains).distinct().collect(Collectors.toList());

        subSystemUserQuickNavMapper.deleteByUserIdAndSubSystemId(userId, subSystemId);
        for (int i = 0; i < validMenuIds.size(); i++) {
            SubSystemUserQuickNavDO record = new SubSystemUserQuickNavDO();
            record.setUserId(userId);
            record.setSubSystemId(subSystemId);
            record.setMenuId(validMenuIds.get(i));
            record.setSort(i);
            subSystemUserQuickNavMapper.insert(record);
        }
    }

    @Override
    public void deleteByMenuId(Long menuId) {
        subSystemUserQuickNavMapper.deleteByMenuId(menuId);
    }

    @Override
    public void deleteByMenuIds(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        subSystemUserQuickNavMapper.deleteByMenuIds(menuIds);
    }

    private List<SubSystemUserQuickNavCandidateRespVO> buildCandidateTree(List<SubSystemMenuDO> menuList) {
        if (CollUtil.isEmpty(menuList)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemUserQuickNavCandidateRespVO> treeNodeMap = new LinkedHashMap<>();
        menuList.forEach(menu -> {
            SubSystemUserQuickNavCandidateRespVO node = new SubSystemUserQuickNavCandidateRespVO();
            node.setId(menu.getId());
            node.setParentId(menu.getParentId());
            node.setName(menu.getMenuName());
            node.setType(menu.getType());
            node.setIcon(menu.getIcon());
            treeNodeMap.put(menu.getId(), node);
        });
        treeNodeMap.values().stream()
                .filter(node -> node.getParentId() != null && node.getParentId() != 0L)
                .forEach(childNode -> {
                    SubSystemUserQuickNavCandidateRespVO parentNode = treeNodeMap.get(childNode.getParentId());
                    if (parentNode == null) {
                        return;
                    }
                    if (parentNode.getChildren() == null) {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(childNode);
                });
        return filterList(treeNodeMap.values(), node -> node.getParentId() == null || node.getParentId() == 0L);
    }

}
