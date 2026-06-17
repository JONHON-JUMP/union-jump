package cn.jonhon.jump.module.system.service.user;



import cn.hutool.core.collection.CollUtil;

import cn.hutool.core.util.StrUtil;

import cn.hutool.core.util.URLUtil;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;

import cn.jonhon.jump.framework.common.pojo.PageResult;

import cn.jonhon.jump.framework.common.util.object.BeanUtils;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;

import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;

import cn.jonhon.jump.module.system.dal.dataobject.user.*;

import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;

import cn.jonhon.jump.module.system.dal.mysql.user.*;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.validation.annotation.Validated;



import javax.annotation.Resource;

import java.util.*;

import java.util.stream.Collectors;



import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;

import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;

import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;



@Service

@Validated

public class SubSystemUsersServiceImpl implements SubSystemUsersService {



    @Resource

    private SubSystemUsersMapper subSystemUsersMapper;

    @Resource

    private SubSystemMapper subSystemMapper;

    @Resource

    private SubSystemRoleMapper subSystemRoleMapper;

    @Resource

    private SubSystemMenuMapper subSystemMenuMapper;

    @Resource

    private SubSystemPostMapper subSystemPostMapper;

    @Resource

    private SubSystemTeamMapper subSystemTeamMapper;

    @Resource

    private SubSystemUserRoleMapper subSystemUserRoleMapper;

    @Resource

    private SubSystemUserPostMapper subSystemUserPostMapper;

    @Resource

    private SubSystemRoleMenuMapper subSystemRoleMenuMapper;

    @Resource

    private SubSystemHomePageMapper subSystemHomePageMapper;

    @Resource

    private AdminUserMapper adminUserMapper;

    @Resource

    private OAuth2ClientMapper oauth2ClientMapper;



    @Override

    public List<SubSystemUsersRespVO> getListByMainUserId(Long mainUserId) {

        List<SubSystemUsersDO> list = subSystemUsersMapper.selectListByMainUserId(mainUserId);

        if (CollUtil.isEmpty(list)) {

            return Collections.emptyList();

        }

        return buildRespList(list);

    }



    @Override

    public Map<Long, Long> getCountMapByMainUserIds(Collection<Long> mainUserIds) {

        if (CollUtil.isEmpty(mainUserIds)) {

            return Collections.emptyMap();

        }

        List<SubSystemUsersDO> list = subSystemUsersMapper.selectListByMainUserIds(mainUserIds);

        return list.stream().collect(Collectors.groupingBy(SubSystemUsersDO::getMainUserId, Collectors.counting()));

    }



    @Override

    public List<UserExternalSystemRespVO> getMyExternalSystemList(Long userId) {

        List<SubSystemUsersDO> list = subSystemUsersMapper.selectListByMainUserId(userId);

        if (CollUtil.isEmpty(list)) {

            return Collections.emptyList();

        }

        return list.stream()

                .filter(item -> !"1".equals(item.getStatus()))

                .map(this::convertExternal)

                .filter(Objects::nonNull)

                .collect(Collectors.toList());

    }



    @Override

    public List<SubSystemClientSimpleRespVO> getClientSimpleList() {

        List<SubSystemDO> subSystems = subSystemMapper.selectListOrderByClientId();

        if (CollUtil.isEmpty(subSystems)) {

            return Collections.emptyList();

        }

        return subSystems.stream().map(subSystem -> {

            SubSystemClientSimpleRespVO vo = new SubSystemClientSimpleRespVO();

            vo.setId(subSystem.getId());

            vo.setClientId(subSystem.getClientId());

            vo.setName(subSystem.getSystemName());

            if (StrUtil.isNotBlank(subSystem.getSystemIcon())) {
                vo.setLogo(subSystem.getSystemIcon());
            } else {
                OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(subSystem.getClientId());
                if (client != null) {
                    vo.setLogo(client.getLogo());
                }
            }

            vo.setUserCount(subSystemUsersMapper.selectCountBySubSystemId(subSystem.getId()));

            vo.setRoleCount(subSystemRoleMapper.selectCountBySubSystemId(subSystem.getId()));

            vo.setMenuCount(subSystemMenuMapper.selectCountBySubSystemId(subSystem.getId()));

            vo.setPostCount(subSystemPostMapper.selectCountBySubSystemId(subSystem.getId()));

            vo.setTeamCount(subSystemTeamMapper.selectCountBySubSystemId(subSystem.getId()));

            return vo;

        }).collect(Collectors.toList());

    }



