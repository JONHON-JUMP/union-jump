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
            getApi(createReqVO.getSubSystemId())
                    .create(BeanUtils.toBean(createReqVO, SubSystemEmployeeDTO.class));
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
    }

    @Override
    public void updateEmployee(SubSystemEmployeeSaveReqVO updateReqVO) {
        try {
            getApi(updateReqVO.getSubSystemId())
                    .update(BeanUtils.toBean(updateReqVO, SubSystemEmployeeDTO.class));
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
                getApi(reqVO.getApiSubSystemId()).create(buildEmployeeDTO(roster, reqVO.getApiSubSystemId()));
                markRegistered(roster.getId());
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

    /** 按接口目标的车间对照（JUMP 部门 → 接口目标车间）解析；解析不到退回花名册现值 */
    private SubSystemEmployeeDTO buildEmployeeDTO(SubSystemUsersDO roster, Long apiSubSystemId) {
        AdminUserDO mainUser = roster.getMainUserId() == null
                ? null : adminUserMapper.selectById(roster.getMainUserId());
        SubSystemEmployeeDTO dto = new SubSystemEmployeeDTO();
        dto.setUserCode(roster.getUsername());
        dto.setUserName(StrUtil.blankToDefault(roster.getNickname(),
                mainUser != null ? mainUser.getNickname() : null));
        dto.setWorkshopCode(resolveWorkshopCode(roster, apiSubSystemId, mainUser));
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

    private String resolveWorkshopCode(SubSystemUsersDO roster, Long apiSubSystemId, AdminUserDO mainUser) {
        if (mainUser != null && mainUser.getDeptId() != null) {
            SubSystemWorkshopSimpleRespVO workshop = subSystemWorkshopService
                    .getWorkshopByDept(apiSubSystemId, mainUser.getDeptId());
            if (workshop != null && StrUtil.isNotBlank(workshop.getWorkshopCode())) {
                return workshop.getWorkshopCode();
            }
        }
        return roster.getWorkshopId();
    }

    private void markRegistered(Long rosterId) {
        SubSystemUsersDO updateObj = new SubSystemUsersDO();
        updateObj.setId(rosterId);
        updateObj.setEmployeeRegistered("1");
        subSystemUsersMapper.updateById(updateObj);
    }

    private SubSystemEmployeeApi getApi(Long subSystemId) {
        return subSystemEmployeeApiFactory.getApi(subSystemId);
    }

}
