package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.exception.ServiceException;
import cn.jonhon.jump.module.infra.api.config.ConfigApi;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersAssignRoleReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.UserBizImportExcelVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemWorkshopMapper;
import cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.USER_IMPORT_INIT_PASSWORD;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.USER_IMPORT_LIST_IS_EMPTY;

/**
 * 一次导入 JUMP 主用户和业务账号。
 * 用户名已在 JUMP 里的，身份一致（姓名/工号/域账号）时沿用该用户，只补建业务账号，支持失败后重导；
 * 身份不一致才拒绝。同一个 JUMP 用户下，业务用户名不能重复。
 * 已存在且已挂当前用户的业务账号按表内数据对齐（姓名/角色/卡号/ERP，表内为空的卡号、ERP 不覆盖）——
 * 即导入对文件内容负责，重导会覆盖页面上对角色的手工增删。
 * 建新主用户时系统会按既有规则自动挂接同名/「工号-后缀」花名册；这些行如出现在表内，同样按表内数据对齐。
 * 勾选推送时，本地保存之后按该车间叶子上的接口绑定调用：新增人员，成功后再更新人员信息，然后分配角色。
 * 调哪个系统、哪条接口，只看叶子绑定，不在这里指定。
 */
@Service
@Slf4j
public class UserBizImportService {

    private static final String INIT_PASSWORD_KEY = "system.user.init-password";

    @Resource
    private ConfigApi configApi;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private SubSystemWorkshopMapper subSystemWorkshopMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemUsersService subSystemUsersService;
    @Resource
    private SubSystemEmployeeService subSystemEmployeeService;
    @Resource
    private LeafApiBindingService leafApiBindingService;
    @Resource
    private PlatformTransactionManager transactionManager;