    @Override

    public PageResult<SubSystemUsersRespVO> getSubSystemUserPage(SubSystemUsersPageReqVO pageReqVO) {

        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        }

        Collection<String> teamCodes = null;
        if (StrUtil.isNotBlank(pageReqVO.getTeamName())) {
            List<SubSystemTeamDO> teams = pageReqVO.getSubSystemId() != null
                    ? subSystemTeamMapper.selectListBySubSystemIdAndTeamNameLike(
                            pageReqVO.getSubSystemId(), pageReqVO.getTeamName())
                    : subSystemTeamMapper.selectListByTeamNameLike(pageReqVO.getTeamName());
            teamCodes = convertList(teams, SubSystemTeamDO::getTeamCode);
            if (CollUtil.isEmpty(teamCodes)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
        }

        Collection<Long> mainUserIds = null;
        if (StrUtil.isNotBlank(pageReqVO.getNickname())
                || StrUtil.isNotBlank(pageReqVO.getEmployeeNo())
                || StrUtil.isNotBlank(pageReqVO.getDomainNo())) {
            mainUserIds = convertList(adminUserMapper.selectListByMainUserSearch(
                    pageReqVO.getNickname(), pageReqVO.getEmployeeNo(), pageReqVO.getDomainNo()), AdminUserDO::getId);
            if (CollUtil.isEmpty(mainUserIds)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
        }

        PageResult<SubSystemUsersDO> pageResult = subSystemUsersMapper.selectPage(pageReqVO, teamCodes, mainUserIds);

        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());

    }



    @Override

