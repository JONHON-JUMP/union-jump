package cn.jonhon.jump.module.system.service.user;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.tenant.core.context.TenantContextHolder;
import cn.jonhon.jump.framework.tenant.core.util.TenantUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemCardLoginRespVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.SubSystemUserPermissionRespVO;
import cn.jonhon.jump.module.system.controller.admin.oauth2.vo.subsystem.PortalPermContextRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.*;
import cn.jonhon.jump.module.system.dal.mysql.oauth2.OAuth2ClientMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.*;
import cn.jonhon.jump.module.system.service.oauth2.OAuth2ClientService;
import cn.jonhon.jump.module.system.util.MenuStyleHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertList;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;
@Service
@Validated
@Slf4j
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
    private OAuth2ClientService oauth2ClientService;
    @Resource
    private SubSystemHomePageMapper subSystemHomePageMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private OAuth2ClientMapper oauth2ClientMapper;
    @Resource
    private cn.jonhon.jump.module.system.service.permission.MenuColorService menuColorService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;
    @Resource
    private cn.jonhon.jump.module.system.dal.redis.user.PortalMyMenusRedisDAO portalMyMenusRedisDAO;
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
        List<SubSystemDO> subSystems = subSystemMapper.selectListOrderByOauth2ClientId();
        if (CollUtil.isEmpty(subSystems)) {
            return Collections.emptyList();
        }
        return subSystems.stream().map(subSystem -> {
            SubSystemClientSimpleRespVO vo = new SubSystemClientSimpleRespVO();
            vo.setId(subSystem.getId());
            OAuth2ClientDO oauth2Client = getOAuth2Client(subSystem);
            vo.setClientId(oauth2Client != null ? oauth2Client.getClientId() : null);
            vo.setName(subSystem.getSystemName());
            if (StrUtil.isNotBlank(subSystem.getSystemIcon())) {
                vo.setLogo(subSystem.getSystemIcon());
            } else if (oauth2Client != null) {
                vo.setLogo(oauth2Client.getLogo());
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
        PageResult<SubSystemUsersDO> pageResult = subSystemUsersMapper.selectPage(pageReqVO, teamCodes);
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
        validateUsernameUnique(createReqVO.getSubSystemId(), createReqVO.getUsername(), null);
        if (createReqVO.getMainUserId() != null) {
            validateMainUserExists(createReqVO.getMainUserId());
            validateSubSystemUserNotExists(createReqVO.getSubSystemId(), createReqVO.getMainUserId());
        }
        validateHomeMenu(createReqVO.getSubSystemId(), createReqVO.getHomeMenuId(), createReqVO.getRoleIds());
        SubSystemUsersDO user = BeanUtils.toBean(createReqVO, SubSystemUsersDO.class);
        if (StrUtil.isEmpty(user.getStatus())) {
            user.setStatus("0");
        }
        subSystemUsersMapper.insert(user);
        assignUserRoles(user.getId(), createReqVO.getSubSystemId(), createReqVO.getRoleIds());
        assignUserPosts(user.getId(), createReqVO.getSubSystemId(), createReqVO.getPostIds());
        subSystemPermissionContextService.evictBySubSystemUserId(user.getId());
        return user.getId();
    }

    @Override
    public SubSystemUsersRespVO getBySubSystemIdAndUsername(Long subSystemId, String username) {
        validateSubSystemExists(subSystemId);
        if (StrUtil.isBlank(username)) {
            return null;
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndUsername(subSystemId, username.trim());
        if (user == null) {
            return null;
        }
        return buildRespList(Collections.singletonList(user)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bindMainUser(Long subSystemId, Long mainUserId) {
        validateSubSystemExists(subSystemId);
        AdminUserDO mainUser = validateMainUserExists(mainUserId);
        SubSystemUsersDO existByMain = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, mainUserId);
        if (existByMain != null) {
            return existByMain.getId();
        }
        SubSystemUsersDO roster = subSystemUsersMapper.selectBySubSystemIdAndUsername(subSystemId, mainUser.getUsername());
        if (roster == null) {
            throw exception(SUB_SYSTEM_USER_USERNAME_NOT_FOUND);
        }
        if (roster.getMainUserId() != null && !Objects.equals(roster.getMainUserId(), mainUserId)) {
            throw exception(SUB_SYSTEM_USER_MAIN_BOUND);
        }
        SubSystemUsersDO updateObj = new SubSystemUsersDO();
        updateObj.setId(roster.getId());
        updateObj.setMainUserId(mainUserId);
        subSystemUsersMapper.updateById(updateObj);
        subSystemPermissionContextService.evictBySubSystemUserId(roster.getId());
        return roster.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubSystemUser(SubSystemUsersSaveReqVO updateReqVO) {
        SubSystemUsersDO existUser = validateSubSystemUserExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateUsernameUnique(updateReqVO.getSubSystemId(), updateReqVO.getUsername(), updateReqVO.getId());
        if (updateReqVO.getMainUserId() != null
                && (!Objects.equals(existUser.getSubSystemId(), updateReqVO.getSubSystemId())
                || !Objects.equals(existUser.getMainUserId(), updateReqVO.getMainUserId()))) {
            validateMainUserExists(updateReqVO.getMainUserId());
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
        subSystemPermissionContextService.evictBySubSystemUserId(updateReqVO.getId());
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemUser(Long id) {
        SubSystemUsersDO user = validateSubSystemUserExists(id);
        subSystemPermissionContextService.evictBySubSystemUserId(id);
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
        subSystemPermissionContextService.evictBySubSystemUserId(id);
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
        subSystemPermissionContextService.evictBySubSystemUserId(reqVO.getId());
    }
    @Override
    public List<Long> getSubSystemUserRoleIds(Long id) {
        validateSubSystemUserExists(id);
        return convertList(subSystemUserRoleMapper.selectListByUserId(id), SubSystemUserRoleDO::getRoleId);
    }

    @Override
    public SubSystemCardLoginRespVO cardLogin(String clientId, String clientSecret, String username) {
        if (StrUtil.isBlank(clientSecret)) {
            throw exception0(BAD_REQUEST.getCode(), "client_secret 未正确传递");
        }
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(
                clientId, clientSecret, null, null, null);
        SubSystemDO subSystem = subSystemMapper.selectByOauth2ClientId(client.getId());
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        if (StrUtil.isBlank(username)) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndUsername(
                subSystem.getId(), username.trim());
        if (user == null) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        if ("1".equals(user.getStatus())) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_DISABLED);
        }

        List<Long> roleIds = convertList(
                subSystemUserRoleMapper.selectListByUserId(user.getId()), SubSystemUserRoleDO::getRoleId);
        List<String> roleCodes = Collections.emptyList();
        if (CollUtil.isNotEmpty(roleIds)) {
            roleCodes = subSystemRoleMapper.selectBatchIds(roleIds).stream()
                    .filter(role -> role.getStatus() == null || Objects.equals(role.getStatus(), 0))
                    .map(SubSystemRoleDO::getCode)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());
        }

        SubSystemCardLoginRespVO resp = new SubSystemCardLoginRespVO();
        resp.setSubSystemId(subSystem.getId());
        resp.setClientId(client.getClientId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setStatus(user.getStatus());
        resp.setWorkshopId(user.getWorkshopId());
        resp.setTeamId(user.getTeamId());
        resp.setMainUserId(user.getMainUserId());
        resp.setRoleCodes(roleCodes);
        return resp;
    }

    @Override
    public SubSystemUserPermissionRespVO getPermissionInfo(String clientId, String clientSecret, String username) {
        if (StrUtil.isBlank(clientSecret)) {
            throw exception0(BAD_REQUEST.getCode(), "client_secret 未正确传递");
        }
        OAuth2ClientDO client = oauth2ClientService.validOAuthClientFromCache(
                clientId, clientSecret, null, null, null);
        SubSystemDO subSystem = subSystemMapper.selectByOauth2ClientId(client.getId());
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
        if (StrUtil.isBlank(username)) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndUsername(
                subSystem.getId(), username.trim());
        if (user == null) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        if ("1".equals(user.getStatus())) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_DISABLED);
        }

        List<Long> roleIds = convertList(
                subSystemUserRoleMapper.selectListByUserId(user.getId()), SubSystemUserRoleDO::getRoleId);
        List<SubSystemRoleDO> roleList = Collections.emptyList();
        if (CollUtil.isNotEmpty(roleIds)) {
            roleList = subSystemRoleMapper.selectBatchIds(roleIds).stream()
                    .filter(role -> role.getStatus() == null || Objects.equals(role.getStatus(), 0))
                    .collect(Collectors.toList());
        }

        // 主系统下发角色 + 页面菜单 + 按钮 permissions；数据范围不下发（子系统本地管）
        List<PortalPermContextRespVO.Role> roles = new ArrayList<>();
        Set<String> permissions = new LinkedHashSet<>();
        boolean allPerms = false;
        for (SubSystemRoleDO role : roleList) {
            PortalPermContextRespVO.Role r = new PortalPermContextRespVO.Role();
            r.setId(role.getId());
            r.setCode(role.getCode());
            r.setName(role.getName());
            roles.add(r);
            if ("super_admin".equals(role.getCode()) || "admin".equals(role.getCode())) {
                allPerms = true;
            }
        }
        List<SubSystemMenuDO> allMenusRaw = subSystemMenuMapper.selectListBySubSystemId(subSystem.getId()).stream()
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 0)
                .collect(Collectors.toList());
        Set<Long> roleMenuIds;
        if (allPerms) {
            permissions.add("*:*:*");
            roleMenuIds = convertSet(allMenusRaw, SubSystemMenuDO::getId);
        } else {
            roleMenuIds = CollUtil.isEmpty(roleList) ? Collections.emptySet()
                    : convertSet(subSystemRoleMenuMapper.selectListByRoleIds(
                            convertList(roleList, SubSystemRoleDO::getId)), SubSystemRoleMenuDO::getMenuId);
            // 勾了页面则自动带上按钮 perms（即使历史 role_menu 未存 F）
            roleMenuIds = expandWithButtonChildren(allMenusRaw, roleMenuIds);
            if (CollUtil.isNotEmpty(roleMenuIds)) {
                Map<Long, SubSystemMenuDO> authMenuMap = convertMap(allMenusRaw, SubSystemMenuDO::getId);
                for (Long menuId : roleMenuIds) {
                    SubSystemMenuDO menu = authMenuMap.get(menuId);
                    if (menu == null || StrUtil.isBlank(menu.getPerms())) {
                        continue;
                    }
                    for (String perm : menu.getPerms().split(",")) {
                        if (StrUtil.isNotBlank(perm)) {
                            permissions.add(perm.trim());
                        }
                    }
                }
            }
        }

        // 路由只要目录/页面
        List<SubSystemMenuDO> pageMenus = allMenusRaw.stream()
                .filter(menu -> !"F".equals(menu.getType()))
                .collect(Collectors.toList());
        Map<Long, SubSystemMenuDO> menuMap = convertMap(pageMenus, SubSystemMenuDO::getId);
        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            if (menuMap.containsKey(menuId)) {
                addMenuAndAncestors(menuId, menuMap, displayMenuIds);
            }
        }
        List<SubSystemMenuDO> displayMenus = pageMenus.stream()
                .filter(menu -> displayMenuIds.contains(menu.getId()))
                .collect(Collectors.toList());

        SubSystemUserPermissionRespVO resp = new SubSystemUserPermissionRespVO();
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setStatus(user.getStatus());
        resp.setWorkshopId(user.getWorkshopId());
        resp.setTeamId(user.getTeamId());
        resp.setRoles(roles);
        resp.setPermissions(new ArrayList<>(permissions));
        resp.setMainUserId(user.getMainUserId());
        resp.setMenus(buildPermissionMenuTree(displayMenus, 0L));
        // 登录灌权时写入主系统 Redis 权限包；改菜单/角色会 evict，子系统读 miss 可提示重登
        if (user.getMainUserId() != null) {
            warmPortalPermContext(user.getMainUserId(), subSystem.getId());
        }
        // 会话侧用版本号比对：my-menus 会 warm 回权限包，仅靠 Redis exists 会漏掉重登提示
        resp.setRbacVersion(subSystemPermissionContextService.getRbacVersion(subSystem.getId()));
        return resp;
    }

    private List<SubSystemUserPermissionRespVO.MenuNode> buildPermissionMenuTree(
            List<SubSystemMenuDO> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .sorted(Comparator.comparing(SubSystemMenuDO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemMenuDO::getId))
                .map(menu -> {
                    SubSystemUserPermissionRespVO.MenuNode node = new SubSystemUserPermissionRespVO.MenuNode();
                    node.setId(menu.getId());
                    node.setParentId(menu.getParentId());
                    node.setName(menu.getMenuName());
                    node.setType(menu.getType());
                    node.setPath(menu.getPath());
                    // Camstar/外链内链：不要把门户 Empty 组件灌给 4200，否则无法生成 InnerLink → 404
                    String path = menu.getPath();
                    if (StrUtil.isNotBlank(path)
                            && (StrUtil.startWithIgnoreCase(path, "http://")
                            || StrUtil.startWithIgnoreCase(path, "https://"))) {
                        node.setComponent(null);
                    } else {
                        node.setComponent(menu.getComponent());
                    }
                    node.setPerms(menu.getPerms());
                    node.setIcon(menu.getIcon());
                    node.setOrderNum(menu.getOrderNum());
                    node.setVisible(menu.getVisible());
                    node.setStatus(menu.getStatus());
                    node.setChildren(buildPermissionMenuTree(menus, menu.getId()));
                    return node;
                })
                .collect(Collectors.toList());
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
                OAuth2ClientDO oauth2Client = getOAuth2Client(subSystem);
                vo.setClientId(oauth2Client != null ? oauth2Client.getClientId() : null);
                vo.setClientName(subSystem.getSystemName());
            }
            // 身份字段以子系统用户表为准；仅在本地字段为空时用主用户兜底用户名/姓名
            AdminUserDO mainUser = item.getMainUserId() == null ? null : mainUserMap.get(item.getMainUserId());
            if (mainUser != null) {
                if (StrUtil.isBlank(vo.getUsername())) {
                    vo.setUsername(mainUser.getUsername());
                }
                if (StrUtil.isBlank(vo.getNickname())) {
                    vo.setNickname(mainUser.getNickname());
                }
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
        if (mainUserId == null) {
            return;
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, mainUserId);
        if (user != null) {
            throw exception(SUB_SYSTEM_USER_EXISTS);
        }
    }

    private void validateUsernameUnique(Long subSystemId, String username, Long id) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        SubSystemUsersDO exist = subSystemUsersMapper.selectBySubSystemIdAndUsername(subSystemId, username.trim());
        if (exist == null) {
            return;
        }
        if (id == null || !Objects.equals(exist.getId(), id)) {
            throw exception(SUB_SYSTEM_USER_USERNAME_EXISTS);
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
    /**
     * 勾选目录/菜单时，自动带上其下按钮（F），用于 permissions 下发与 role_menu 落库。
     */
    private Set<Long> expandWithButtonChildren(List<SubSystemMenuDO> menus, Set<Long> selectedIds) {
        if (CollUtil.isEmpty(selectedIds) || CollUtil.isEmpty(menus)) {
            return new LinkedHashSet<>(CollUtil.emptyIfNull(selectedIds));
        }
        Map<Long, List<SubSystemMenuDO>> childrenMap = menus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SubSystemMenuDO::getParentId));
        LinkedHashSet<Long> result = new LinkedHashSet<>(selectedIds);
        ArrayDeque<Long> queue = new ArrayDeque<>(selectedIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<SubSystemMenuDO> children = childrenMap.get(parentId);
            if (CollUtil.isEmpty(children)) {
                continue;
            }
            for (SubSystemMenuDO child : children) {
                if (!"F".equals(child.getType())) {
                    continue;
                }
                if (result.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
        return result;
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
        OAuth2ClientDO client = getOAuth2Client(subSystem);
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
        List<SubSystemPortalMenuRespVO> cached = portalMyMenusRedisDAO.get(userId, subSystemId);
        if (cached != null) {
            warmPortalPermContext(userId, subSystemId);
            return cached;
        }
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
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 0)
                .collect(Collectors.toList());
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);
        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            addMenuAndAncestors(menuId, menuMap, displayMenuIds);
        }
        // 与权限包一致：角色勾选（含仅勾按钮时的祖先页面）均可展示；仅目录/页面进门户树
        List<SubSystemMenuDO> displayMenus = allMenus.stream()
                .filter(menu -> displayMenuIds.contains(menu.getId()))
                .filter(menu -> "M".equals(menu.getType()) || "C".equals(menu.getType()))
                .collect(Collectors.toList());
        Set<Long> styleIds = displayMenus.stream()
                .filter(menu -> MenuStyleHelper.isFirstLevelMenu(menu.getParentId()))
                .map(SubSystemMenuDO::getStyleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO> colorMap =
                menuColorService.getMenuColorMap(styleIds);
        warmPortalPermContext(userId, subSystemId);
        List<SubSystemPortalMenuRespVO> tree = buildPortalMenuTree(displayMenus, 0L, subSystem, menuMap, colorMap, null);
        portalMyMenusRedisDAO.set(userId, subSystemId, tree);
        return tree;
    }
    /**
     * 进入子系统时预热权限包：仅 miss 时从 DB 重建。
     * 改权靠各处 evict 失效，不在每次 my-menus 强制 rebuild。
     * 必须带租户上下文，否则多租户表查询失败会导致 Redis 永远没有 portal:perm:context。
     */
    private void warmPortalPermContext(Long mainUserId, Long subSystemId) {
        try {
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null) {
                tenantId = 1L;
            }
            Long finalTenantId = tenantId;
            TenantUtils.execute(finalTenantId, () ->
                    subSystemPermissionContextService.getOrRebuild(finalTenantId, mainUserId, subSystemId));
        } catch (Exception ex) {
            // 菜单返回不以权限包写入失败阻断，但必须打日志便于排查子系统鉴权 HTTP 放大
            log.warn("[warmPortalPermContext] failed mainUserId={}, subSystemId={}, cause={}",
                    mainUserId, subSystemId, ex.toString());
        }
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
        // 与 my-menus 一致：角色勾了按钮/子项时，祖先页面也应可加入快捷导航（可见即可加星，无额外权限）
        List<SubSystemMenuDO> allMenus = subSystemMenuMapper.selectListBySubSystemId(subSystemId).stream()
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 0)
                .collect(Collectors.toList());
        Map<Long, SubSystemMenuDO> menuMap = convertMap(allMenus, SubSystemMenuDO::getId);
        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            addMenuAndAncestors(menuId, menuMap, displayMenuIds);
        }
        return allMenus.stream()
                .filter(menu -> "C".equals(menu.getType()) && displayMenuIds.contains(menu.getId()))
                .filter(menu -> menu.getVisible() == null || menu.getVisible() == 0)
                .map(SubSystemMenuDO::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> retainAllowedQuickNavMenuIds(Long userId, Long subSystemId, Collection<Long> candidateIds) {
        if (CollUtil.isEmpty(candidateIds)) {
            return Collections.emptySet();
        }
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
        LinkedHashSet<Long> candidates = new LinkedHashSet<>();
        for (Long id : candidateIds) {
            if (id != null) {
                candidates.add(id);
            }
        }
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> seedIds = new LinkedHashSet<>(candidates);
        seedIds.addAll(roleMenuIds);
        Map<Long, SubSystemMenuDO> menuMap = loadSubSystemMenusWithAncestors(seedIds);
        Set<Long> displayMenuIds = new HashSet<>();
        for (Long menuId : roleMenuIds) {
            addMenuAndAncestors(menuId, menuMap, displayMenuIds);
        }
        return candidates.stream()
                .map(menuMap::get)
                .filter(Objects::nonNull)
                .filter(menu -> "C".equals(menu.getType()) && displayMenuIds.contains(menu.getId()))
                .filter(menu -> menu.getStatus() == null || menu.getStatus() == 0)
                .filter(menu -> menu.getVisible() == null || menu.getVisible() == 0)
                .map(SubSystemMenuDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<Long, SubSystemMenuDO> loadSubSystemMenusWithAncestors(Collection<Long> ids) {
        Map<Long, SubSystemMenuDO> menuMap = new HashMap<>();
        Set<Long> pending = new LinkedHashSet<>(ids);
        while (CollUtil.isNotEmpty(pending)) {
            List<SubSystemMenuDO> batch = subSystemMenuMapper.selectListByIds(pending);
            pending.clear();
            if (CollUtil.isEmpty(batch)) {
                break;
            }
            for (SubSystemMenuDO menu : batch) {
                if (menu == null || menu.getId() == null || menuMap.containsKey(menu.getId())) {
                    continue;
                }
                menuMap.put(menu.getId(), menu);
                Long parentId = menu.getParentId();
                if (parentId != null && parentId != 0L && !menuMap.containsKey(parentId)) {
                    pending.add(parentId);
                }
            }
        }
        return menuMap;
    }

    private List<SubSystemPortalMenuRespVO> buildPortalMenuTree(List<SubSystemMenuDO> menus, Long parentId,
                                                                SubSystemDO subSystem,
                                                                Map<Long, SubSystemMenuDO> menuMap,
                                                                Map<Long, cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO> colorMap,
                                                                cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO firstLevelStyle) {
        return menus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .sorted(Comparator.comparing(SubSystemMenuDO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SubSystemMenuDO::getId))
                .map(menu -> {
                    cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO currentFirstLevel = firstLevelStyle;
                    if (cn.jonhon.jump.module.system.util.MenuStyleHelper.isFirstLevelMenu(menu.getParentId())) {
                        currentFirstLevel = cn.jonhon.jump.module.system.util.MenuStyleHelper
                                .resolveFirstLevelStyle(menu.getStyleId(), colorMap);
                    }
                    cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO effective =
                            cn.jonhon.jump.module.system.util.MenuStyleHelper.resolveEffectiveStyle(
                                    menu.getParentId(), menu.getStyleId(), currentFirstLevel, colorMap);
                    return convertPortalMenu(menu, subSystem, menuMap,
                            buildPortalMenuTree(menus, menu.getId(), subSystem, menuMap, colorMap, currentFirstLevel),
                            effective);
                })
                .collect(Collectors.toList());
    }
    private SubSystemPortalMenuRespVO convertPortalMenu(SubSystemMenuDO menu, SubSystemDO subSystem,
                                                        Map<Long, SubSystemMenuDO> menuMap,
                                                        List<SubSystemPortalMenuRespVO> children,
                                                        cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO effectiveStyle) {
        SubSystemPortalMenuRespVO vo = new SubSystemPortalMenuRespVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        cn.jonhon.jump.module.system.util.MenuStyleHelper.applyStyle(effectiveStyle, new cn.jonhon.jump.module.system.util.MenuStyleHelper.StyleTarget() {
            @Override
            public void setStyleId(Long styleId) {
                vo.setStyleId(styleId);
            }
            @Override
            public void setColor(String color) {
                vo.setColor(color);
            }
            @Override
            public void setShape(String shape) {
                vo.setShape(shape);
            }
        });
        vo.setComponentName(menu.getComponentName());
        vo.setVisible(menu.getVisible() == null || menu.getVisible() == 0);
        vo.setKeepAlive(menu.getIsCache() != null && menu.getIsCache() == 0);
        vo.setAlwaysShow(menu.getAlwaysShow() != null && menu.getAlwaysShow() == 1);
        vo.setManualUrl(menu.getManualUrl());
        vo.setChildren(children);
        if ("C".equals(menu.getType())) {
            String path = menu.getPath();
            boolean httpRoute = StrUtil.isNotBlank(path)
                    && (StrUtil.startWithIgnoreCase(path, "http://")
                    || StrUtil.startWithIgnoreCase(path, "https://"));
            // 两类菜单：Camstar/外链认「路由地址」；若依认「组件路径」
            if (httpRoute) {
                vo.setComponent(null);
            } else {
                vo.setComponent(menu.getComponent());
            }
            // 门户壳没有子系统 Vue 页，一律 iframe；真正打开地址在 link
            vo.setLink(buildIframeLink(subSystem, menu, menuMap));
        }
        return vo;
    }
    private String buildIframeLink(SubSystemDO subSystem, SubSystemMenuDO menu, Map<Long, SubSystemMenuDO> menuMap) {
        String baseUrl = StrUtil.removeSuffix(subSystem.getSystemUrl(), "/");
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }
        String leafPath = menu.getPath();
        String component = menu.getComponent();
        boolean ruoyiComponent = StrUtil.isNotBlank(component)
                && !"InnerLink".equalsIgnoreCase(component)
                && !StrUtil.containsIgnoreCase(component, "empty")
                && !StrUtil.containsIgnoreCase(component, "portal/");

        // 若依：有组件路径 → 一律 systemUrl/#/路由（禁止再走 http 直链分支）
        if (ruoyiComponent) {
            String routePath = buildMenuRoutePath(menu.getId(), menuMap);
            if (StrUtil.isBlank(routePath)) {
                return baseUrl;
            }
            return baseUrl + "/#/" + routePath.replace(":", "/");
        }

        // Camstar/外链：路由地址 http → 直开
        if (StrUtil.isNotBlank(leafPath)
                && (leafPath.startsWith("http://") || leafPath.startsWith("https://"))) {
            return leafPath;
        }
        String routePath = buildMenuRoutePath(menu.getId(), menuMap);
        if (StrUtil.isBlank(routePath)) {
            return baseUrl;
        }
        // 无组件 + IP:port 编码 → Camstar/外链直链；还原失败则仍按若依 hash
        if (routePath.contains(":") || routePath.matches(".*\\d+[./]\\d+[./]\\d+[./]\\d+.*")
                || routePath.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+9\\d{2,5}.*")) {
            String asHttp = slashIpPortPathToHttp(routePath);
            if (asHttp == null) {
                String normalized = routePath.replace(":", "/");
                normalized = normalized.replaceAll("(?<=^|/)(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(?=/)", "$1/$2/$3/$4");
                asHttp = slashIpPortPathToHttp(normalized);
            }
            if (asHttp != null && isCamstarPortalUrl(asHttp)) {
                return asHttp;
            }
            if (asHttp != null) {
                // 其它业务机端口：同样直开
                return asHttp;
            }
            return baseUrl + "/#/" + routePath.replace(":", "/");
        }
        return baseUrl + "/#/" + routePath.replace(":", "/");
    }

    /**
     * 192.168.240.12794200/Process/... → http://192.168.240.127:4200/Process/...
     * 或 15/192/168/240/126/43061/mes/... → http://192.168.240.126:43061/mes/...
     */
    private static String slashIpPortPathToHttp(String path) {
        if (path == null) {
            return null;
        }
        String raw = path.startsWith("/") ? path.substring(1) : path;
        java.util.regex.Matcher dotted9 = java.util.regex.Pattern
                .compile("^(?:.*/)?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})9(\\d{2,5})(?:/(.*))?$")
                .matcher(raw);
        if (dotted9.matches()) {
            int port = Integer.parseInt(dotted9.group(2));
            if (port > 255) {
                String after = dotted9.group(3);
                if (StrUtil.isBlank(after)) {
                    return "http://" + dotted9.group(1) + ":" + port + "/";
                }
                return "http://" + dotted9.group(1) + ":" + port + "/" + after;
            }
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(?:.*/)?(\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)/(.*)$")
                .matcher(raw);
        if (!m.matches()) {
            return null;
        }
        return "http://" + m.group(1) + "." + m.group(2) + "." + m.group(3) + "." + m.group(4)
                + ":" + m.group(5) + "/" + m.group(6);
    }

    /** 与若依 SysMenuServiceImpl.innerLinkReplaceEach 对齐（含端口冒号→/） */
    private static String innerLinkReplaceEach(String path) {
        if (path == null) {
            return "";
        }
        return path.replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .replace(".", "/")
                .replace(":", "/");
    }

    private static boolean isCamstarPortalUrl(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        String p = path.toLowerCase();
        return p.contains(":4200/") || p.contains(":4200?") || p.endsWith(":4200")
                || p.contains("94200/") || p.endsWith("94200")
                || p.contains("/4200/") || p.endsWith("/4200")
                || p.contains("camstarportal") || p.contains("/camstar/");
    }

    private String buildParentRoutePrefix(Long menuId, Map<Long, SubSystemMenuDO> menuMap) {
        List<String> segments = new ArrayList<>();
        SubSystemMenuDO leaf = menuMap.get(menuId);
        if (leaf == null) {
            return "";
        }
        Long current = leaf.getParentId();
        while (current != null && current != 0L) {
            SubSystemMenuDO item = menuMap.get(current);
            if (item == null) {
                break;
            }
            if (("M".equals(item.getType()) || "C".equals(item.getType())) && StrUtil.isNotBlank(item.getPath())) {
                String p = StrUtil.removePrefix(item.getPath(), "/");
                if (!p.startsWith("http://") && !p.startsWith("https://")
                        && !p.contains(":") && !p.matches(".*\\d+/\\d+/\\d+/\\d+.*")
                        && !"null".equalsIgnoreCase(p) && !"Empty".equalsIgnoreCase(p)) {
                    segments.add(0, p);
                }
            }
            current = item.getParentId();
        }
        return String.join("/", segments);
    }
    private String buildMenuRoutePath(Long menuId, Map<Long, SubSystemMenuDO> menuMap) {
        SubSystemMenuDO leaf = menuMap.get(menuId);
        if (leaf != null && StrUtil.isNotBlank(leaf.getPath())) {
            String leafPath = StrUtil.removePrefix(leaf.getPath(), "/");
            // 若依内链整段（含端口或点改斜杠 IP）已是完整业务 path，勿再拼上级菜单 path
            if (leafPath.contains(":") || leafPath.matches(".*\\d+/\\d+/\\d+/\\d+.*")
                    || leafPath.startsWith("http://") || leafPath.startsWith("https://")) {
                return leafPath;
            }
        }
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
    private OAuth2ClientDO getOAuth2Client(SubSystemDO subSystem) {
        if (subSystem == null || subSystem.getOauth2ClientId() == null) {
            return null;
        }
        return oauth2ClientMapper.selectById(subSystem.getOauth2ClientId());
    }
    private String encodeQueryParam(String value) {
        if (StrUtil.isEmpty(value)) {
            return "";
        }
        return URLUtil.encode(value);
    }
}