    public UserImportRespVO importUsers(Long deptId, boolean syncToExternal, List<UserBizImportExcelVO> rows) {
        if (deptId == null) {
            throw exception0(BAD_REQUEST.getCode(), "请先选择部门");
        }
        if (CollUtil.isEmpty(rows)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        String initPassword = configApi.getConfigValueByKey(INIT_PASSWORD_KEY);
        if (StrUtil.isBlank(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }
        UserImportRespVO resp = UserImportRespVO.builder()
                .createUsernames(new ArrayList<>())
                .failureUsernames(new LinkedHashMap<>())
                .businessCreated(new ArrayList<>())
                .businessLinked(new ArrayList<>())
                .businessUpdated(new ArrayList<>())
                .build();
        Set<String> createdHere = new HashSet<>();
        Map<String, Set<String>> bizNamesByUser = new HashMap<>();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int index = 0;
        int success = 0;
        for (UserBizImportExcelVO row : rows) {
            index++;
            // 表头占第 1 行，数据从第 2 行开始
            int excelRow = index + 1;
            String key = "第 " + excelRow + " 行";
            try {
                Prepared prepared = prepare(row);
                key = "第 " + excelRow + " 行 " + prepared.username + " / " + prepared.bizUsername;
                Set<String> seenBizNames = bizNamesByUser.computeIfAbsent(prepared.username, name -> new HashSet<>());
                if (seenBizNames.contains(prepared.bizUsername)) {
                    throw exception0(BAD_REQUEST.getCode(),
                            "该用户下业务用户名【" + prepared.bizUsername + "】重复");
                }
                LocalResult local = tx.execute(status -> writeLocal(deptId, initPassword, prepared, createdHere));
                seenBizNames.add(prepared.bizUsername);
                if (syncToExternal) {
                    try {
                        pushExternal(local.rosterId);
                    } catch (Exception pushEx) {
                        throw new IllegalStateException("本地已保存。" + rootMessage(pushEx), pushEx);
                    }
                }
                if (createdHere.contains(prepared.username)
                        && !resp.getCreateUsernames().contains(prepared.username)) {
                    resp.getCreateUsernames().add(prepared.username);
                }
                String bizLabel = prepared.username + " / " + prepared.bizUsername;
                if (local.createdBiz) {
                    resp.getBusinessCreated().add(bizLabel);
                } else if (local.linkedExisting) {
                    resp.getBusinessLinked().add(bizLabel);
                } else if (local.updatedExisting) {
                    resp.getBusinessUpdated().add(bizLabel);
                }
                success++;
            } catch (Exception ex) {
                log.warn("[importUsers] {} 失败", key, ex);
                resp.getFailureUsernames().put(key, rootMessage(ex));
            }
        }
        resp.setSuccessCount(success);
        resp.setFailureCount(resp.getFailureUsernames().size());
        return resp;
    }

    private Prepared prepare(UserBizImportExcelVO row) {
        Prepared prepared = new Prepared();
        prepared.username = StrUtil.trim(row.getUsername());
        prepared.nickname = StrUtil.trim(row.getNickname());
        prepared.employeeNo = StrUtil.trimToNull(row.getEmployeeNo());
        prepared.domainNo = StrUtil.trimToNull(row.getDomainNo());
        prepared.workshopId = StrUtil.trim(row.getWorkshopId());
        prepared.bizUsername = StrUtil.trim(row.getBizUsername());
        prepared.cardNo = StrUtil.trimToNull(row.getCardNo());
        prepared.erpNo = StrUtil.trimToNull(row.getErpNo());
        if (StrUtil.isBlank(prepared.username) || StrUtil.isBlank(prepared.nickname)) {
            throw exception0(BAD_REQUEST.getCode(), "用户名、用户姓名不能为空");
        }
        if (!prepared.username.matches("^[a-zA-Z0-9]{4,30}$")) {
            throw exception0(BAD_REQUEST.getCode(), "用户名须为 4-30 位字母或数字");
        }
        if (prepared.nickname.length() > 30) {
            throw exception0(BAD_REQUEST.getCode(), "用户姓名不能超过 30 个字符");
        }
        if (prepared.employeeNo != null && prepared.employeeNo.length() > 20) {
            throw exception0(BAD_REQUEST.getCode(), "工号不能超过 20 个字符");
        }
        if (prepared.domainNo != null && prepared.domainNo.length() > 32) {
            throw exception0(BAD_REQUEST.getCode(), "域账号不能超过 32 个字符");
        }
        if (StrUtil.hasBlank(prepared.workshopId, prepared.bizUsername, row.getRoleCodes())) {
            throw exception0(BAD_REQUEST.getCode(), "车间编号、业务用户名、角色标识不能为空");
        }
        if (prepared.bizUsername.length() > 64) {
            throw exception0(BAD_REQUEST.getCode(), "业务用户名不能超过 64 个字符");
        }
        SubSystemWorkshopDO leaf = subSystemWorkshopMapper.selectByWorkshopCode(prepared.workshopId);
        if (leaf == null || leaf.getSubSystemId() == null) {
            throw exception0(BAD_REQUEST.getCode(), "车间编号【" + prepared.workshopId + "】不是业务系统叶子");
        }
        prepared.subSystemId = leaf.getSubSystemId();
        prepared.roleIds = resolveRoleIds(prepared.subSystemId, row.getRoleCodes());
        if (prepared.cardNo != null && prepared.cardNo.length() > 64) {
            throw exception0(BAD_REQUEST.getCode(), "刷卡卡号不能超过 64 个字符");
        }
        if (prepared.erpNo != null && prepared.erpNo.length() > 64) {
            throw exception0(BAD_REQUEST.getCode(), "ERP号不能超过 64 个字符");
        }
        return prepared;
    }

    private LocalResult writeLocal(Long deptId, String initPassword, Prepared prepared, Set<String> createdHere) {
        LocalResult local = new LocalResult();
        AdminUserDO user = adminUserMapper.selectByUsername(prepared.username);
        if (user == null) {
            AdminUserDO employeeOwner = prepared.employeeNo == null ? null
                    : adminUserService.getUserByEmployeeNo(prepared.employeeNo);
            if (employeeOwner != null) {
                throw exception0(BAD_REQUEST.getCode(),
                        "工号【" + prepared.employeeNo + "】已被用户【" + employeeOwner.getUsername() + "】使用");
            }
            UserSaveReqVO createReq = new UserSaveReqVO();
            createReq.setUsername(prepared.username);
            createReq.setNickname(prepared.nickname);
            createReq.setDeptId(deptId);
            createReq.setEmployeeNo(prepared.employeeNo);
            createReq.setDomainNo(prepared.domainNo);
            createReq.setPassword(initPassword);
            createReq.setPostIds(new HashSet<>());
            adminUserService.createUser(createReq);
            user = adminUserMapper.selectByUsername(prepared.username);
            createdHere.add(prepared.username);
            local.createdUser = true;
        } else {
            // 已存在用户（重导/增量补账号）：身份一致才沿用，避免改到别人的主数据
            assertSameMaster(user, prepared);
            // 表内填了工号而库里为空：先确认没被别人占用，再回填主用户，增量导入能补全资料
            if (prepared.employeeNo != null && StrUtil.isBlank(user.getEmployeeNo())) {
                AdminUserDO employeeOwner = adminUserService.getUserByEmployeeNo(prepared.employeeNo);
                if (employeeOwner != null) {
                    throw exception0(BAD_REQUEST.getCode(),
                            "工号【" + prepared.employeeNo + "】已被用户【" + employeeOwner.getUsername() + "】使用");
                }
                AdminUserDO patch = new AdminUserDO();
                patch.setId(user.getId());
                patch.setEmployeeNo(prepared.employeeNo);
                adminUserMapper.updateById(patch);
            }
        }
        SubSystemUsersDO roster = subSystemUsersMapper.selectBySubSystemIdAndUsernameAndWorkshopId(
                prepared.subSystemId, prepared.bizUsername, prepared.workshopId);
        if (roster == null) {
            // 新建前保证该用户名下（任意系统）没有同名业务账号
            assertBizUsernameUnique(user.getId(), prepared.bizUsername);
            // 一个用户在一个叶子只挂一条业务账号，防止与自动挂接/历史数据撞出两条
            SubSystemUsersDO occupied = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                    prepared.subSystemId, user.getId(), prepared.workshopId);
            if (occupied != null) {
                throw exception0(BAD_REQUEST.getCode(), "该用户在车间【" + prepared.workshopId
                        + "】已挂接业务账号【" + occupied.getUsername() + "】");
            }
            SubSystemUsersSaveReqVO createReq = new SubSystemUsersSaveReqVO();
            createReq.setSubSystemId(prepared.subSystemId);
            createReq.setMainUserId(user.getId());
            createReq.setUsername(prepared.bizUsername);
            createReq.setNickname(prepared.nickname);
            createReq.setWorkshopId(prepared.workshopId);
            createReq.setStatus("0");
            createReq.setEmployeeRegistered("0");
            createReq.setUsernameWithWorkshop("0");
            createReq.setCardNo(prepared.cardNo);
            createReq.setErpNo(prepared.erpNo);
            Long rosterId = subSystemUsersService.createSubSystemUser(createReq);
            assignRolesLocal(rosterId, prepared.roleIds);
            local.rosterId = rosterId;
            local.createdBiz = true;
            return local;
        }
        if (roster.getMainUserId() != null && !roster.getMainUserId().equals(user.getId())) {
            throw exception0(BAD_REQUEST.getCode(), "业务账号【" + prepared.bizUsername + "】已挂到其他用户");
        }
        local.rosterId = roster.getId();
        local.createdBiz = false;
        if (roster.getMainUserId() == null) {
            // 挂接前同样保证该用户名下没有同名业务账号
            assertBizUsernameUnique(user.getId(), prepared.bizUsername);
            SubSystemUsersDO occupied = subSystemUsersMapper.selectBySubSystemIdAndMainUserIdAndWorkshopId(
                    prepared.subSystemId, user.getId(), prepared.workshopId);
            if (occupied != null) {
                throw exception0(BAD_REQUEST.getCode(), "该用户在车间【" + prepared.workshopId
                        + "】已挂接业务账号【" + occupied.getUsername() + "】");
            }
            SubSystemUsersDO hang = new SubSystemUsersDO();
            hang.setId(roster.getId());
            hang.setMainUserId(user.getId());
            hang.setNickname(prepared.nickname);
            if (prepared.cardNo != null) {
                hang.setCardNo(prepared.cardNo);
            }
            if (prepared.erpNo != null) {
                hang.setErpNo(prepared.erpNo);
            }
            updateRosterSafely(hang);
            assignRolesLocal(roster.getId(), prepared.roleIds);
            local.linkedExisting = true;
        } else {
            // 已挂当前用户（重导，或建主用户时自动挂接抢先挂上）：按 Excel 数据对齐，导入即对表内内容负责。
            // 卡号/ERP 表内为空时不覆盖库中已有值
            SubSystemUsersDO update = new SubSystemUsersDO();
            update.setId(roster.getId());
            update.setNickname(prepared.nickname);
            if (prepared.cardNo != null) {
                update.setCardNo(prepared.cardNo);
            }
            if (prepared.erpNo != null) {
                update.setErpNo(prepared.erpNo);
            }
            updateRosterSafely(update);
            assignRolesLocal(roster.getId(), prepared.roleIds);
            local.updatedExisting = true;
        }
        return local;
    }

    /** 挂接/对齐都是按 id 的局部更新。刷卡卡号、ERP 号允许重复，不在这里拦截。 */
    private void updateRosterSafely(SubSystemUsersDO update) {
        subSystemUsersMapper.updateById(update);
    }

    private void pushExternal(Long rosterId) {
        SubSystemUserRegisterReqVO registerReq = new SubSystemUserRegisterReqVO();
        registerReq.setIds(Collections.singletonList(rosterId));
        // 不显式传拼接标记：按花名册自身 username_with_workshop 决定。
        // 存量标记行按 车间_工号 注册，与后续更新/授权用的 externalUserCode 一致，避免注册和授权对不上
        List<SubSystemUserRegisterRespVO> registered;
        try {
            registered = subSystemEmployeeService.registerEmployees(registerReq);
        } catch (Exception ex) {
            throw step("新增人员", ex);
        }
        if (CollUtil.isEmpty(registered) || !Boolean.TRUE.equals(registered.get(0).getSuccess())) {
            String message = CollUtil.isEmpty(registered) ? "没有返回结果" : registered.get(0).getMessage();
            throw exception0(BAD_REQUEST.getCode(), "新增人员失败：" + StrUtil.blankToDefault(message, "接口调用失败"));
        }
        try {
            subSystemEmployeeService.syncImportedAccount(rosterId);
        } catch (Exception ex) {
            throw step("更新人员信息", ex);
        }
        SubSystemUsersDO roster = subSystemUsersMapper.selectById(rosterId);
        if (roster == null) {
            throw exception0(BAD_REQUEST.getCode(), "业务账号不存在，无法分配角色");
        }
        try {
            Long apiSubSystemId = leafApiBindingService.requireApiSubSystemId(
                    roster.getSubSystemId(), roster.getWorkshopId(), LeafApiSceneEnum.ROLE_ASSIGN);
            List<Long> roleIds = subSystemUsersService.getSubSystemUserRoleIds(rosterId);
            if (CollUtil.isEmpty(roleIds)) {
                throw exception0(BAD_REQUEST.getCode(), "没有可同步的角色");
            }
            subSystemEmployeeService.syncUserRoles(apiSubSystemId, roster.getSubSystemId(),
                    subSystemEmployeeService.externalUserCode(roster), roleIds);
        } catch (Exception ex) {
            throw step("分配角色", ex);
        }
    }

    private void assignRolesLocal(Long rosterId, List<Long> roleIds) {
        SubSystemUsersAssignRoleReqVO assignReq = new SubSystemUsersAssignRoleReqVO();
        assignReq.setId(rosterId);
        assignReq.setRoleIds(roleIds);
        assignReq.setSyncToExternal(Boolean.FALSE);
        subSystemUsersService.assignSubSystemUserRole(assignReq);
    }

    private void assertBizUsernameUnique(Long mainUserId, String bizUsername) {
        List<SubSystemUsersDO> owned = subSystemUsersMapper.selectListByMainUserId(mainUserId);
        for (SubSystemUsersDO item : owned) {
            if (bizUsername.equals(StrUtil.trim(item.getUsername()))) {
                throw exception0(BAD_REQUEST.getCode(), "该用户下业务用户名【" + bizUsername + "】已存在");
            }
        }
    }

    private void assertSameMaster(AdminUserDO user, Prepared prepared) {
        if (!StrUtil.equals(StrUtil.trim(user.getNickname()), prepared.nickname)) {
            throw exception0(BAD_REQUEST.getCode(), "用户【" + prepared.username + "】在 JUMP 中姓名为【"
                    + StrUtil.trim(user.getNickname()) + "】，与表内【" + prepared.nickname + "】不一致");
        }
        if (prepared.employeeNo != null && StrUtil.isNotBlank(user.getEmployeeNo())
                && !prepared.employeeNo.equals(StrUtil.trim(user.getEmployeeNo()))) {
            throw exception0(BAD_REQUEST.getCode(), "用户【" + prepared.username + "】在 JUMP 中工号为【"
                    + StrUtil.trim(user.getEmployeeNo()) + "】，与表内【" + prepared.employeeNo + "】不一致");
        }
        if (prepared.domainNo != null && StrUtil.isNotBlank(user.getDomainNo())
                && !prepared.domainNo.equals(StrUtil.trim(user.getDomainNo()))) {
            throw exception0(BAD_REQUEST.getCode(), "用户【" + prepared.username + "】在 JUMP 中域账号为【"
                    + StrUtil.trim(user.getDomainNo()) + "】，与表内【" + prepared.domainNo + "】不一致");
        }
    }

    private List<Long> resolveRoleIds(Long subSystemId, String roleCodes) {
        List<String> codes = splitCodes(roleCodes);
        if (codes.isEmpty()) {
            throw exception0(BAD_REQUEST.getCode(), "角色标识不能为空");
        }
        List<SubSystemRoleDO> roles = subSystemRoleMapper.selectListBySubSystemId(subSystemId);
        List<Long> ids = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String code : codes) {
            SubSystemRoleDO matched = roles.stream()
                    .filter(role -> code.equals(StrUtil.trim(role.getCode())))
                    .findFirst().orElse(null);
            if (matched == null) {
                missing.add(code);
            } else {
                ids.add(matched.getId());
            }
        }
        if (!missing.isEmpty()) {
            throw exception0(BAD_REQUEST.getCode(), "角色标识未匹配：" + String.join("、", missing));
        }
        return ids;
    }

