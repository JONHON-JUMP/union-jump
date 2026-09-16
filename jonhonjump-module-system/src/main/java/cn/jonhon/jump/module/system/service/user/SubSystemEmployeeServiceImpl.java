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
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApi;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.http.EndpointSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private AdminUserMapper adminUserMapper;
    @Resource
    private SubSystemWorkshopService subSystemWorkshopService;

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
        // 接口目标 = create 用途叶子已启用的接入系统（如 Camstar人员管理），与花名册系统解耦
        List<Long> apiSubSystemIds = configs.stream()
                .filter(this::isCreateEndpointEnabled)
                .map(SubSystemApiConfigDO::getSubSystemId)
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

    private boolean isCreateEndpointEnabled(SubSystemApiConfigDO config) {
        if (config == null || StrUtil.isBlank(config.getApiCreate())) {
            return false;
        }
        try {
            return EndpointSpec.parse(config.getApiCreate(), "新增接口").isEnabled();
        } catch (Exception e) {
            return false;
        }
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
        List<SubSystemUserRegisterRespVO> results = new ArrayList<>(reqVO.getIds().size());
        Long rosterSubSystemId = null;
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
            // 同一批须同属一个花名册系统（注册状态打在该系统名册行上）
            if (rosterSubSystemId == null) {
                rosterSubSystemId = roster.getSubSystemId();
            } else if (!rosterSubSystemId.equals(roster.getSubSystemId())) {
                throw exception0(BAD_REQUEST.getCode(), "所选用户分属不同业务系统，请分批注册");
            }
            if ("1".equals(roster.getEmployeeRegistered())) {
                result.setSuccess(true).setMessage("已注册，跳过推送");
                continue;
            }
            try {
                SubSystemEmployeeDTO dto = buildEmployeeDTO(roster, reqVO.getApiSubSystemId(), reqVO.getWorkshopCode(),
                        usernameWithWorkshop);
                if (StrUtil.isBlank(dto.getWorkshopCode())) {
                    result.setSuccess(false).setMessage("车间编码为空：请在注册时选择车间，或先在花名册维护该用户的车间（选业务系统不等于选车间）");
                    continue;
                }
                getApi(reqVO.getApiSubSystemId()).create(dto);
                markRegistered(roster.getId(), dto.getWorkshopCode(), usernameWithWorkshop,
                        resolveApiType(reqVO.getApiSubSystemId()));
                result.setSuccess(true);
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
        dto.setUserName(StrUtil.blankToDefault(roster.getNickname(),
                mainUser != null ? mainUser.getNickname() : null));
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

    private String resolveWorkshopCode(SubSystemUsersDO roster, Long apiSubSystemId, AdminUserDO mainUser,
                                       String overrideWorkshopCode) {
        if (StrUtil.isNotBlank(roster.getWorkshopId())) {
            return roster.getWorkshopId().trim();
        }
        if (StrUtil.isNotBlank(overrideWorkshopCode)) {
            return overrideWorkshopCode.trim();
        }
        if (mainUser != null && mainUser.getDeptId() != null) {
            SubSystemWorkshopSimpleRespVO workshop = subSystemWorkshopService
                    .getWorkshopByDept(roster.getSubSystemId(), mainUser.getDeptId());
            if (workshop != null && StrUtil.isNotBlank(workshop.getWorkshopCode())) {
                return workshop.getWorkshopCode();
            }
            workshop = subSystemWorkshopService.getWorkshopByDept(apiSubSystemId, mainUser.getDeptId());
            if (workshop != null && StrUtil.isNotBlank(workshop.getWorkshopCode())) {
                return workshop.getWorkshopCode();
            }
        }
        // 花名册系统本身带车间：MES4200 / mes4200 → 4200，或该系统只有一条车间对照
        return subSystemWorkshopService.inferWorkshopCode(roster.getSubSystemId());
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