    public SubSystemUsersRespVO getSubSystemUser(Long id) {

        SubSystemUsersDO user = validateSubSystemUserExists(id);

        SubSystemUsersRespVO vo = buildRespList(Collections.singletonList(user)).get(0);

        vo.setRoleIds(getSubSystemUserRoleIds(id));

        vo.setPostIds(getSubSystemUserPostIds(id));

        return vo;

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public Long createSubSystemUser(SubSystemUsersSaveReqVO createReqVO) {

        validateSubSystemExists(createReqVO.getSubSystemId());

        validateMainUserExists(createReqVO.getMainUserId());

        validateSubSystemUserNotExists(createReqVO.getSubSystemId(), createReqVO.getMainUserId());

        validateHomeMenu(createReqVO.getSubSystemId(), createReqVO.getHomeMenuId(), createReqVO.getRoleIds());

        SubSystemUsersDO user = BeanUtils.toBean(createReqVO, SubSystemUsersDO.class);

        if (StrUtil.isEmpty(user.getStatus())) {

            user.setStatus("0");

        }

        subSystemUsersMapper.insert(user);

        assignUserRoles(user.getId(), createReqVO.getSubSystemId(), createReqVO.getRoleIds());

        assignUserPosts(user.getId(), createReqVO.getSubSystemId(), createReqVO.getPostIds());

        return user.getId();

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void updateSubSystemUser(SubSystemUsersSaveReqVO updateReqVO) {

        SubSystemUsersDO existUser = validateSubSystemUserExists(updateReqVO.getId());

        validateSubSystemExists(updateReqVO.getSubSystemId());

        if (!Objects.equals(existUser.getSubSystemId(), updateReqVO.getSubSystemId())

                || !Objects.equals(existUser.getMainUserId(), updateReqVO.getMainUserId())) {

            validateSubSystemUserNotExists(updateReqVO.getSubSystemId(), updateReqVO.getMainUserId());

        }

        validateHomeMenu(updateReqVO.getSubSystemId(), updateReqVO.getHomeMenuId(), updateReqVO.getRoleIds());

        SubSystemUsersDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemUsersDO.class);

        subSystemUsersMapper.updateById(updateObj);

        if (updateReqVO.getRoleIds() != null) {

            assignUserRoles(updateReqVO.getId(), updateReqVO.getSubSystemId(), updateReqVO.getRoleIds());

        }

        if (updateReqVO.getPostIds() != null) {

            assignUserPosts(updateReqVO.getId(), updateReqVO.getSubSystemId(), updateReqVO.getPostIds());

        }

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void deleteSubSystemUser(Long id) {

        validateSubSystemUserExists(id);

        subSystemUsersMapper.deleteById(id);

        subSystemUserRoleMapper.deleteListByUserId(id);

        subSystemUserPostMapper.deleteListByUserId(id);

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void deleteSubSystemUserList(List<Long> ids) {

        if (CollUtil.isEmpty(ids)) {

            return;

        }

        ids.forEach(this::deleteSubSystemUser);

    }



    @Override

    public void updateSubSystemUserStatus(Long id, String status) {

        validateSubSystemUserExists(id);

        SubSystemUsersDO updateObj = new SubSystemUsersDO();

        updateObj.setId(id);

        updateObj.setStatus(status);

        subSystemUsersMapper.updateById(updateObj);

    }



    @Override

    public List<SubSystemRoleSimpleRespVO> getRoleSimpleList(Long subSystemId) {

        validateSubSystemExists(subSystemId);

        return BeanUtils.toBean(subSystemRoleMapper.selectListBySubSystemId(subSystemId), SubSystemRoleSimpleRespVO.class);

    }



    @Override

    public List<SubSystemPostSimpleRespVO> getPostSimpleList(Long subSystemId) {

        validateSubSystemExists(subSystemId);

        return BeanUtils.toBean(subSystemPostMapper.selectListBySubSystemId(subSystemId), SubSystemPostSimpleRespVO.class);

    }



    @Override

    public List<SubSystemTeamSimpleRespVO> getTeamSimpleList(Long subSystemId) {

        validateSubSystemExists(subSystemId);

        return BeanUtils.toBean(subSystemTeamMapper.selectListBySubSystemId(subSystemId), SubSystemTeamSimpleRespVO.class);

    }

    @Override
    public List<SubSystemMenuTreeRespVO> getUserHomeMenuTree(Long subSystemId, List<Long> roleIds) {
        validateSubSystemExists(subSystemId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        validateRolesBelongToSubSystem(subSystemId, roleIds);

        Set<Long> roleMenuIds = convertSet(subSystemRoleMenuMapper.selectListByRoleIds(roleIds),
                SubSystemRoleMenuDO::getMenuId);
        if (CollUtil.isEmpty(roleMenuIds)) {
            return Collections.emptyList();
        }

        List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId);
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);

        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            addMenuAndAncestors(menuId, menuMap, displayMenuIds);
        }

        List<SubSystemMenuDO> displayMenus = allMenus.stream()
                .filter(menu -> displayMenuIds.contains(menu.getId()))
                .filter(menu -> "M".equals(menu.getType())
                        || ("C".equals(menu.getType()) && roleMenuIds.contains(menu.getId())))
                .collect(Collectors.toList());

        return buildMenuTree(displayMenus, 0L);
    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void assignSubSystemUserRole(SubSystemUsersAssignRoleReqVO reqVO) {

        SubSystemUsersDO user = validateSubSystemUserExists(reqVO.getId());

        assignUserRoles(reqVO.getId(), user.getSubSystemId(), reqVO.getRoleIds());

    }



    @Override

    public List<Long> getSubSystemUserRoleIds(Long id) {

        validateSubSystemUserExists(id);

        return convertList(subSystemUserRoleMapper.selectListByUserId(id), SubSystemUserRoleDO::getRoleId);

    }



    private List<Long> getSubSystemUserPostIds(Long id) {

        return convertList(subSystemUserPostMapper.selectListByUserId(id), SubSystemUserPostDO::getPostId);

    }



    private List<SubSystemUsersRespVO> buildRespList(List<SubSystemUsersDO> list) {

        if (CollUtil.isEmpty(list)) {

            return Collections.emptyList();

        }

        Set<Long> subSystemIds = list.stream().map(SubSystemUsersDO::getSubSystemId).collect(Collectors.toSet());

        Set<Long> mainUserIds = list.stream().map(SubSystemUsersDO::getMainUserId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, SubSystemDO> subSystemMap = convertMap(

                subSystemMapper.selectListByIds(subSystemIds), SubSystemDO::getId);

        Map<Long, AdminUserDO> mainUserMap = CollUtil.isEmpty(mainUserIds) ? Collections.emptyMap()

                : convertMap(adminUserMapper.selectList(AdminUserDO::getId, mainUserIds), AdminUserDO::getId);



        Map<Long, List<SubSystemUserRoleDO>> userRoleMap = subSystemUserRoleMapper.selectListByUserIds(

                convertList(list, SubSystemUsersDO::getId)).stream()

                .collect(Collectors.groupingBy(SubSystemUserRoleDO::getUserId));

        Map<Long, List<SubSystemUserPostDO>> userPostMap = subSystemUserPostMapper.selectListByUserIds(

                convertList(list, SubSystemUsersDO::getId)).stream()

                .collect(Collectors.groupingBy(SubSystemUserPostDO::getUserId));



        Set<Long> roleIds = userRoleMap.values().stream()

                .flatMap(items -> items.stream().map(SubSystemUserRoleDO::getRoleId))

                .collect(Collectors.toSet());

        Set<Long> postIds = userPostMap.values().stream()

                .flatMap(items -> items.stream().map(SubSystemUserPostDO::getPostId))

                .collect(Collectors.toSet());

        Map<Long, SubSystemRoleDO> roleMap = CollUtil.isEmpty(roleIds) ? Collections.emptyMap()

                : convertMap(subSystemRoleMapper.selectListByIds(roleIds), SubSystemRoleDO::getId);

        Map<Long, SubSystemPostDO> postMap = CollUtil.isEmpty(postIds) ? Collections.emptyMap()

                : convertMap(subSystemPostMapper.selectListByIds(postIds), SubSystemPostDO::getId);

        Set<Long> homeMenuIds = list.stream().map(SubSystemUsersDO::getHomeMenuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Map<Long, SubSystemMenuDO>> subSystemMenuMapCache = new HashMap<>();
        if (CollUtil.isNotEmpty(homeMenuIds)) {
            subSystemIds.forEach(subSystemId -> subSystemMenuMapCache.put(subSystemId,
                    convertMap(subSystemMenuMapper.selectListBySubSystemId(subSystemId), SubSystemMenuDO::getId)));
        }



        return list.stream().map(item -> {

            SubSystemUsersRespVO vo = BeanUtils.toBean(item, SubSystemUsersRespVO.class);

            SubSystemDO subSystem = subSystemMap.get(item.getSubSystemId());

            if (subSystem != null) {

                vo.setClientId(subSystem.getClientId());

                vo.setClientName(subSystem.getSystemName());

            }

            AdminUserDO mainUser = mainUserMap.get(item.getMainUserId());

            if (mainUser != null) {

                vo.setUsername(mainUser.getUsername());

                vo.setNickname(mainUser.getNickname());

                vo.setEmployeeNo(mainUser.getEmployeeNo());

                vo.setCardNo(mainUser.getCardNo());

                vo.setErpNos(mainUser.getErpNos());

                vo.setDomainNo(mainUser.getDomainNo());

            }

            if (StrUtil.isNotEmpty(item.getTeamId())) {

                SubSystemTeamDO team = subSystemTeamMapper.selectBySubSystemIdAndTeamCode(item.getSubSystemId(), item.getTeamId());

                if (team != null) {

                    vo.setTeamName(team.getTeamName());

                }

            }

            List<SubSystemUserRoleDO> userRoles = userRoleMap.getOrDefault(item.getId(), Collections.emptyList());

            vo.setRoleIds(convertList(userRoles, SubSystemUserRoleDO::getRoleId));

            vo.setRoleNames(userRoles.stream()

                    .map(userRole -> roleMap.get(userRole.getRoleId()))

                    .filter(Objects::nonNull)

                    .map(SubSystemRoleDO::getName)

                    .collect(Collectors.joining("、")));

            List<SubSystemUserPostDO> userPosts = userPostMap.getOrDefault(item.getId(), Collections.emptyList());

            vo.setPostIds(convertList(userPosts, SubSystemUserPostDO::getPostId));

            vo.setPostNames(userPosts.stream()

                    .map(userPost -> postMap.get(userPost.getPostId()))

                    .filter(Objects::nonNull)

                    .map(SubSystemPostDO::getName)

                    .collect(Collectors.joining("、")));

            if (item.getHomeMenuId() != null) {
                Map<Long, SubSystemMenuDO> menuMap = subSystemMenuMapCache.get(item.getSubSystemId());
                if (menuMap != null) {
                    vo.setHomeMenuName(buildMenuPath(item.getHomeMenuId(), menuMap));
                }
            }

            return vo;

        }).collect(Collectors.toList());

    }



    private void assignUserRoles(Long userId, Long subSystemId, List<Long> roleIds) {

        subSystemUserRoleMapper.deleteListByUserId(userId);

        if (CollUtil.isEmpty(roleIds)) {

            return;

        }

        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListByIds(roleIds);

        for (SubSystemRoleDO role : roles) {

            if (!Objects.equals(subSystemId, role.getSubSystemId())) {

                throw exception(ROLE_NOT_EXISTS);

            }

            SubSystemUserRoleDO userRole = new SubSystemUserRoleDO();

            userRole.setUserId(userId);

            userRole.setRoleId(role.getId());

            subSystemUserRoleMapper.insert(userRole);

        }

    }



    private void assignUserPosts(Long userId, Long subSystemId, List<Long> postIds) {

        subSystemUserPostMapper.deleteListByUserId(userId);

        if (CollUtil.isEmpty(postIds)) {

            return;

        }

        List<SubSystemPostDO> posts = subSystemPostMapper.selectListByIds(postIds);

        for (SubSystemPostDO post : posts) {

            if (!Objects.equals(subSystemId, post.getSubSystemId())) {

                throw exception(POST_NOT_FOUND);

            }

            SubSystemUserPostDO userPost = new SubSystemUserPostDO();

            userPost.setUserId(userId);

            userPost.setPostId(post.getId());

            subSystemUserPostMapper.insert(userPost);

        }

    }



    private SubSystemUsersDO validateSubSystemUserExists(Long id) {

        SubSystemUsersDO user = subSystemUsersMapper.selectById(id);

        if (user == null) {

            throw exception(SUB_SYSTEM_USER_NOT_EXISTS);

        }

        return user;

    }



    private void validateSubSystemUserNotExists(Long subSystemId, Long mainUserId) {

        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, mainUserId);

        if (user != null) {

            throw exception(SUB_SYSTEM_USER_EXISTS);

        }

    }

    private void validateHomeMenu(Long subSystemId, Long homeMenuId, List<Long> roleIds) {
        if (homeMenuId == null) {
            return;
        }
        SubSystemMenuDO menu = subSystemMenuMapper.selectById(homeMenuId);
        if (menu == null || !Objects.equals(menu.getSubSystemId(), subSystemId) || !"C".equals(menu.getType())) {
            throw exception(SUB_SYSTEM_USER_HOME_MENU_INVALID);
        }
        if (CollUtil.isEmpty(roleIds)) {
            throw exception(SUB_SYSTEM_USER_HOME_MENU_INVALID);
        }
        Set<Long> roleMenuIds = getRoleMenuIds(subSystemId, roleIds);
        if (!roleMenuIds.contains(homeMenuId)) {
            throw exception(SUB_SYSTEM_USER_HOME_MENU_INVALID);
        }
    }

    private Set<Long> getRoleMenuIds(Long subSystemId, List<Long> roleIds) {
        List<SubSystemRoleDO> roles = validateRolesBelongToSubSystem(subSystemId, roleIds);
        if (hasSubSystemSuperAdminRole(roles)) {
            return convertSet(subSystemMenuMapper.selectListBySubSystemId(subSystemId), SubSystemMenuDO::getId);
        }
        return convertSet(subSystemRoleMenuMapper.selectListByRoleIds(roleIds), SubSystemRoleMenuDO::getMenuId);
    }

    /**
     * Ruoyi/SCADA 超级管理员（roleKey=admin）拥有全部菜单，sys_role_menu 中通常无记录。
     */
    private boolean hasSubSystemSuperAdminRole(List<SubSystemRoleDO> roles) {
        return roles.stream().anyMatch(role -> "admin".equals(role.getCode()));
    }

    private List<SubSystemRoleDO> validateRolesBelongToSubSystem(Long subSystemId, List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw exception(ROLE_NOT_EXISTS);
        }
        for (SubSystemRoleDO role : roles) {
            if (!Objects.equals(subSystemId, role.getSubSystemId())) {
                throw exception(ROLE_NOT_EXISTS);
            }
        }
        return roles;
    }

    private void addMenuAndAncestors(Long menuId, Map<Long, SubSystemMenuDO> menuMap, Set<Long> displayMenuIds) {
        Long current = menuId;
        while (current != null && current != 0L) {
            if (!displayMenuIds.add(current)) {
                break;
            }
            SubSystemMenuDO menu = menuMap.get(current);
            if (menu == null) {
                break;
            }
            current = menu.getParentId();
        }
    }

    private List<SubSystemMenuTreeRespVO> buildMenuTree(List<SubSystemMenuDO> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .sorted(Comparator.comparing(SubSystemMenuDO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemMenuDO::getId))
                .map(menu -> {
                    SubSystemMenuTreeRespVO node = new SubSystemMenuTreeRespVO();
                    node.setId(menu.getId());
                    node.setName(menu.getMenuName());
                    node.setParentId(menu.getParentId());
                    node.setType(menu.getType());
                    node.setOrderNum(menu.getOrderNum());
                    node.setChildren(buildMenuTree(menus, menu.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    private String buildMenuPath(Long menuId, Map<Long, SubSystemMenuDO> menuMap) {
        List<String> names = new ArrayList<>();
        Long current = menuId;
        while (current != null && current != 0L) {
            SubSystemMenuDO menu = menuMap.get(current);
            if (menu == null) {
                break;
            }
            names.add(0, menu.getMenuName());
            current = menu.getParentId();
        }
        return String.join(" / ", names);
    }



    private SubSystemDO validateSubSystemExists(Long subSystemId) {

        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);

        if (subSystem == null) {

            throw exception(SUB_SYSTEM_NOT_EXISTS);

        }

        return subSystem;

    }



    private AdminUserDO validateMainUserExists(Long mainUserId) {

        AdminUserDO mainUser = adminUserMapper.selectById(mainUserId);

        if (mainUser == null) {

            throw exception(USER_NOT_EXISTS);

        }

        return mainUser;

    }



    private UserExternalSystemRespVO convertExternal(SubSystemUsersDO item) {

        SubSystemDO subSystem = subSystemMapper.selectById(item.getSubSystemId());

        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {

            return null;

        }

        OAuth2ClientDO client = oauth2ClientMapper.selectByClientId(subSystem.getClientId());

        if (client == null || CommonStatusEnum.isDisable(client.getStatus())) {

            return null;

        }

        UserExternalSystemRespVO vo = new UserExternalSystemRespVO();

        vo.setId(item.getId());

        vo.setSubSystemId(subSystem.getId());

        vo.setSystemUrl(subSystem.getSystemUrl());

        vo.setClientId(client.getClientId());

        vo.setClientName(subSystem.getSystemName());

        if (StrUtil.isNotBlank(subSystem.getSystemIcon())) {
            vo.setLogo(subSystem.getSystemIcon());
        } else {
            vo.setLogo(client.getLogo());
        }

        vo.setWorkshopId(item.getWorkshopId());

        vo.setTeamId(item.getTeamId());

        vo.setHomeMenuId(item.getHomeMenuId());

        SubSystemHomePageDO homePage = subSystemHomePageMapper.selectBySubSystemId(subSystem.getId());

        if (homePage != null) {
            vo.setHomePageName(homePage.getHomePageName());
            vo.setHomePageUrl(homePage.getHomePageUrl());
        }

        vo.setSsoUrl(buildSsoUrl(client));

        return vo;

    }



    @Override
    public List<SubSystemPortalMenuRespVO> getMyPortalMenus(Long userId, Long subSystemId) {
        SubSystemUsersDO subSystemUser = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, userId);
        if (subSystemUser == null || "1".equals(subSystemUser.getStatus())) {
            throw exception(SUB_SYSTEM_USER_NOT_EXISTS);
        }
        SubSystemDO subSystem = validateSubSystemExists(subSystemId);
        if (CommonStatusEnum.isDisable(subSystem.getStatus())) {
            return Collections.emptyList();
        }
        List<Long> roleIds = getSubSystemUserRoleIds(subSystemUser.getId());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> roleMenuIds = getRoleMenuIds(subSystemId, roleIds);
        if (CollUtil.isEmpty(roleMenuIds)) {
            return Collections.emptyList();
        }

        List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                .filter(menu -> menu.getStatus() != null && menu.getStatus() == 0)
                .collect(Collectors.toList());
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);

        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            addMenuAndAncestors(menuId, menuMap, displayMenuIds);
        }

        List<SubSystemMenuDO> displayMenus = allMenus.stream()
                .filter(menu -> displayMenuIds.contains(menu.getId()))
                .filter(menu -> "M".equals(menu.getType())
                        || ("C".equals(menu.getType()) && roleMenuIds.contains(menu.getId())))
                .collect(Collectors.toList());

        return buildPortalMenuTree(displayMenus, 0L, subSystem, menuMap);
    }

    @Override
    public Set<Long> getAllowedQuickNavMenuIds(Long userId, Long subSystemId) {
        SubSystemUsersDO subSystemUser = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, userId);
        if (subSystemUser == null || "1".equals(subSystemUser.getStatus())) {
            return Collections.emptySet();
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            return Collections.emptySet();
        }
        List<Long> roleIds = getSubSystemUserRoleIds(subSystemUser.getId());
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }
        Set<Long> roleMenuIds = getRoleMenuIds(subSystemId, roleIds);
        if (CollUtil.isEmpty(roleMenuIds)) {
            return Collections.emptySet();
        }
        return subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                .filter(menu -> menu.getStatus() != null && menu.getStatus() == 0)
                .filter(menu -> "C".equals(menu.getType()) && roleMenuIds.contains(menu.getId()))
                .filter(menu -> menu.getVisible() == null || menu.getVisible() == 0)
                .map(SubSystemMenuDO::getId)
                .collect(Collectors.toSet());
    }

    private List<SubSystemPortalMenuRespVO> buildPortalMenuTree(List<SubSystemMenuDO> menus, Long parentId,
                                                                SubSystemDO subSystem,
                                                                Map<Long, SubSystemMenuDO> menuMap) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .sorted(Comparator.comparing(SubSystemMenuDO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemMenuDO::getId))
                .map(menu -> convertPortalMenu(menu, subSystem, menuMap, buildPortalMenuTree(menus, menu.getId(), subSystem, menuMap)))
                .collect(Collectors.toList());
    }