    private List<String> splitCodes(String raw) {
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[,，;；]+"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                // Login 是业务系统自带角色，导入表不填，也不写入 JUMP
                .filter(code -> !"Login".equalsIgnoreCase(code))
                .distinct()
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
    }

    private RuntimeException step(String name, Exception ex) {
        String message = rootMessage(ex);
        if (message.startsWith(name)) {
            return ex instanceof RuntimeException ? (RuntimeException) ex : new IllegalStateException(message);
        }
        return exception0(BAD_REQUEST.getCode(), name + "失败：" + message);
    }

    private String rootMessage(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current
                && StrUtil.isBlank(current.getMessage())) {
            current = current.getCause();
        }
        if (current instanceof ServiceException && StrUtil.isNotBlank(current.getMessage())) {
            return current.getMessage();
        }
        return StrUtil.blankToDefault(current.getMessage(), "导入失败");
    }

    private static class Prepared {
        private String username;
        private String nickname;
        private String employeeNo;
        private String domainNo;
        private Long subSystemId;
        private String workshopId;
        private String bizUsername;
        private String cardNo;
        private String erpNo;
        private List<Long> roleIds;
    }

    private static class LocalResult {
        private Long rosterId;
        private boolean createdUser;
        private boolean createdBiz;
        private boolean linkedExisting;
        private boolean updatedExisting;
    }

}
