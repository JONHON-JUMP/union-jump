package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeCreateFromUserReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeePageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEmployeeSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemEnabledSystemVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.framework.subsystemapi.ExternalApiException;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApi;
import cn.jonhon.jump.module.system.framework.subsystemapi.SubSystemEmployeeApiFactory;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_EMPLOYEE_API_ERROR;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.SUB_SYSTEM_EMPLOYEE_DEPT_NOT_MAPPED;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

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
    private AdminUserMapper adminUserMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
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
    @Transactional(rollbackFor = Exception.class)
    public void createFromMainUser(SubSystemEmployeeCreateFromUserReqVO reqVO) {
        AdminUserDO user = adminUserMapper.selectById(reqVO.getUserId());
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (user.getDeptId() == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_DEPT_NOT_MAPPED);
        }
        for (Long subSystemId : reqVO.getSubSystemIds()) {
            syncOneFromMainUser(user, subSystemId);
        }
    }

    /**
     * 对单个接口目标：始终调对方「新增人员」；
     * 仅当该系统已绑定 OAuth（JUMP 门户业务系统）时，再写入外部用户管理（sub_system_users）。
     */
    private void syncOneFromMainUser(AdminUserDO user, Long subSystemId) {
        SubSystemWorkshopSimpleRespVO workshop = subSystemWorkshopService
                .getWorkshopByDept(subSystemId, user.getDeptId());
        if (workshop == null || workshop.getWorkshopCode() == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_DEPT_NOT_MAPPED);
        }
        SubSystemEmployeeDTO dto = new SubSystemEmployeeDTO();
        dto.setUserCode(user.getUsername());
        dto.setUserName(user.getNickname());
        dto.setWorkshopCode(workshop.getWorkshopCode());
        dto.setDomainName(user.getDomainNo());
        if (CollUtil.isNotEmpty(user.getErpNos())) {
            dto.setErpNo(user.getErpNos().iterator().next());
        }
        dto.setCardNo(user.getCardNo());
        try {
            getApi(subSystemId).create(dto);
        } catch (ExternalApiException e) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, e.getMessage());
        }
        SubSystemDO subSystem = subSystemMapper.selectById(subSystemId);
        // 仅接口目标（如 Camstar人员管理，无 OAuth）不同步到 JUMP 外部用户管理
        if (subSystem == null || subSystem.getOauth2ClientId() == null) {
            return;
        }
        upsertPortalSubSystemUser(user, subSystemId, workshop.getWorkshopCode());
    }

    private void upsertPortalSubSystemUser(AdminUserDO user, Long subSystemId, String workshopCode) {
        SubSystemUsersDO existing = subSystemUsersMapper.selectBySubSystemIdAndUsername(
                subSystemId, user.getUsername());
        if (existing != null) {
            existing.setMainUserId(user.getId());
            existing.setNickname(user.getNickname());
            existing.setWorkshopId(workshopCode);
            existing.setStatus("0");
            existing.setRemark("用户创建同步业务系统");
            subSystemUsersMapper.updateById(existing);
            return;
        }
        SubSystemUsersDO subUser = new SubSystemUsersDO();
        subUser.setSubSystemId(subSystemId);
        subUser.setMainUserId(user.getId());
        subUser.setUsername(user.getUsername());
        subUser.setNickname(user.getNickname());
        subUser.setWorkshopId(workshopCode);
        subUser.setStatus("0");
        subUser.setRemark("用户创建同步业务系统");
        subSystemUsersMapper.insert(subUser);
    }

    @Override
    public List<SubSystemEnabledSystemVO> getEnabledSystems() {
        // 仅返回「新增人员」叶子已启用的系统；勾选同步时调对方 create；workshopCode=对照表部门编号
        List<Long> enabledIds = subSystemApiConfigService.getEnabledSubSystemIds();
        if (CollUtil.isEmpty(enabledIds)) {
            return Collections.emptyList();
        }
        List<SubSystemDO> subSystems = subSystemMapper.selectListByIds(enabledIds);
        if (CollUtil.isEmpty(subSystems)) {
            return Collections.emptyList();
        }
        return subSystems.stream().map(sys -> {
            SubSystemEnabledSystemVO vo = new SubSystemEnabledSystemVO();
            vo.setId(sys.getId());
            vo.setName(sys.getSystemName());
            vo.setPortalBound(sys.getOauth2ClientId() != null);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SubSystemWorkshopSimpleRespVO> getWorkshopOptions(Long subSystemId, Long deptId) {
        return subSystemWorkshopService.getWorkshopSimpleList(subSystemId, deptId);
    }

    private SubSystemEmployeeApi getApi(Long subSystemId) {
        return subSystemEmployeeApiFactory.getApi(subSystemId);
    }

}
