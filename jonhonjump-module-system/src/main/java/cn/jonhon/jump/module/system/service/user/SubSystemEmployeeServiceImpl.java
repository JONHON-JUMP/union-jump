package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRegisterableApiRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUserRegisterRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemRoleDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemRoleMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.CamstarEmployeeApiAdapter;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApi;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_EMPLOYEE_API_ERROR;

/**
 * 子系统人员 Service 实现（经适配器分发）
 */
@Service
@Validated
@Slf4j
public class SubSystemEmployeeServiceImpl implements SubSystemEmployeeService {

    @Resource
    private SubSystemEmployeeApiFactory subSystemEmployeeApiFactory;
    @Resource
    private SubSystemApiConfigService subSystemApiConfigService;
    @Resource
    private SubSystemApiConfigMapper subSystemApiConfigMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemRoleMapper subSystemRoleMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private SubSystemAccessService subSystemAccessService;
    @Resource
    private SubSystemPermissionContextService subSystemPermissionContextService;

    @Override
    public PageResult<SubSystemEmployeeRespVO> getEmployeePage(SubSystemEmployeePageReqVO pageReqVO) {
        SubSystemEmployeeQueryDTO query = new SubSystemEmployeeQueryDTO();
        query.setPage(pageReqVO.getPageNo());
        query.setRows(pageReqVO.getPageSize());
        query.setWorkshopCode(pageReqVO.getWorkshopCode());
        query.setUserCode(pageReqVO.getUserCode());
        query.setUserName(pageReqVO.getUserName());
        try {
            SubSystemEmployeePageRespDTO page = getApi(pageReqVO.getSubSystemId()).page(query);
            List<SubSystemEmployeeRespVO> list = page.getList() == null
                    ? Collections.emptyList()
                    : page.getList().stream()
                            .map(dto -> BeanUtils.toBean(dto, SubSystemEmployeeRespVO.class))
                            .collect(Collectors.toList());
            return new PageResult<>(list, page.getTotal());
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public void createEmployee(SubSystemEmployeeSaveReqVO createReqVO) {
        try {
            SubSystemEmployeeDTO dto = BeanUtils.toBean(createReqVO, SubSystemEmployeeDTO.class);
            applyWorkshopPrefixedUserCode(dto);
            getApi(createReqVO.getSubSystemId()).create(dto);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public void updateEmployee(SubSystemEmployeeSaveReqVO updateReqVO) {
        try {
            SubSystemEmployeeDTO dto = BeanUtils.toBean(updateReqVO, SubSystemEmployeeDTO.class);
            applyWorkshopPrefixedUserCode(dto);
            getApi(updateReqVO.getSubSystemId()).update(dto);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public void deleteEmployee(Long subSystemId, String userCode) {
        try {
            getApi(subSystemId).delete(userCode);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public List<SubSystemTeamComboDTO> getTeamCombo(Long subSystemId, String workshopCode) {
        try {
            List<SubSystemTeamComboDTO> list = getApi(subSystemId).teamCombo(workshopCode);
            return CollUtil.isEmpty(list) ? Collections.emptyList() : list;
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public String getDeleteTip(Long subSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigService.getEnabledConfigBySubSystemId(subSystemId);
        return config == null ? null : config.getDeleteTip();
    }

    @Override
    public List<SubSystemRegisterableApiRespVO> getRegisterableApis() {
        List<SubSystemApiConfigDO> configs = subSystemApiConfigMapper.selectList();
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyList();
        }
        // 接口目标 = create 用途接口已启用的接入系统（如 Camstar人员管理），与花名册系统解耦
        List<Long> apiSubSystemIds = configs.stream()
                .map(SubSystemApiConfigDO::getSubSystemId)
                .filter(subSystemApiConfigService::isCreateEndpointEnabled)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(apiSubSystemIds)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = subSystemMapper.selectListByIds(apiSubSystemIds).stream()
                .collect(Collectors.toMap(SubSystemDO::getId, s -> s, (a, b) -> a, LinkedHashMap::new));
        return apiSubSystemIds.stream()
                .filter(subSystemMap::containsKey)
                .map(id -> new SubSystemRegisterableApiRespVO()
                        .setSubSystemId(id)
                        .setSystemName(subSystemMap.get(id).getSystemName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubSystemEmployeeRespVO> listExternalEmployees(Long apiSubSystemId, String workshopCode, String userCode) {
        if (apiSubSystemId == null) {
            throw exception0(BAD_REQUEST.getCode(), "请选择接口目标");
        }
        if (StrUtil.isBlank(userCode)) {
            throw exception0(BAD_REQUEST.getCode(), "请按工号查询外部人员");
        }
        // 与接口管理「查询人员」相同：带工号、不传车间。按车间拉固定条数会把新建用户截在页外
        List<SubSystemEmployeeDTO> rows = queryEmployeesByUserCode(apiSubSystemId, userCode.trim(), workshopCode, true);
        return rows.stream()
                .map(dto -> BeanUtils.toBean(dto, SubSystemEmployeeRespVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubSystemUserRegisterRespVO> registerEmployees(SubSystemUserRegisterReqVO reqVO) {
        // 接口目标必须已配置且新增接口启用（未配置时 getApi 会抛具体业务异常）
        getApi(reqVO.getApiSubSystemId());
        // 用户名拼接车间编号为可选项，对任意类型接口（camstar/http）均可用：
        // 对接系统要求用户名全局唯一时，以 车间编号_工号 注册，同一工号可登录不同车间系统
        boolean usernameWithWorkshop = Boolean.TRUE.equals(reqVO.getUsernameWithWorkshop());
        List<SubSystemUsersDO> rosters = subSystemUsersMapper.selectBatchIds(reqVO.getIds());
        Map<Long, SubSystemUsersDO> rosterMap = rosters.stream()
                .collect(Collectors.toMap(SubSystemUsersDO::getId, r -> r, (a, b) -> a));
        Set<Long> rosterSubSystemIds = new HashSet<>();
        for (Long id : reqVO.getIds()) {
            SubSystemUsersDO roster = rosterMap.get(id);
            if (roster != null && roster.getSubSystemId() != null) {
                rosterSubSystemIds.add(roster.getSubSystemId());
            }
        }
        if (rosterSubSystemIds.size() > 1) {
            throw exception0(BAD_REQUEST.getCode(), "所选用户分属不同业务系统，请分批注册");
        }
        List<SubSystemUserRegisterRespVO> results = new ArrayList<>(reqVO.getIds().size());
        for (Long id : reqVO.getIds()) {
            SubSystemUsersDO roster = rosterMap.get(id);
            SubSystemUserRegisterRespVO result = new SubSystemUserRegisterRespVO();
            results.add(result);
            if (roster == null) {
                result.setId(id).setSuccess(false).setMessage("花名册记录不存在");
                continue;
            }
            result.setId(roster.getId())
                    .setUsername(roster.getUsername())
                    .setNickname(roster.getNickname());
            // 可管系统范围校验：受限角色只能推送授权系统的花名册
            try {
                subSystemAccessService.checkAccessible(roster.getSubSystemId());
            } catch (Exception e) {
                result.setSuccess(false).setMessage(e.getMessage());
                continue;
            }
            if ("1".equals(roster.getEmployeeRegistered())) {
                result.setSuccess(true).setMessage("已注册，跳过推送");
                continue;
            }
            try {
                SubSystemEmployeeDTO dto = buildEmployeeDTO(roster, reqVO.getApiSubSystemId(), reqVO.getWorkshopCode(),
                        usernameWithWorkshop);
                if (StrUtil.isBlank(dto.getWorkshopCode())) {
                    result.setSuccess(false).setMessage("请填写车间编号");
                    continue;
                }
                // 按工号全系统核对（其他车间以 车间_工号 形态注册的也算同一工号），防止同一工号在对方系统重复建号
                SubSystemEmployeeDTO existing = findExternalEmployee(reqVO.getApiSubSystemId(), roster.getUsername(),
                        dto.getWorkshopCode());
                if (existing != null && !StrUtil.trim(dto.getUserCode())
                        .equalsIgnoreCase(StrUtil.trim(existing.getUserCode()))) {
                    String workshopInfo = StrUtil.blankToDefault(StrUtil.trim(existing.getWorkshopName()),
                            StrUtil.trim(existing.getWorkshopCode()));
                    result.setSuccess(false).setMessage("工号 " + roster.getUsername() + " 已在对方系统注册（账号 "
                            + StrUtil.trim(existing.getUserCode())
                            + (StrUtil.isBlank(workshopInfo) ? "" : "，车间 " + workshopInfo)
                            + "），本次未注册。如属人员调动，请先在对方系统调整原账号后重试");
                    continue;
                }
                boolean alreadyExists = existing != null;
                if (!alreadyExists) {
                    getApi(reqVO.getApiSubSystemId()).create(dto);
                }
                String markMessage = markRegisteredWithRetry(roster.getId(), dto.getWorkshopCode(),
                        usernameWithWorkshop, resolveApiType(reqVO.getApiSubSystemId()));
                result.setSuccess(true);
                if (alreadyExists) {
                    result.setMessage("对方已有该人员，已标记为已注册");
                } else if (markMessage != null) {
                    result.setMessage(markMessage);
                }
            } catch (ExternalApiException e) {
                log.warn("[registerEmployees] rosterId={} apiSubSystemId={} 调用新增人员接口失败",
                        roster.getId(), reqVO.getApiSubSystemId(), e);
                result.setSuccess(false).setMessage(e.getMessage());
            } catch (Exception e) {
                log.warn("[registerEmployees] rosterId={} apiSubSystemId={} 注册异常",
                        roster.getId(), reqVO.getApiSubSystemId(), e);
                result.setSuccess(false).setMessage(e.getMessage());
            }
        }
        return results;
    }

    @Override
    public void syncUserRoles(Long apiSubSystemId, Long rosterSubSystemId, String userCode, List<Long> jumpRoleIds) {
        if (apiSubSystemId == null) {
            throw exception0(BAD_REQUEST.getCode(), "请选择接口目标");
        }
        if (StrUtil.isBlank(userCode)) {
            throw exception0(BAD_REQUEST.getCode(), "同步外部角色需要用户名");
        }
        SubSystemEmployeeApi api = getApi(apiSubSystemId);
        if (!(api instanceof CamstarEmployeeApiAdapter)) {
            throw exception0(BAD_REQUEST.getCode(), "该接口目标不是 Camstar，暂不支持角色同步");
        }
        // 目标角色：本次勾选角色中已关联外部 roleId 的；未关联的直接报错引导先绑定
        List<SubSystemRoleDO> roles = CollUtil.isEmpty(jumpRoleIds)
                ? Collections.emptyList() : subSystemRoleMapper.selectListByIds(jumpRoleIds);
        Set<String> targetRoleIds = new LinkedHashSet<>();
        List<String> unboundRoleNames = new ArrayList<>();
        for (SubSystemRoleDO role : roles) {
            if (StrUtil.isBlank(role.getExternalRoleId())) {
                unboundRoleNames.add(role.getName());
            } else {
                targetRoleIds.add(role.getExternalRoleId().trim());
            }
        }
        if (!unboundRoleNames.isEmpty()) {
            throw exception0(BAD_REQUEST.getCode(), "角色【" + String.join("、", unboundRoleNames)
                    + "】未关联外部角色，请先在业务角色管理中「关联外部角色」，或取消勾选同步");
        }
        // JUMP 管辖角色全集：花名册系统下所有已关联外部 roleId 的角色（同步只在这个范围内增删）
        Set<String> managedRoleIds = subSystemRoleMapper.selectListBySubSystemId(rosterSubSystemId).stream()
                .map(SubSystemRoleDO::getExternalRoleId)
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        try {
            ((CamstarEmployeeApiAdapter) api).assignRoles(userCode.trim(), managedRoleIds, targetRoleIds);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    /** 车间优先：花名册已填 > 注册弹窗指定 > 花名册系统车间对照 > 接口目标车间对照 */
    private SubSystemEmployeeDTO buildEmployeeDTO(SubSystemUsersDO roster, Long apiSubSystemId, String overrideWorkshopCode,
                                                  boolean usernameWithWorkshop) {
        AdminUserDO mainUser = roster.getMainUserId() == null
                ? null : adminUserMapper.selectById(roster.getMainUserId());
        SubSystemEmployeeDTO dto = new SubSystemEmployeeDTO();
        String workshopCode = resolveWorkshopCode(roster, apiSubSystemId, mainUser, overrideWorkshopCode);
        dto.setWorkshopCode(workshopCode);
        // Camstar 用户名全局唯一：勾选拼接时以 车间编号_工号 注册，同一工号可登录不同车间 MES
        dto.setUserCode(usernameWithWorkshop && StrUtil.isNotBlank(workshopCode)
                ? workshopCode.trim() + "_" + roster.getUsername() : roster.getUsername());
        // Camstar 的 userName 是姓名，校验文案写成「用户名」。花名册没填姓名时用登录用户名，避免对方报【用户名】不允许为空
        String userName = StrUtil.blankToDefault(StrUtil.trim(roster.getNickname()),
                mainUser != null ? StrUtil.trim(mainUser.getNickname()) : null);
        dto.setUserName(StrUtil.blankToDefault(userName, StrUtil.trim(roster.getUsername())));
        dto.setTeamCode(roster.getTeamId());
        if (mainUser != null) {
            dto.setDomainName(mainUser.getDomainNo());
            if (CollUtil.isNotEmpty(mainUser.getErpNos())) {
                dto.setErpNo(mainUser.getErpNos().iterator().next());
            }
            dto.setCardNo(mainUser.getCardNo());
        }
        return dto;
    }

    /** 仅用花名册自身编号或本次入参；不再从部门对照/系统名推断 */
    private String resolveWorkshopCode(SubSystemUsersDO roster, Long apiSubSystemId, AdminUserDO mainUser,
                                       String overrideWorkshopCode) {
        if (StrUtil.isNotBlank(roster.getWorkshopId())) {
            return roster.getWorkshopId().trim();
        }
        if (StrUtil.isNotBlank(overrideWorkshopCode)) {
            return overrideWorkshopCode.trim();
        }
        return null;
    }

    /**
     * 按工号全系统查询，与接口管理「查询人员」同一条 getEmployeeInfo：带 userCode，不传车间。
     * Camstar 工号是精确条件，裸工号查不到「车间_工号」，所以已知车间时再按完整账号查一次。
     * 返回列表里只保留同一工号（含其他车间的 车间_工号）。
     *
     * @param failOnPrimary 裸工号这一查失败时是否抛出；补查失败只记日志
     */
    private List<SubSystemEmployeeDTO> queryEmployeesByUserCode(Long apiSubSystemId, String userCode,
                                                                String workshopCode, boolean failOnPrimary) {
        String bare = bareEmployeeCode(userCode);
        Map<String, SubSystemEmployeeDTO> merged = new LinkedHashMap<>();
        boolean primaryOk = appendEmployeesByCode(apiSubSystemId, bare, bare, merged, failOnPrimary);
        if (StrUtil.isNotBlank(workshopCode)) {
            String prefixed = workshopCode.trim() + "_" + bare;
            if (!prefixed.equalsIgnoreCase(bare)) {
                appendEmployeesByCode(apiSubSystemId, prefixed, bare, merged, false);
            }
        }
        if (!userCode.trim().equalsIgnoreCase(bare)) {
            appendEmployeesByCode(apiSubSystemId, userCode.trim(), bare, merged, false);
        }
        if (!primaryOk && merged.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(merged.values());
    }

    /** 按一个账号名查一页，收进工号完全一致、或工号部分与 wantedBare 一致的行。 */
    private boolean appendEmployeesByCode(Long apiSubSystemId, String queryCode, String wantedBare,
                                          Map<String, SubSystemEmployeeDTO> merged, boolean required) {
        try {
            SubSystemEmployeeQueryDTO query = new SubSystemEmployeeQueryDTO();
            query.setUserCode(queryCode);
            query.setPage(1);
            query.setRows(20);
            SubSystemEmployeePageRespDTO page = getApi(apiSubSystemId).page(query);
            List<SubSystemEmployeeDTO> rows = page == null || page.getList() == null
                    ? Collections.emptyList() : page.getList();
            for (SubSystemEmployeeDTO row : rows) {
                if (row == null || StrUtil.isBlank(row.getUserCode())) {
                    continue;
                }
                String code = row.getUserCode().trim();
                if (!queryCode.equalsIgnoreCase(code) && !wantedBare.equalsIgnoreCase(bareEmployeeCode(code))) {
                    continue;
                }
                merged.putIfAbsent(code.toLowerCase(), row);
            }
            return true;
        } catch (Exception e) {
            if (required) {
                if (e instanceof ExternalApiException) {
                    throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, ((ExternalApiException) e).getMessage());
                }
                throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
            }
            log.warn("[listExternalEmployees][按工号({})查询外部人员失败]", queryCode, e);
            return false;
        }
    }

    /**
     * 注册前按工号全系统核对：账号名完全一致，或为「车间_工号」形态且工号部分一致，均视为已存在。
     * 查不到或查询失败都按「对方还没有」处理，继续走新增。
     */
    private SubSystemEmployeeDTO findExternalEmployee(Long apiSubSystemId, String userCode, String workshopCode) {
        if (StrUtil.isBlank(userCode)) {
            return null;
        }
        try {
            List<SubSystemEmployeeDTO> rows = queryEmployeesByUserCode(apiSubSystemId, userCode.trim(), workshopCode, false);
            return CollUtil.isEmpty(rows) ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("[registerEmployees] 注册前查询对方人员失败，继续尝试新增。userCode={}", userCode, e);
            return null;
        }
    }

    /**
     * 去掉「车间_」前缀取工号部分。拼接规则为 车间编号_工号，工号本身约定不含下划线。
     */
    private String bareEmployeeCode(String userCode) {
        String code = StrUtil.trim(userCode);
        if (StrUtil.isEmpty(code)) {
            return code;
        }
        int idx = code.lastIndexOf('_');
        return idx >= 0 ? code.substring(idx + 1) : code;
    }

    /**
     * 对方人员已建出之后再写本地注册状态。第一次失败再试一次；仍失败则告诉管理员不要再次注册。
     */
    private String markRegisteredWithRetry(Long rosterId, String workshopCode, boolean usernameWithWorkshop,
                                           String apiType) {
        try {
            markRegistered(rosterId, workshopCode, usernameWithWorkshop, apiType);
            return null;
        } catch (Exception first) {
            log.warn("[registerEmployees] rosterId={} 写入注册状态失败，准备重试", rosterId, first);
            try {
                markRegistered(rosterId, workshopCode, usernameWithWorkshop, apiType);
                return "对方人员已创建，本地注册状态已补记";
            } catch (Exception second) {
                log.warn("[registerEmployees] rosterId={} 写入注册状态再次失败", rosterId, second);
                throw new IllegalStateException(
                        "对方系统已有该人员，但本地注册状态写入失败。请在列表将接口注册改为已注册，不要再次注册。");
            }
        }
    }

    private void markRegistered(Long rosterId, String workshopCode, boolean usernameWithWorkshop, String apiType) {
        SubSystemUsersDO updateObj = new SubSystemUsersDO();
        updateObj.setId(rosterId);
        updateObj.setEmployeeRegistered("1");
        // 记录拼接方式：后续访问子系统与手动接口都按该标记还原 车间编号_工号
        updateObj.setUsernameWithWorkshop(usernameWithWorkshop ? "1" : "0");
        // 记录所调接口类型：Camstar Cookie 身份预取只认注册到 camstar 的行
        updateObj.setRegisteredApiType(apiType);
        if (StrUtil.isNotBlank(workshopCode)) {
            updateObj.setWorkshopId(workshopCode.trim());
        }
        subSystemUsersMapper.updateById(updateObj);
        // 注册状态/拼接方式/车间变了，门户权限包中的外部身份（车间_工号）一并失效
        subSystemPermissionContextService.evictBySubSystemUserId(rosterId);
    }

    /** 所调「新增人员」接口的适配器类型（注册成功后记录在行上） */
    private String resolveApiType(Long apiSubSystemId) {
        SubSystemApiConfigDO config = subSystemApiConfigMapper.selectBySubSystemId(apiSubSystemId);
        return config != null ? config.getApiType() : null;
    }

    /**
     * 手动新增/修改人员时的用户名归一：已按 车间编号_工号 注册过的用户，传裸工号会被对接系统
     * 的 addOrUpdateUser（按 userCode 精确匹配）当成新用户重复建号，这里按行上的拼接标记自动补车间前缀。
     * 传入已含 "_" 视为操作者明确指定全名，原样透传。
     */
    private void applyWorkshopPrefixedUserCode(SubSystemEmployeeDTO dto) {
        if (StrUtil.isBlank(dto.getUserCode()) || dto.getUserCode().contains("_")) {
            return;
        }
        List<SubSystemUsersDO> rosters = subSystemUsersMapper
                .selectListByUsernameAndUsernameWithWorkshop(dto.getUserCode().trim());
        if (CollUtil.isEmpty(rosters)) {
            return;
        }
        if (rosters.size() > 1) {
            throw exception0(BAD_REQUEST.getCode(),
                    "工号【" + dto.getUserCode() + "】存在多个车间身份，请使用完整用户名（车间编号_工号）操作");
        }
        SubSystemUsersDO roster = rosters.get(0);
        if (StrUtil.isNotBlank(roster.getWorkshopId())) {
            dto.setUserCode(roster.getWorkshopId().trim() + "_" + dto.getUserCode().trim());
        }
    }

    private SubSystemEmployeeApi getApi(Long subSystemId) {
        return subSystemEmployeeApiFactory.getApi(subSystemId);
    }

}