    private SubSystemPortalMenuRespVO convertPortalMenu(SubSystemMenuDO menu, SubSystemDO subSystem,
                                                        Map<Long, SubSystemMenuDO> menuMap,
                                                        List<SubSystemPortalMenuRespVO> children) {
        SubSystemPortalMenuRespVO vo = new SubSystemPortalMenuRespVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setComponentName(menu.getComponentName());
        vo.setVisible(menu.getVisible() == null || menu.getVisible() == 0);
        vo.setKeepAlive(menu.getIsCache() != null && menu.getIsCache() == 0);
        vo.setAlwaysShow(menu.getAlwaysShow() != null && menu.getAlwaysShow() == 1);
        vo.setChildren(children);
        if ("C".equals(menu.getType())) {
            vo.setComponent("system/subSystem/portal/Empty");
            vo.setLink(buildIframeLink(subSystem, menu, menuMap));
        }
        return vo;
    }

    private String buildIframeLink(SubSystemDO subSystem, SubSystemMenuDO menu, Map<Long, SubSystemMenuDO> menuMap) {
        if (StrUtil.isNotBlank(menu.getPath())
                && (menu.getPath().startsWith("http://") || menu.getPath().startsWith("https://"))) {
            return menu.getPath();
        }
        String baseUrl = StrUtil.removeSuffix(subSystem.getSystemUrl(), "/");
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }
        String routePath = buildMenuRoutePath(menu.getId(), menuMap);
        if (StrUtil.isBlank(routePath)) {
            return baseUrl;
        }
        return baseUrl + "/#/" + routePath;
    }

    private String buildMenuRoutePath(Long menuId, Map<Long, SubSystemMenuDO> menuMap) {
        List<String> segments = new ArrayList<>();
        Long current = menuId;
        while (current != null && current != 0L) {
            SubSystemMenuDO item = menuMap.get(current);
            if (item == null) {
                break;
            }
            if (("M".equals(item.getType()) || "C".equals(item.getType())) && StrUtil.isNotBlank(item.getPath())) {
                segments.add(0, StrUtil.removePrefix(item.getPath(), "/"));
            }
            current = item.getParentId();
        }
        return String.join("/", segments);
    }

    private String buildSsoUrl(OAuth2ClientDO client) {

        if (CollUtil.isEmpty(client.getRedirectUris())) {

            return null;

        }

        String redirectUri = client.getRedirectUris().get(0);

        List<String> requestScopes = CollUtil.isNotEmpty(client.getAutoApproveScopes())
                ? client.getAutoApproveScopes()
                : (CollUtil.isNotEmpty(client.getScopes()) ? client.getScopes() : Collections.singletonList("user.read"));
        String scope = String.join(" ", requestScopes);

        return "/sso?client_id=" + encodeQueryParam(client.getClientId())

                + "&redirect_uri=" + encodeQueryParam(redirectUri)

                + "&response_type=code"

                + "&scope=" + encodeQueryParam(scope);

    }



    private String encodeQueryParam(String value) {

        if (StrUtil.isEmpty(value)) {

            return "";

        }

        return URLUtil.encode(value);

    }



}

