package cn.jonhon.jump.module.system.service.user;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.framework.tenant.core.context.TenantContextHolder;
import cn.jonhon.jump.framework.tenant.core.util.TenantUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.*;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.UserSubSystemRosterReqVO;
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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
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
    @Resource
    private SubSystemAccessService subSystemAccessService;
    @Resource
    private SubSystemEmployeeService subSystemEmployeeService;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;
    @Resource
    private SubSystemWorkshopMapper subSystemWorkshopMapper;
    @Resource
    private LeafApiBindingService leafApiBindingService;

    @Override
    public List<SubSystemUsersRespVO> getListByMainUserId(Long mainUserId) {
        List<SubSystemUsersDO> list = subSystemUsersMapper.selectListByMainUserId(mainUserId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        list = filterByAllowedSubSystems(list, SubSystemUsersDO::getSubSystemId);
        return buildRespList(list);
    }
    @Override
    public Map<Long, Long> getCountMapByMainUserIds(Collection<Long> mainUserIds) {
        if (CollUtil.isEmpty(mainUserIds)) {
            return Collections.emptyMap();
        }
        List<SubSystemUsersDO> list = filterByAllowedSubSystems(
                subSystemUsersMapper.selectListByMainUserIds(mainUserIds), SubSystemUsersDO::getSubSystemId);
        return list.stream().collect(Collectors.groupingBy(SubSystemUsersDO::getMainUserId, Collectors.counting()));
    }
    @Override
    public List<UserExternalSystemRespVO> getMyExternalSystemList(Long userId) {
        List<SubSystemUsersDO> list = subSystemUsersMapper.selectListByMainUserId(userId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        AdminUserDO mainUser = adminUserMapper.selectById(userId);
        Map<Long, SubSystemDO> subSystemMap = loadSubSystemMap(list);
        Map<String, String> workshopNameDict = loadWorkshopNameDict();
        Map<Long, Set<String>> workshopLeavesBySubSystem = loadWorkshopLeavesBySubSystemId();
        List<UserExternalSystemRespVO> result = new ArrayList<>();
        for (SubSystemUsersDO item : list) {
            if ("1".equals(item.getStatus())) {
                continue;
            }
            List<Long> roleIds = listRoleIds(item.getId());
            if (CollUtil.isEmpty(roleIds)) {
                continue;
            }
            UserExternalSystemRespVO base = convertExternal(item, mainUser);
            if (base == null) {
                continue;
            }
            SubSystemDO subSystem = subSystemMap.get(item.getSubSystemId());
            UserExternalSystemRespVO entry = toPortalEntryByHungLeaf(
                    base, item, subSystem, workshopNameDict, workshopLeavesBySubSystem, roleIds);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }
    @Override
    public List<SubSystemClientSimpleRespVO> getClientSimpleList(Boolean portalOnly) {
        List<SubSystemDO> subSystems = subSystemMapper.selectListOrderByOauth2ClientId();
        if (CollUtil.isEmpty(subSystems)) {
            return Collections.emptyList();
        }
        // 可管系统范围过滤：左侧业务系统列表只显示授权系统
        subSystems = filterByAllowedSubSystems(subSystems, SubSystemDO::getId);
        boolean onlyPortal = Boolean.TRUE.equals(portalOnly);
        return subSystems.stream()
                .filter(subSystem -> !onlyPortal || subSystem.portalBound())
                .map(subSystem -> {
                    SubSystemClientSimpleRespVO vo = new SubSystemClientSimpleRespVO();
                    vo.setId(subSystem.getId());
                    OAuth2ClientDO oauth2Client = getOAuth2Client(subSystem);
                    vo.setClientId(subSystem.resolvePortalClientId(
                            oauth2Client != null ? oauth2Client.getClientId() : null));
                    vo.setName(subSystem.getSystemName());
                    vo.setPortalBound(subSystem.portalBound());
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
                    vo.setWorkshopScopeEnabled(subSystem.workshopScopeOn());
                    return vo;
                }).collect(Collectors.toList());
    }
    @Override
    public PageResult<SubSystemUsersRespVO> getSubSystemUserPage(SubSystemUsersPageReqVO pageReqVO) {
        // 可管系统范围：受限时强制只查授权系统（null=不受限）
        Set<Long> allowedSubSystemIds = subSystemAccessService.getAllowedSubSystemIds(
                SecurityFrameworkUtils.getLoginUserId());
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        } else if (allowedSubSystemIds != null && allowedSubSystemIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
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
        PageResult<SubSystemUsersDO> pageResult = subSystemUsersMapper.selectPage(pageReqVO, teamCodes, allowedSubSystemIds);
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
        createReqVO.setUsername(StrUtil.trim(createReqVO.getUsername()));
        createReqVO.setWorkshopId(StrUtil.trim(createReqVO.getWorkshopId()));
        validateHangLeaf(createReqVO.getSubSystemId(), createReqVO.getWorkshopId());
        validateUsernameUnique(createReqVO.getSubSystemId(), createReqVO.getUsername(), null,
                createReqVO.getWorkshopId());
        if (createReqVO.getMainUserId() != null) {
            validateMainUserExists(createReqVO.getMainUserId());
            validateSubSystemUserNotExists(createReqVO.getSubSystemId(), createReqVO.getMainUserId(),
                    createReqVO.getWorkshopId());
        }
        // 角色分配唯一入口是「分配角色」（assignSubSystemUserRole），新增/修改不再写角色
        SubSystemUsersDO user = BeanUtils.toBean(createReqVO, SubSystemUsersDO.class);
        if (StrUtil.isEmpty(user.getStatus())) {
            user.setStatus("0");
        }
        if (StrUtil.isEmpty(user.getEmployeeRegistered())) {
            user.setEmployeeRegistered("0");
        }
        // 关联车间编号：1=对接用车间_工号。新用户默认按 JUMP 规则「车间编号_工号」：
        // 挂在车间叶子下（多车间系统）且未显式传值时默认 1，无车间系统默认 0。
        // 带横线的多账号（如 123-1）仅存量数据兼容，新建不再产生
        String reqWithWorkshop = StrUtil.trimToNull(createReqVO.getUsernameWithWorkshop());
        user.setUsernameWithWorkshop(reqWithWorkshop != null
                ? ("1".equals(reqWithWorkshop) ? "1" : "0")
                : (StrUtil.isNotBlank(createReqVO.getWorkshopId()) ? "1" : "0"));
        try {
            subSystemUsersMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 同叶子重名，或库仍是旧「系统+用户名」唯一（需执行 sql/postgresql/upgrade_20261016_sub_system_review_fix.sql）
            String detail = ex.getMostSpecificCause() != null
                    ? StrUtil.nullToEmpty(ex.getMostSpecificCause().getMessage()) : "";
            log.warn("[createSubSystemUser] duplicate key subSystemId={}, username={}, workshopId={}, cause={}",
                    createReqVO.getSubSystemId(), createReqVO.getUsername(), createReqVO.getWorkshopId(), detail);
            if (detail.contains("main_user") || detail.contains("leaf_main_user")) {
                throw exception(SUB_SYSTEM_USER_WORKSHOP_EXISTS);
            }
            throw exception(SUB_SYSTEM_USER_USERNAME_EXISTS);
        } catch (DataIntegrityViolationException ex) {
            log.warn("[createSubSystemUser] integrity fail subSystemId={}, username={}, workshopId={}, cause={}",
                    createReqVO.getSubSystemId(), createReqVO.getUsername(), createReqVO.getWorkshopId(),
                    ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.toString());
            throw exception(SUB_SYSTEM_USER_USERNAME_EXISTS);
        } catch (BadSqlGrammarException ex) {
            // username_with_workshop / registered_api_type 等新列未升级时缺列，提示执行升级脚本
            throw exception(SUB_SYSTEM_SCHEMA_OUTDATED);
        }
        assignUserPosts(user.getId(), createReqVO.getSubSystemId(), createReqVO.getPostIds());
        tryAutoBindMainUser(user);
        try {
            subSystemPermissionContextService.evictBySubSystemUserId(user.getId());
        } catch (Exception ex) {
            log.warn("[createSubSystemUser] evict cache skip id={}, cause={}", user.getId(), ex.toString());
        }
        return user.getId();
    }

    @Override
    public SubSystemUsersRespVO getBySubSystemIdAndUsername(Long subSystemId, String username, String workshopId) {
        validateSubSystemExists(subSystemId);
        if (StrUtil.isBlank(username)) {
            return null;
        }
        String workshop = StrUtil.trim(workshopId);
        SubSystemUsersDO user;
        if (StrUtil.isNotBlank(workshop)) {
            user = subSystemUsersMapper.selectBySubSystemIdAndUsernameAndWorkshopId(
                    subSystemId, username.trim(), workshop);
            if (user == null) {
                user = selectByUsernameCompat(subSystemId, username.trim());
                if (user != null && !workshop.equals(StrUtil.trim(user.getWorkshopId()))) {
                    user = null;
                }
            }
        } else {
            user = selectByUsernameCompat(subSystemId, username.trim());
        }
        if (user == null) {
            return null;
        }
        return buildRespList(Collections.singletonList(user)).get(0);
    }

    @Override
    public String getMyCamstarUsername(Long userId) {
        AdminUserDO mainUser = adminUserMapper.selectById(userId);
        if (mainUser == null) {
            return null;
        }
        // 全局预取只回主登录工号。同 MES 多叶子（5500/5501）身份不同，
        // 若这里拼「最新一条」车间_工号，前端迟到预取会把已切到 5501 的 Cookie 盖回 5500。
        // 真正打开/切换系统以 my-list 的 externalUsername + refreshCamstarCookieForSystem 为准。
        return mainUser.getUsername();
    }

    /**
     * 按用户名查子系统用户的兼容查询：子系统（Camstar）侧用户名为 车间编号_工号 时，
     * 先按传入全名精确匹配；查不到且含 "_" 时按后缀工号回查，
     * 命中行须已标记拼接车间（username_with_workshop=1）且车间编号与前缀一致才认。
     */
    private SubSystemUsersDO selectByUsernameCompat(Long subSystemId, String username) {
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndUsername(subSystemId, username);
        if (user != null) {
            return user;
        }
        int split = username.lastIndexOf('_');
        if (split <= 0 || split >= username.length() - 1) {
            return null;
        }
        String workshopCode = username.substring(0, split);
        String bareUsername = username.substring(split + 1);
        // 按叶子精确查，避免同工号多车间时命中错行
        user = subSystemUsersMapper.selectBySubSystemIdAndUsernameAndWorkshopId(
                subSystemId, bareUsername, workshopCode);
        if (user == null || !"1".equals(user.getUsernameWithWorkshop())) {
            return null;
        }
        return user;
    }

    @Override
    public void bindMatchingRosters(Long mainUserId, String username) {
        if (mainUserId == null || StrUtil.isBlank(username)) {
            return;
        }
        List<SubSystemUsersDO> rows = subSystemUsersMapper.selectListByUsername(username.trim());
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        Set<Long> blankWorkshopSystems = new HashSet<>();
        List<SubSystemUsersDO> alreadyHung = subSystemUsersMapper.selectListByMainUserId(mainUserId);
        if (CollUtil.isNotEmpty(alreadyHung)) {
            for (SubSystemUsersDO hung : alreadyHung) {
                if (hung.getSubSystemId() != null && StrUtil.isBlank(hung.getWorkshopId())) {
                    blankWorkshopSystems.add(hung.getSubSystemId());
                }
            }
        }
        for (SubSystemUsersDO row : rows) {
            // 空、0、指向已删除用户都算还没挂。已挂在别的仍存在的 JUMP 用户上则不动
            if (!canAutoBind(row.getMainUserId())) {
                continue;
            }
            // 该系统已有一条空车间挂接时，不再把同名叶子行也绑上，避免同一系统两条入口
            if (blankWorkshopSystems.contains(row.getSubSystemId()) && StrUtil.isNotBlank(row.getWorkshopId())) {
                continue;
            }
            // 同系统多叶子：按车间区分占用，避免 5500 已挂挡住 4200
            SubSystemUsersDO occupied = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                    row.getSubSystemId(), mainUserId, StrUtil.trim(row.getWorkshopId()));
            if (occupied != null) {
                continue;
            }
            SubSystemUsersDO updateObj = new SubSystemUsersDO();
            updateObj.setId(row.getId());
            updateObj.setMainUserId(mainUserId);
            subSystemUsersMapper.updateById(updateObj);
            subSystemPermissionContextService.evictBySubSystemUserId(row.getId());
        }
    }

    @Override
    public void bindExternalEmployee(Long id, String userCode, String workshopCode, Long apiSubSystemId) {
        SubSystemUsersDO roster = validateSubSystemUserExists(id);
        String code = StrUtil.trimToNull(userCode);
        LambdaUpdateWrapper<SubSystemUsersDO> wrapper = new LambdaUpdateWrapper<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getId, id);
        if (code == null) {
            wrapper.set(SubSystemUsersDO::getEmployeeRegistered, "0")
                    .set(SubSystemUsersDO::getUsernameWithWorkshop, "0")
                    .set(SubSystemUsersDO::getRegisteredApiType, null);
            subSystemUsersMapper.update(null, wrapper);
            subSystemPermissionContextService.evictBySubSystemUserId(id);
            return;
        }
        String bare = StrUtil.trim(roster.getUsername());
        String workshop = StrUtil.trim(workshopCode);
        boolean withWorkshop = false;
        if (StrUtil.isNotBlank(workshop) && code.equals(workshop + "_" + bare)) {
            withWorkshop = true;
        } else if (code.equals(bare)) {
            withWorkshop = false;
        } else if (bare.endsWith("_" + code) || code.endsWith("_" + bare)) {
            String prefix = code.endsWith("_" + bare)
                    ? code.substring(0, code.length() - bare.length() - 1) : "";
            if (StrUtil.isNotBlank(prefix) && (StrUtil.isBlank(workshop) || prefix.equals(workshop))) {
                workshop = prefix;
                withWorkshop = true;
            } else {
                throw externalUserMismatch(code, bare);
            }
        } else {
            throw externalUserMismatch(code, bare);
        }
        String apiType = null;
        if (apiSubSystemId != null) {
            SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(apiSubSystemId);
            apiType = config != null ? config.getApiType() : null;
        }
        wrapper.set(SubSystemUsersDO::getEmployeeRegistered, "1")
                .set(SubSystemUsersDO::getUsernameWithWorkshop, withWorkshop ? "1" : "0")
                .set(SubSystemUsersDO::getRegisteredApiType, apiType);
        if (StrUtil.isNotBlank(workshop)) {
            wrapper.set(SubSystemUsersDO::getWorkshopId, workshop);
        }
        subSystemUsersMapper.update(null, wrapper);
        subSystemPermissionContextService.evictBySubSystemUserId(id);
    }

    private static cn.jonhon.jump.framework.common.exception.ServiceException externalUserMismatch(String code, String bare) {
        return exception0(BAD_REQUEST.getCode(),
                "所选外部用户【" + code + "】与本地用户名【" + bare + "】不一致。请选择工号相同的人员，或先把本地用户名改成一致后再关联");
    }

    /**
     * 还没挂到一个仍然存在的 JUMP 用户上。
     * main_user_id 为空、0，或指向已删除用户时，新建同名用户可以挂上。
     */
    private boolean canAutoBind(Long currentMainUserId) {
        if (currentMainUserId == null || currentMainUserId <= 0) {
            return true;
        }
        return adminUserMapper.selectById(currentMainUserId) == null;
    }

    /** 花名册用户名与主系统用户名相同则挂上；已挂过或该主用户在本系统已有别的行则不动 */
    private void tryAutoBindMainUser(SubSystemUsersDO roster) {
        if (roster == null || roster.getId() == null || !canAutoBind(roster.getMainUserId())
                || StrUtil.isBlank(roster.getUsername())) {
            return;
        }
        try {
            AdminUserDO main = adminUserMapper.selectByUsername(roster.getUsername().trim());
            if (main == null) {
                return;
            }
            // 同系统多叶子：按车间区分是否已被该 JUMP 人占用，避免 5500 行挡住 4200 自动绑定
            SubSystemUsersDO occupied = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                    roster.getSubSystemId(), main.getId(), StrUtil.trim(roster.getWorkshopId()));
            if (occupied != null && !Objects.equals(occupied.getId(), roster.getId())) {
                return;
            }
            SubSystemUsersDO updateObj = new SubSystemUsersDO();
            updateObj.setId(roster.getId());
            updateObj.setMainUserId(main.getId());
            subSystemUsersMapper.updateById(updateObj);
            roster.setMainUserId(main.getId());
        } catch (Exception ex) {
            // 自动挂接失败不阻断新建花名册（例如库仍有「系统+主用户」唯一约束）
            log.warn("[tryAutoBindMainUser] skip rosterId={}, username={}, cause={}",
                    roster.getId(), roster.getUsername(), ex.toString());
        }
    }

    @Override
    public void syncDisplayNickname(Long mainUserId, String oldNickname, String newNickname) {
        if (mainUserId == null || StrUtil.isBlank(newNickname)) {
            return;
        }
        String name = newNickname.trim();
        String previous = StrUtil.nullToEmpty(oldNickname);
        String previousTrim = previous.trim();
        if (name.equals(previousTrim)) {
            return;
        }
        LambdaUpdateWrapper<SubSystemUsersDO> wrapper = new LambdaUpdateWrapper<SubSystemUsersDO>()
                .eq(SubSystemUsersDO::getMainUserId, mainUserId)
                .set(SubSystemUsersDO::getNickname, name);
        if (StrUtil.isBlank(previousTrim)) {
            wrapper.and(w -> w.isNull(SubSystemUsersDO::getNickname)
                    .or().eq(SubSystemUsersDO::getNickname, ""));
        } else if (previous.equals(previousTrim)) {
            wrapper.eq(SubSystemUsersDO::getNickname, previousTrim);
        } else {
            wrapper.and(w -> w.eq(SubSystemUsersDO::getNickname, previous)
                    .or().eq(SubSystemUsersDO::getNickname, previousTrim));
        }
        subSystemUsersMapper.update(null, wrapper);
    }

    @Override
    public void registerFromMainUser(AdminUserDO mainUser, List<Long> subSystemIds) {
        if (mainUser == null || CollUtil.isEmpty(subSystemIds)) {
            return;
        }
        // 兼容旧调用：只给系统 id，多车间系统仍跳过空车间登记
        List<UserSubSystemRosterReqVO> targets = subSystemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(subSystemId -> {
                    UserSubSystemRosterReqVO target = new UserSubSystemRosterReqVO();
                    target.setSubSystemId(subSystemId);
                    return target;
                })
                .collect(Collectors.toList());
        registerFromMainUser(mainUser, targets, false);
    }

    @Override
    public void registerFromMainUser(AdminUserDO mainUser, List<UserSubSystemRosterReqVO> targets,
                                     boolean explicit) {
        if (mainUser == null || CollUtil.isEmpty(targets)) {
            return;
        }
        // 同一 (系统, 车间) 只登记一次
        Map<String, UserSubSystemRosterReqVO> uniqueTargets = new LinkedHashMap<>();
        for (UserSubSystemRosterReqVO target : targets) {
            if (target == null || target.getSubSystemId() == null) {
                continue;
            }
            String workshop = StrUtil.trim(target.getWorkshopId());
            uniqueTargets.putIfAbsent(target.getSubSystemId() + "|" + StrUtil.nullToEmpty(workshop), target);
        }
        if (uniqueTargets.isEmpty()) {
            if (explicit) {
                throw exception0(BAD_REQUEST.getCode(), "登记业务系统的目标不能为空");
            }
            return;
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(uniqueTargets.values(),
                        UserSubSystemRosterReqVO::getSubSystemId)),
                SubSystemDO::getId);
        for (UserSubSystemRosterReqVO target : uniqueTargets.values()) {
            SubSystemDO subSystem = subSystemMap.get(target.getSubSystemId());
            if (subSystem == null) {
                if (explicit) {
                    throw exception(SUB_SYSTEM_NOT_EXISTS);
                }
                log.warn("[registerFromMainUser] subSystemId={} 不存在，跳过花名册登记", target.getSubSystemId());
                continue;
            }
            // 无登录上下文（内部调用）不受限；受限角色不能把主用户登记到未授权系统
            subSystemAccessService.checkAccessible(subSystem.getId());
            if (!subSystem.portalBound()) {
                if (explicit) {
                    throw exception0(BAD_REQUEST.getCode(),
                            "业务系统【" + subSystem.getSystemName() + "】未绑定门户，不能登记业务系统用户");
                }
                // 仅接口目标（如 Camstar人员管理）不属于外部用户管理，跳过花名册登记
                log.warn("[registerFromMainUser] subSystemId={}({}) 未绑定门户，跳过花名册登记",
                        subSystem.getId(), subSystem.getSystemName());
                continue;
            }
            String workshop = StrUtil.trim(target.getWorkshopId());
            if (StrUtil.isBlank(workshop)) {
                if (isMultiLeaf(subSystem)) {
                    if (explicit) {
                        throw exception0(BAD_REQUEST.getCode(),
                                "业务系统【" + subSystem.getSystemName()
                                        + "】按车间管理，请选择具体车间叶子后再登记");
                    }
                    // 旧调用兼容：多叶子系统的空车间挂接不会出现在门户，跳过而不是落脏数据
                    log.info("[registerFromMainUser] subSystemId={}({}) 多车间且未指定叶子，跳过登记",
                            subSystem.getId(), subSystem.getSystemName());
                    continue;
                }
                // 一对一/无车间系统：车间可空
                upsertRosterFromMainUser(mainUser, subSystem.getId(), null);
                continue;
            }
            validateRosterWorkshop(subSystem.getId(), workshop);
            upsertRosterFromMainUser(mainUser, subSystem.getId(), workshop);
        }
    }

    /** 登记目标里的车间必须是该系统已启用的叶子，避免前端传任意字符串落库 */
    private void validateRosterWorkshop(Long subSystemId, String workshopCode) {
        if (!enabledLeafCodes(subSystemId).contains(workshopCode)) {
            throw exception(SUB_SYSTEM_USER_WORKSHOP_LEAF_INVALID);
        }
    }

    private void upsertRosterFromMainUser(AdminUserDO mainUser, Long subSystemId, String workshopCode) {
        // 业务用户名可能不等于工号（如环控 123-1）：先按「主用户+车间」查，
        // 该人这个车间已有挂接行就复用（幂等），绝不按工号再建第二行——
        // 否则同车间两行绑同一人，cookie 取行按 id 倒序会拿到业务系统里不存在的账号
        SubSystemUsersDO hung = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                subSystemId, mainUser.getId(), StrUtil.trim(workshopCode));
        if (hung != null) {
            return;
        }
        String username = StrUtil.trim(mainUser.getUsername());
        SubSystemUsersDO roster = subSystemUsersMapper.selectBySubSystemIdAndUsernameAndWorkshopId(
                subSystemId, username, StrUtil.trim(workshopCode));
        if (roster != null) {
            // 已绑其他主用户则报错，回滚整个建用户事务
            if (!canAutoBind(roster.getMainUserId()) && !Objects.equals(roster.getMainUserId(), mainUser.getId())) {
                throw exception(SUB_SYSTEM_USER_MAIN_BOUND);
            }
            SubSystemUsersDO updateObj = new SubSystemUsersDO();
            updateObj.setId(roster.getId());
            updateObj.setMainUserId(mainUser.getId());
            subSystemUsersMapper.updateById(updateObj);
            subSystemPermissionContextService.evictBySubSystemUserId(roster.getId());
            return;
        }
        SubSystemUsersDO subUser = new SubSystemUsersDO();
        subUser.setSubSystemId(subSystemId);
        subUser.setMainUserId(mainUser.getId());
        subUser.setUsername(username);
        subUser.setNickname(mainUser.getNickname());
        subUser.setWorkshopId(workshopCode);
        subUser.setStatus("0");
        // 新登记默认按 JUMP 规则：挂在车间叶子下即「车间编号_工号」，无车间系统用工号；
        // 横线多账号（123-1 等）仅存量兼容，新用户不再产生
        subUser.setUsernameWithWorkshop(StrUtil.isNotBlank(workshopCode) ? "1" : "0");
        // 新登记默认未注册（是否已调「新增人员」接口，后续手动推送时自动置 1）
        subUser.setEmployeeRegistered("0");
        subUser.setRemark("用户创建登记");
        subSystemUsersMapper.insert(subUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubSystemUser(SubSystemUsersSaveReqVO updateReqVO) {
        SubSystemUsersDO existUser = validateSubSystemUserExists(updateReqVO.getId());
        // 归属系统不可改。请求里的 subSystemId 若被换成别的系统，后面的角色会在新系统被当成 admin。
        updateReqVO.setSubSystemId(existUser.getSubSystemId());
        // 已绑其他 JUMP 用户的行禁止静默改绑
        if (updateReqVO.getMainUserId() != null
                && !canAutoBind(existUser.getMainUserId())
                && !Objects.equals(existUser.getMainUserId(), updateReqVO.getMainUserId())) {
            throw exception(SUB_SYSTEM_USER_MAIN_BOUND);
        }
        validateSubSystemExists(updateReqVO.getSubSystemId());
        updateReqVO.setUsername(StrUtil.trim(updateReqVO.getUsername()));
        updateReqVO.setWorkshopId(StrUtil.trim(updateReqVO.getWorkshopId()));
        validateHangLeaf(updateReqVO.getSubSystemId(), updateReqVO.getWorkshopId());
        String existingWorkshop = StrUtil.trim(existUser.getWorkshopId());
        String targetWorkshop = StrUtil.trim(updateReqVO.getWorkshopId());
        // 跨车间角色放开后，人员持有其它车间的角色属正常分配，改车间不再受角色车间限制
        validateUsernameUnique(updateReqVO.getSubSystemId(), updateReqVO.getUsername(), updateReqVO.getId(),
                updateReqVO.getWorkshopId());
        if (updateReqVO.getMainUserId() != null
                && (!Objects.equals(existUser.getMainUserId(), updateReqVO.getMainUserId())
                || !Objects.equals(StrUtil.trim(existUser.getWorkshopId()), updateReqVO.getWorkshopId()))) {
            validateMainUserExists(updateReqVO.getMainUserId());
            validateSubSystemUserNotExists(updateReqVO.getSubSystemId(), updateReqVO.getMainUserId(),
                    updateReqVO.getWorkshopId());
        }
        // 主页面按该用户「已分配角色」校验（新增/修改弹窗已无角色行）
        validateHomeMenu(updateReqVO.getSubSystemId(), updateReqVO.getHomeMenuId(),
                getSubSystemUserRoleIds(updateReqVO.getId()));
        SubSystemUsersDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemUsersDO.class);
        // 关联车间编号（usernameWithWorkshop）由页面单独维护：1=对接用车间_工号，0=工号
        updateObj.setUsernameWithWorkshop(
                "1".equals(StrUtil.trim(updateReqVO.getUsernameWithWorkshop())) ? "1" : "0");
        // 外部账号是按“旧用户名/旧车间/旧拼接方式”建的；身份字段一变，本地再显示“已注册”
        // 会导致注册入口跳过推送、门户身份也对不上。置回未注册，提示管理员重新注册/关联。
        String oldWithWorkshop = "1".equals(StrUtil.trim(existUser.getUsernameWithWorkshop())) ? "1" : "0";
        boolean identityChanged = !Objects.equals(StrUtil.trim(existUser.getUsername()), updateReqVO.getUsername())
                || !Objects.equals(existingWorkshop, targetWorkshop)
                || !Objects.equals(oldWithWorkshop, updateObj.getUsernameWithWorkshop());
        if (identityChanged && "1".equals(existUser.getEmployeeRegistered())) {
            updateObj.setEmployeeRegistered("0");
            updateObj.setRegisteredApiType("");
            log.info("[updateSubSystemUser] rosterId={} 身份字段变更，外部注册状态置回未注册",
                    updateReqVO.getId());
        }
        try {
            subSystemUsersMapper.updateById(updateObj);
        } catch (DuplicateKeyException ex) {
            // 库仍是旧「系统+用户名」唯一（需执行 sql/postgresql/upgrade_20261016_sub_system_review_fix.sql）时，改车间会撞索引
            log.warn("[updateSubSystemUser] duplicate key id={}, username={}, workshopId={}",
                    updateReqVO.getId(), updateReqVO.getUsername(), updateReqVO.getWorkshopId());
            throw exception(SUB_SYSTEM_USER_USERNAME_EXISTS);
        } catch (BadSqlGrammarException ex) {
            throw exception(SUB_SYSTEM_SCHEMA_OUTDATED);
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
    public void updateSubSystemUserRegisterStatus(Long id, String employeeRegistered) {
        validateSubSystemUserExists(id);
        SubSystemUsersDO updateObj = new SubSystemUsersDO();
        updateObj.setId(id);
        updateObj.setEmployeeRegistered("1".equals(employeeRegistered) ? "1" : "0");
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
    public List<SubSystemTeamSimpleRespVO> getTeamSimpleList(Long subSystemId, Long deptId) {
        validateSubSystemExists(subSystemId);
        if (deptId != null) {
            return BeanUtils.toBean(
                    subSystemTeamMapper.selectListBySubSystemIdAndDeptId(subSystemId, deptId),
                    SubSystemTeamSimpleRespVO.class);
        }
        return BeanUtils.toBean(subSystemTeamMapper.selectListBySubSystemId(subSystemId), SubSystemTeamSimpleRespVO.class);
    }
    @Override
    public List<SubSystemMenuTreeRespVO> getUserHomeMenuTree(Long subSystemId, List<Long> roleIds) {
        validateSubSystemExists(subSystemId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        Set<Long> roleMenuIds = getRoleMenuIds(subSystemId, roleIds);
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
    public SubSystemUsersAssignRoleRespVO assignSubSystemUserRole(SubSystemUsersAssignRoleReqVO reqVO) {
        // 本地角色先提交并释放连接，再调外部接口。避免 Camstar 慢或不可达时占着数据库连接。
        SubSystemUsersDO user = new TransactionTemplate(transactionManager).execute(status -> {
            SubSystemUsersDO existing = validateSubSystemUserExists(reqVO.getId());
            assignUserRoles(reqVO.getId(), existing.getSubSystemId(), reqVO.getRoleIds());
            subSystemPermissionContextService.evictBySubSystemUserId(reqVO.getId());
            return existing;
        });
        SubSystemUsersAssignRoleRespVO respVO = new SubSystemUsersAssignRoleRespVO();
        respVO.setExternalSynced(false);
        if (!Boolean.TRUE.equals(reqVO.getSyncToExternal())) {
            return respVO;
        }
        // 外部同步失败不回滚本地分配（同步只调外部接口，不写本地库）；
        // 结果明示，重新分配一次即重试
        respVO.setExternalSynced(true);
        respVO.setExternalSuccess(true);
        try {
            syncExternalUserRoles(user, reqVO);
        } catch (Exception e) {
            log.warn("[assignSubSystemUserRole] rosterId={} apiSubSystemId={} 外部角色同步失败",
                    reqVO.getId(), reqVO.getApiSubSystemId(), e);
            respVO.setExternalSuccess(false);
            respVO.setExternalMessage(e.getMessage());
        }
        return respVO;
    }

    /** 勾选同步时：按 external_role_id 映射调对方接口上挂/解除（查-合并-回写，不动对方手工挂的角色） */
    private void syncExternalUserRoles(SubSystemUsersDO user, SubSystemUsersAssignRoleReqVO reqVO) {
        if (reqVO.getApiSubSystemId() == null) {
            reqVO.setApiSubSystemId(leafApiBindingService.requireApiSubSystemId(
                    user.getSubSystemId(), user.getWorkshopId(),
                    cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum.ROLE_ASSIGN));
        }
        if (!"1".equals(user.getEmployeeRegistered())) {
            throw exception0(BAD_REQUEST.getCode(), "该用户尚未关联外部人员，请先在列表中【关联外部】");
        }
        String workshop = StrUtil.trim(user.getWorkshopId());
        String bare = StrUtil.blankToDefault(user.getUsername(), "").trim();
        String userCode = bare;
        if ("1".equals(user.getUsernameWithWorkshop()) && StrUtil.isNotBlank(workshop)
                && !bare.startsWith(workshop + "_")) {
            userCode = workshop + "_" + bare;
        }
        subSystemEmployeeService.syncUserRoles(reqVO.getApiSubSystemId(), user.getSubSystemId(),
                userCode, reqVO.getRoleIds());
    }
    @Override
    public List<Long> getSubSystemUserRoleIds(Long id) {
        validateSubSystemUserExists(id);
        return listRoleIds(id);
    }

    /** 自助门户读自己的角色。不走可管系统范围，那是管理员接口的鉴权。 */
    private List<Long> listRoleIds(Long subSystemUserId) {
        if (subSystemUserId == null) {
            return Collections.emptyList();
        }
        return convertList(subSystemUserRoleMapper.selectListByUserId(subSystemUserId), SubSystemUserRoleDO::getRoleId);
    }

    private List<SubSystemRoleDO> enabledRolesInSystem(Collection<Long> roleIds, Long subSystemId) {
        if (CollUtil.isEmpty(roleIds) || subSystemId == null) {
            return Collections.emptyList();
        }
        return subSystemRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Objects.equals(subSystemId, role.getSubSystemId()))
                .filter(role -> role.getStatus() == null || Objects.equals(role.getStatus(), 0))
                .collect(Collectors.toList());
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
        SubSystemUsersDO user = selectByUsernameCompat(subSystem.getId(), username.trim());
        if (user == null) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        if ("1".equals(user.getStatus())) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_DISABLED);
        }

        List<Long> roleIds = convertList(
                subSystemUserRoleMapper.selectListByUserId(user.getId()), SubSystemUserRoleDO::getRoleId);
        List<String> roleCodes = enabledRolesInSystem(roleIds, subSystem.getId()).stream()
                .map(SubSystemRoleDO::getCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

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
        SubSystemUsersDO user = selectByUsernameCompat(subSystem.getId(), username.trim());
        if (user == null) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_NOT_EXISTS);
        }
        if ("1".equals(user.getStatus())) {
            throw exception(SUB_SYSTEM_CARD_LOGIN_USER_DISABLED);
        }

        List<Long> roleIds = convertList(
                subSystemUserRoleMapper.selectListByUserId(user.getId()), SubSystemUserRoleDO::getRoleId);
        List<SubSystemRoleDO> roleList = enabledRolesInSystem(roleIds, subSystem.getId());

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
        Set<String> teamCodes = list.stream().map(SubSystemUsersDO::getTeamId)
                .filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        Map<String, String> teamNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(teamCodes)) {
            for (SubSystemTeamDO team : subSystemTeamMapper.selectListBySubSystemIdsAndTeamCodes(subSystemIds, teamCodes)) {
                teamNameMap.putIfAbsent(team.getSubSystemId() + ":" + team.getTeamCode(), team.getTeamName());
            }
        }
        Set<Long> oauthClientIds = subSystemMap.values().stream()
                .map(SubSystemDO::getOauth2ClientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OAuth2ClientDO> oauthClientMap = CollUtil.isEmpty(oauthClientIds) ? Collections.emptyMap()
                : convertMap(oauth2ClientMapper.selectBatchIds(oauthClientIds), OAuth2ClientDO::getId);
        // 接口配置类型（camstar/http）：前端按它筛选可同步域账号/卡号的挂接行
        Map<Long, String> apiTypeMap = new HashMap<>();
        for (SubSystemApiConfigDO config : subSystemApiConfigMapper.selectListBySubSystemIds(subSystemIds)) {
            if (config.getSubSystemId() != null && StrUtil.isNotBlank(config.getApiType())) {
                apiTypeMap.putIfAbsent(config.getSubSystemId(), config.getApiType());
            }
        }
        return list.stream().map(item -> {
            SubSystemUsersRespVO vo = BeanUtils.toBean(item, SubSystemUsersRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(item.getSubSystemId());
            if (subSystem != null) {
                OAuth2ClientDO oauth2Client = subSystem.getOauth2ClientId() == null
                        ? null : oauthClientMap.get(subSystem.getOauth2ClientId());
                vo.setClientId(subSystem.resolvePortalClientId(
                        oauth2Client != null ? oauth2Client.getClientId() : null));
                vo.setClientName(subSystem.getSystemName());
                vo.setApiType(apiTypeMap.get(item.getSubSystemId()));
            }
            // 身份字段以子系统用户表为准；仅在本地字段为空时用主用户兜底用户名/姓名
            AdminUserDO mainUser = item.getMainUserId() == null ? null : mainUserMap.get(item.getMainUserId());
            // 0 或已删除用户不是「有 JUMP 用户」，列表按未关联展示，避免详情里查不到
            if (mainUser == null) {
                vo.setMainUserId(null);
            }
            if (mainUser != null) {
                if (StrUtil.isBlank(vo.getUsername())) {
                    vo.setUsername(mainUser.getUsername());
                }
                if (StrUtil.isBlank(vo.getNickname())) {
                    vo.setNickname(mainUser.getNickname());
                }
            }
            if (StrUtil.isNotEmpty(item.getTeamId())) {
                vo.setTeamName(teamNameMap.get(item.getSubSystemId() + ":" + item.getTeamId()));
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
        Set<Long> requestedRoleIds = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (roles.size() != requestedRoleIds.size()) {
            throw exception(ROLE_NOT_EXISTS);
        }
        for (SubSystemRoleDO role : roles) {
            if (!Objects.equals(subSystemId, role.getSubSystemId())) {
                throw exception(ROLE_NOT_EXISTS);
            }
            // 停用角色仍能分配、但登录后不生效，属于“分配了却没权限”的坑，直接拦
            if (CommonStatusEnum.isDisable(role.getStatus())) {
                throw exception0(BAD_REQUEST.getCode(),
                        "角色【" + role.getName() + "】已停用，请先启用或改选其它角色");
            }
            // 跨车间角色：同一套 MES 管多车间（如 5601~5606）时角色可跨车间分配，
            // 只校验同 subSystemId，不再比对角色车间与人员车间
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
        // 可管系统范围校验：受限角色只能操作授权系统的花名册
        subSystemAccessService.checkAccessible(user.getSubSystemId());
        return user;
    }
    private void validateSubSystemUserNotExists(Long subSystemId, Long mainUserId) {
        validateSubSystemUserNotExists(subSystemId, mainUserId, null);
    }

    private void validateSubSystemUserNotExists(Long subSystemId, Long mainUserId, String workshopId) {
        if (mainUserId == null) {
            return;
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                subSystemId, mainUserId, workshopId);
        if (user != null) {
            throw exception(StrUtil.isNotBlank(workshopId) ? SUB_SYSTEM_USER_WORKSHOP_EXISTS : SUB_SYSTEM_USER_EXISTS);
        }
    }

    private void validateUsernameUnique(Long subSystemId, String username, Long id) {
        validateUsernameUnique(subSystemId, username, id, null);
    }

    private void validateUsernameUnique(Long subSystemId, String username, Long id, String workshopId) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        String name = username.trim();
        String workshop = StrUtil.trim(workshopId);
        // 唯一范围 = 业务系统 + 叶子(workshop) + 用户名；不同车间允许同一工号
        SubSystemUsersDO exist = subSystemUsersMapper.selectBySubSystemIdAndUsernameAndWorkshopId(
                subSystemId, name, workshop);
        if (exist != null && (id == null || !Objects.equals(exist.getId(), id))) {
            throw exception(SUB_SYSTEM_USER_USERNAME_EXISTS);
        }
    }

    /**
     * 多车间系统才强制挂叶子；一对一/未开车间维度（Camstar、接口平台等）车间可空。
     * 有填写时仍校验必须落在子表叶子上。
     */
    private boolean isMultiLeaf(Long subSystemId) {
        if (subSystemId == null) {
            return false;
        }
        return isMultiLeaf(subSystemMapper.selectById(subSystemId));
    }

    private boolean isMultiLeaf(SubSystemDO subSystem) {
        if (subSystem == null || !subSystem.workshopScopeOn()) {
            return false;
        }
        return enabledLeafCodes(subSystem.getId()).size() > 1;
    }

    /** 与 {@link #validateHangLeaf} 同一份启用叶子，含 client_id 回落到系统的行。 */
    private Set<String> enabledLeafCodes(Long subSystemId) {
        if (subSystemId == null) {
            return Collections.emptySet();
        }
        return loadWorkshopLeavesBySubSystemId().getOrDefault(subSystemId, Collections.emptySet());
    }

    private void validateHangLeaf(Long subSystemId, String workshopId) {
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null) {
            return;
        }
        Set<String> leaves = enabledLeafCodes(subSystemId);
        String workshop = StrUtil.trim(workshopId);
        // 真正多叶子才强制；仅开了开关但子表只有 0/1 条时不拦（兼容旧数据与一对一）
        boolean multiLeafRequired = subSystem.workshopScopeOn() && leaves.size() > 1;
        if (!multiLeafRequired) {
            if (StrUtil.isNotBlank(workshop) && !leaves.isEmpty() && !leaves.contains(workshop)) {
                throw exception(SUB_SYSTEM_USER_WORKSHOP_LEAF_INVALID);
            }
            return;
        }
        if (leaves.isEmpty()) {
            throw exception(SUB_SYSTEM_USER_WORKSHOP_LEAF_MISSING);
        }
        if (StrUtil.isBlank(workshop)) {
            throw exception(SUB_SYSTEM_USER_WORKSHOP_LEAF_REQUIRED);
        }
        if (!leaves.contains(workshop)) {
            throw exception(SUB_SYSTEM_USER_WORKSHOP_LEAF_INVALID);
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
        Set<Long> seen = new HashSet<>();
        while (current != null && current != 0L && seen.add(current)) {
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
        // 可管系统范围校验：受限角色只能操作授权系统
        subSystemAccessService.checkAccessible(subSystemId);
        return subSystem;
    }
    /** 可管系统范围过滤：受限时仅保留授权系统的记录（不受限原样返回） */
    private <T> List<T> filterByAllowedSubSystems(List<T> list, Function<T, Long> subSystemIdGetter) {
        Set<Long> allowed = subSystemAccessService.getAllowedSubSystemIds(SecurityFrameworkUtils.getLoginUserId());
        if (allowed == null) {
            return list;
        }
        return list.stream()
                .filter(item -> allowed.contains(subSystemIdGetter.apply(item)))
                .collect(Collectors.toList());
    }
    private AdminUserDO validateMainUserExists(Long mainUserId) {
        AdminUserDO mainUser = adminUserMapper.selectById(mainUserId);
        if (mainUser == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return mainUser;
    }
    private UserExternalSystemRespVO convertExternal(SubSystemUsersDO item, AdminUserDO mainUser) {
        SubSystemDO subSystem = subSystemMapper.selectById(item.getSubSystemId());
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus()) || !subSystem.portalBound()) {
            return null;
        }
        OAuth2ClientDO client = getOAuth2Client(subSystem);
        if (client != null && CommonStatusEnum.isDisable(client.getStatus())) {
            return null;
        }
        String clientId = subSystem.resolvePortalClientId(client != null ? client.getClientId() : null);
        if (StrUtil.isBlank(clientId)) {
            return null;
        }
        UserExternalSystemRespVO vo = new UserExternalSystemRespVO();
        vo.setId(item.getId());
        vo.setSubSystemId(subSystem.getId());
        vo.setSystemUrl(subSystem.getSystemUrl());
        vo.setClientId(clientId);
        vo.setMesClientId(clientId);
        vo.setClientName(subSystem.getSystemName());
        if (StrUtil.isNotBlank(subSystem.getSystemIcon())) {
            vo.setLogo(subSystem.getSystemIcon());
        } else if (client != null) {
            vo.setLogo(client.getLogo());
        }
        vo.setWorkshopId(item.getWorkshopId());
        vo.setTeamId(item.getTeamId());
        // Cookie 身份只认花名册「关联车间编号」：是 → 车间_工号，否 → 工号（不再卡 registeredApiType）
        String bareUsername = StrUtil.isNotBlank(item.getUsername()) ? item.getUsername().trim()
                : (mainUser != null ? mainUser.getUsername() : null);
        vo.setUsername(bareUsername);
        vo.setUsernameWithWorkshop(StrUtil.blankToDefault(item.getUsernameWithWorkshop(), "0"));
        if (bareUsername != null && "1".equals(item.getUsernameWithWorkshop())
                && StrUtil.isNotBlank(item.getWorkshopId())) {
            String workshopCode = item.getWorkshopId().trim();
            // 用户名本身已是 车间_工号 时不再叠一层
            if (bareUsername.startsWith(workshopCode + "_")) {
                vo.setExternalUsername(bareUsername);
            } else {
                vo.setExternalUsername(workshopCode + "_" + bareUsername);
            }
        } else {
            vo.setExternalUsername(bareUsername);
        }
        vo.setHomeMenuId(item.getHomeMenuId());
        SubSystemHomePageDO homePage = subSystemHomePageMapper.selectBySubSystemId(subSystem.getId());
        if (homePage != null) {
            vo.setHomePageName(homePage.getHomePageName());
            vo.setHomePageUrl(homePage.getHomePageUrl());
        }
        vo.setSsoUrl(client != null ? buildSsoUrl(client) : null);
        return vo;
    }

    private UserExternalSystemRespVO toPortalEntryByHungLeaf(
            UserExternalSystemRespVO base, SubSystemUsersDO item, SubSystemDO subSystem,
            Map<String, String> workshopNameDict, Map<Long, Set<String>> workshopLeavesBySubSystem,
            List<Long> roleIds) {
        String mesClientId = StrUtil.blankToDefault(base.getMesClientId(), base.getClientId());
        Set<String> leaves = subSystem == null ? Collections.emptySet()
                : workshopLeavesBySubSystem.getOrDefault(subSystem.getId(), Collections.emptySet());
        boolean workshopScope = subSystem != null && subSystem.workshopScopeOn();
        String hungWorkshop = StrUtil.trim(item.getWorkshopId());

        // 未开车间维度、或尚未配置车间叶子：按业务系统本身进切换列表
        if (!workshopScope || leaves.isEmpty()) {
            UserExternalSystemRespVO vo = copyExternalSystem(base);
            vo.setMesClientId(mesClientId);
            vo.setClientId(mesClientId);
            vo.setClientName(StrUtil.blankToDefault(base.getClientName(), mesClientId));
            if (StrUtil.isNotBlank(hungWorkshop)) {
                vo.setWorkshopId(hungWorkshop);
                vo.setExternalUsername(buildExternalUsername(
                        base.getUsername(), base.getUsernameWithWorkshop(), hungWorkshop));
            }
            return vo;
        }

        // 一对一开了车间维度但未填车间：与 validateHangLeaf 可空一致，按唯一叶子进门户
        if (StrUtil.isBlank(hungWorkshop) && leaves.size() == 1) {
            hungWorkshop = leaves.iterator().next();
        }
        if (StrUtil.isBlank(hungWorkshop) || !leaves.contains(hungWorkshop)) {
            return null;
        }
        if (!rolesAllowWorkshop(roleIds, hungWorkshop)) {
            return null;
        }
        UserExternalSystemRespVO vo = copyExternalSystem(base);
        vo.setMesClientId(mesClientId);
        vo.setWorkshopId(hungWorkshop);
        String leafName = StrUtil.blankToDefault(workshopNameDict.get(hungWorkshop), hungWorkshop);
        if (leaves.size() == 1) {
            vo.setClientId(mesClientId);
            vo.setClientName(StrUtil.blankToDefault(base.getClientName(), leafName));
        } else {
            vo.setClientId(mesClientId + "_" + hungWorkshop);
            vo.setClientName(leafName);
        }
        // 只认花名册 username_with_workshop：1 且车间非空 → 车间_工号，否则工号
        vo.setUsernameWithWorkshop(StrUtil.blankToDefault(base.getUsernameWithWorkshop(), "0"));
        vo.setExternalUsername(buildExternalUsername(
                base.getUsername(), vo.getUsernameWithWorkshop(), hungWorkshop));
        return vo;
    }

    private boolean rolesAllowWorkshop(List<Long> roleIds, String workshopCode) {
        if (CollUtil.isEmpty(roleIds) || StrUtil.isBlank(workshopCode)) {
            return false;
        }
        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListByIds(roleIds);
        boolean anyWorkshopTagged = false;
        boolean hasUntagged = false;
        boolean hasUsable = false;
        for (SubSystemRoleDO role : roles) {
            if (role == null || (role.getStatus() != null && role.getStatus() != 0)) {
                continue;
            }
            hasUsable = true;
            String code = StrUtil.trim(role.getWorkshopCode());
            if (StrUtil.isBlank(code)) {
                // 未打车间的角色（如超级管理员）可进任意叶子
                hasUntagged = true;
                continue;
            }
            anyWorkshopTagged = true;
            if (workshopCode.equals(code)) {
                return true;
            }
        }
        // 角色全部停用/已删：视同无可用角色，不进叶子（与「未分配角色不显示入口」一致）
        if (!hasUsable) {
            return false;
        }
        return hasUntagged || !anyWorkshopTagged;
    }

    private Map<Long, SubSystemDO> loadSubSystemMap(List<SubSystemUsersDO> list) {
        Set<Long> ids = list.stream().map(SubSystemUsersDO::getSubSystemId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return convertMap(subSystemMapper.selectListByIds(ids), SubSystemDO::getId);
    }

    private Map<String, String> loadWorkshopNameDict() {
        Map<String, String> dict = new LinkedHashMap<>();
        try {
            for (SubSystemWorkshopDO row : CollUtil.emptyIfNull(subSystemWorkshopMapper.selectEnabledList())) {
                if (StrUtil.isBlank(row.getWorkshopCode())) {
                    continue;
                }
                dict.putIfAbsent(row.getWorkshopCode().trim(),
                        StrUtil.blankToDefault(row.getWorkshopName(), row.getWorkshopCode().trim()));
            }
        } catch (Exception ex) {
            log.debug("[loadWorkshopNameDict] skip: {}", ex.toString());
        }
        return dict;
    }

    private Map<Long, Set<String>> loadWorkshopLeavesBySubSystemId() {
        Map<Long, Set<String>> map = new HashMap<>();
        try {
            List<SubSystemWorkshopDO> rows = subSystemWorkshopMapper.selectEnabledList();
            if (CollUtil.isEmpty(rows)) {
                return map;
            }
            Map<Long, SubSystemDO> byId = convertMap(
                    subSystemMapper.selectListOrderByOauth2ClientId(), SubSystemDO::getId);
            Map<String, Long> clientToId = new HashMap<>();
            for (SubSystemDO ss : byId.values()) {
                String cid = StrUtil.blankToDefault(ss.getClientId(), "");
                if (StrUtil.isBlank(cid) && ss.getOauth2ClientId() != null) {
                    OAuth2ClientDO oc = getOAuth2Client(ss);
                    cid = oc != null ? StrUtil.nullToEmpty(oc.getClientId()) : "";
                }
                if (StrUtil.isNotBlank(cid)) {
                    clientToId.put(cid.trim().toLowerCase(Locale.ROOT), ss.getId());
                }
            }
            for (SubSystemWorkshopDO row : rows) {
                if (StrUtil.isBlank(row.getWorkshopCode())) {
                    continue;
                }
                Long ssId = row.getSubSystemId();
                if (ssId == null && StrUtil.isNotBlank(row.getClientId())) {
                    ssId = clientToId.get(row.getClientId().trim().toLowerCase(Locale.ROOT));
                }
                if (ssId == null) {
                    continue;
                }
                map.computeIfAbsent(ssId, k -> new LinkedHashSet<>()).add(row.getWorkshopCode().trim());
            }
        } catch (Exception ex) {
            log.debug("[loadWorkshopLeavesBySubSystemId] skip: {}", ex.toString());
        }
        return map;
    }

    private static String buildExternalUsername(String bareUsername, String usernameWithWorkshop, String workshopId) {
        if (StrUtil.isBlank(bareUsername)) {
            return bareUsername;
        }
        if ("1".equals(usernameWithWorkshop) && StrUtil.isNotBlank(workshopId)) {
            String workshopCode = workshopId.trim();
            if (bareUsername.startsWith(workshopCode + "_")) {
                return bareUsername;
            }
            return workshopCode + "_" + bareUsername;
        }
        return bareUsername;
    }

    private UserExternalSystemRespVO copyExternalSystem(UserExternalSystemRespVO src) {
        UserExternalSystemRespVO vo = new UserExternalSystemRespVO();
        vo.setId(src.getId());
        vo.setSubSystemId(src.getSubSystemId());
        vo.setSystemUrl(src.getSystemUrl());
        vo.setClientId(src.getClientId());
        vo.setMesClientId(src.getMesClientId());
        vo.setClientName(src.getClientName());
        vo.setLogo(src.getLogo());
        vo.setWorkshopId(src.getWorkshopId());
        vo.setTeamId(src.getTeamId());
        vo.setUsername(src.getUsername());
        vo.setUsernameWithWorkshop(src.getUsernameWithWorkshop());
        vo.setExternalUsername(src.getExternalUsername());
        vo.setHomeMenuId(src.getHomeMenuId());
        vo.setHomePageName(src.getHomePageName());
        vo.setHomePageUrl(src.getHomePageUrl());
        vo.setSsoUrl(src.getSsoUrl());
        return vo;
    }
    @Override
    public List<SubSystemPortalMenuRespVO> getMyPortalMenus(Long userId, Long subSystemId, String workshopCode) {
        String workshop = StrUtil.trim(workshopCode);
        List<SubSystemPortalMenuRespVO> cached = portalMyMenusRedisDAO.get(userId, subSystemId, workshop);
        if (cached != null) {
            warmPortalPermContext(userId, subSystemId);
            return cached;
        }
        SubSystemUsersDO subSystemUser = resolveHungRoster(userId, subSystemId, workshop);
        if (subSystemUser == null || "1".equals(subSystemUser.getStatus())) {
            throw exception(SUB_SYSTEM_USER_NOT_EXISTS);
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            return Collections.emptyList();
        }
        List<Long> roleIds = listRoleIds(subSystemUser.getId());
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
        portalMyMenusRedisDAO.set(userId, subSystemId, workshop, tree);
        return tree;
    }

    /**
     * 解析当前 JUMP 人在该业务系统下的挂接行：有车间则按叶子，否则系统级/兼容单行。
     */
    private SubSystemUsersDO resolveHungRoster(Long userId, Long subSystemId, String workshopCode) {
        if (StrUtil.isNotBlank(workshopCode)) {
            SubSystemUsersDO byLeaf = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                    subSystemId, userId, workshopCode.trim());
            if (byLeaf != null) {
                return byLeaf;
            }
            // 仅回退车间为空的系统级挂接
            SubSystemUsersDO systemLevel = subSystemUsersMapper
                    .selectBySubSystemIdAndMainUserIdAndWorkshopId(subSystemId, userId, "");
            if (systemLevel != null && StrUtil.isBlank(systemLevel.getWorkshopId())) {
                return systemLevel;
            }
            return null;
        }
        return subSystemUsersMapper.selectBySubSystemIdAndMainUserId(subSystemId, userId);
    }

    /** 合并该 JUMP 人在某业务系统下所有有效挂接行上的角色 ID */
    private LinkedHashSet<Long> collectHungRoleIds(Long userId, Long subSystemId) {
        LinkedHashSet<Long> roleIds = new LinkedHashSet<>();
        List<SubSystemUsersDO> rows = subSystemUsersMapper.selectListByMainUserId(userId);
        if (CollUtil.isEmpty(rows)) {
            return roleIds;
        }
        for (SubSystemUsersDO row : rows) {
            if (!Objects.equals(subSystemId, row.getSubSystemId()) || "1".equals(row.getStatus())) {
                continue;
            }
            roleIds.addAll(listRoleIds(row.getId()));
        }
        return roleIds;
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
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            return Collections.emptySet();
        }
        List<Long> roleIds = new ArrayList<>(collectHungRoleIds(userId, subSystemId));
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
    public Set<Long> retainAllowedQuickNavMenuIds(Long userId, Long subSystemId, Collection<Long> candidateIds,
                                                  String workshopCode) {
        if (CollUtil.isEmpty(candidateIds)) {
            return Collections.emptySet();
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        if (subSystem == null || CommonStatusEnum.isDisable(subSystem.getStatus())) {
            return Collections.emptySet();
        }
        List<Long> roleIds;
        if (StrUtil.isNotBlank(workshopCode)) {
            SubSystemUsersDO subSystemUser = resolveHungRoster(userId, subSystemId, workshopCode.trim());
            if (subSystemUser == null || "1".equals(subSystemUser.getStatus())) {
                return Collections.emptySet();
            }
            roleIds = listRoleIds(subSystemUser.getId());
        } else {
            // 无车间：合并该人在该系统下所有挂接行角色（供 Redis 超集缓存，再按叶子裁剪）
            roleIds = new ArrayList<>(collectHungRoleIds(userId, subSystemId));
        }
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
            // 业务菜单统一直开型：只认「路由地址」，组件路径（若依型）已废弃，不再下发
            vo.setComponent(null);
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

        // 直开型：路由地址为完整 http(s) 地址 → 原样直开（含 /#/ hash 地址，不再被组件路径分支抢占）
        if (StrUtil.isNotBlank(leafPath)
                && (leafPath.startsWith("http://") || leafPath.startsWith("https://"))) {
            return leafPath;
        }

        // 兼容存量：无 http 前缀的 IP:端口编码登记（如 192.168.240.12794200/...）→ 还原为 http 直开
        String routePath = buildMenuRoutePath(menu.getId(), menuMap);
        if (StrUtil.isBlank(routePath)) {
            return baseUrl;
        }
        if (routePath.contains(":") || routePath.matches(".*\\d+[./]\\d+[./]\\d+[./]\\d+.*")
                || routePath.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+9\\d{2,5}.*")) {
            String asHttp = slashIpPortPathToHttp(routePath);
            if (asHttp == null) {
                String normalized = routePath.replace(":", "/");
                normalized = normalized.replaceAll("(?<=^|/)(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)(?=/)", "$1/$2/$3/$4");
                asHttp = slashIpPortPathToHttp(normalized);
            }
            if (asHttp != null) {
                return asHttp;
            }
        }
        // 相对路由为废弃的若依型登记：回退系统入口，不再拼接 /#/
        return baseUrl;
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
