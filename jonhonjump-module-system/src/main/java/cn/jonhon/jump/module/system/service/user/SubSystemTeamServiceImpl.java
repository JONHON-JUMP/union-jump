package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemTeamSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemUsersSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemTeamDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemUsersDO;
import cn.jonhon.jump.module.system.dal.mysql.dept.DeptMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemTeamMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SubSystemTeamServiceImpl implements SubSystemTeamService {

    @Resource
    private SubSystemTeamMapper subSystemTeamMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private SubSystemAccessService subSystemAccessService;
    @Resource
    private LeafApiBindingService leafApiBindingService;
    @Resource
    private SubSystemApiConfigService subSystemApiConfigService;

    @Override
    public PageResult<SubSystemTeamRespVO> getSubSystemTeamPage(SubSystemTeamPageReqVO pageReqVO) {
        // 可管系统范围：受限时强制只查授权系统（null=不受限）
        java.util.Set<Long> allowedSubSystemIds = subSystemAccessService.getAllowedSubSystemIds(
                cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        } else if (allowedSubSystemIds != null && allowedSubSystemIds.isEmpty()) {
            return new PageResult<>(java.util.Collections.emptyList(), 0L);
        }
        PageResult<SubSystemTeamDO> pageResult = subSystemTeamMapper.selectPage(pageReqVO, allowedSubSystemIds);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SubSystemTeamRespVO getSubSystemTeam(Long id) {
        SubSystemTeamDO team = validateSubSystemTeamExists(id);
        return buildResp(team);
    }

    @Override
    public Long createSubSystemTeam(SubSystemTeamSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validateDeptIfPresent(createReqVO.getDeptId());
        createReqVO.setWorkshopCode(requireWorkshopCode(createReqVO.getWorkshopCode()));
        createReqVO.setTeamName(StrUtil.trim(createReqVO.getTeamName()));
        createReqVO.setTeamCode(null);
        fillTeamLeaderName(createReqVO);
        if (StrUtil.isBlank(createReqVO.getTeamName())) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "班组名称不能为空");
        }
        // 勾选同步：对方新增后按班组名称查出班组编码，再写入本地。不勾选时本地编码先用班组名称。
        if (Boolean.TRUE.equals(createReqVO.getSyncToExternal())) {
            String workshopCode = createReqVO.getWorkshopCode();
            // 查询、新增各自走该车间叶子上的绑定，不写死接口系统
            Long queryApiId = leafApiBindingService.requireApiSubSystemId(
                    createReqVO.getSubSystemId(), workshopCode, LeafApiSceneEnum.TEAM_QUERY);
            Long createApiId = leafApiBindingService.requireApiSubSystemId(
                    createReqVO.getSubSystemId(), workshopCode, LeafApiSceneEnum.TEAM_CREATE);
            // 对方落库的编码就是本次名称。先按这个编码查重，避免新增接口改掉已有班组。
            String externalCode = createReqVO.getTeamName();
            validateTeamDuplicate(createReqVO.getSubSystemId(), createReqVO.getTeamName(), externalCode, null);
            assertExternalTeamCodeFree(queryApiId, workshopCode, externalCode);
            subSystemApiConfigService.pushExternalTeamCreate(
                    createApiId, workshopCode, externalCode, createReqVO.getTeamName());
            createReqVO.setTeamCode(requireExternalTeamCode(queryApiId, workshopCode, externalCode));
        } else {
            createReqVO.setTeamCode(createReqVO.getTeamName());
            validateTeamDuplicate(createReqVO.getSubSystemId(), createReqVO.getTeamName(),
                    createReqVO.getTeamCode(), null);
        }

        SubSystemTeamDO team = BeanUtils.toBean(createReqVO, SubSystemTeamDO.class);
        subSystemTeamMapper.insert(team);
        return team.getId();
    }

    @Override
    public void updateSubSystemTeam(SubSystemTeamSaveReqVO updateReqVO) {
        SubSystemTeamDO team = validateSubSystemTeamExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDeptIfPresent(updateReqVO.getDeptId());
        updateReqVO.setWorkshopCode(requireWorkshopCode(updateReqVO.getWorkshopCode()));
        if (StrUtil.isBlank(updateReqVO.getTeamCode())) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "班组编码不能为空");
        }
        validateTeamDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getTeamName(),
                updateReqVO.getTeamCode(), updateReqVO.getId());
        fillTeamLeaderName(updateReqVO);
        // 修改只写 JUMP，不推业务系统

        SubSystemTeamDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemTeamDO.class);
        // 系统归属不变；部门、车间编号写在班组自身
        updateObj.setSubSystemId(team.getSubSystemId());
        subSystemTeamMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemTeam(Long id) {
        SubSystemTeamDO team = validateSubSystemTeamExists(id);
        validateTeamNotAssigned(team);
        subSystemTeamMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemTeamList(List<Long> ids) {
        ids.forEach(id -> {
            SubSystemTeamDO team = validateSubSystemTeamExists(id);
            validateTeamNotAssigned(team);
        });
        subSystemTeamMapper.deleteByIds(ids);
    }

    @Override
    public List<SubSystemUsersSimpleRespVO> getUserSimpleList(Long subSystemId) {
        validateSubSystemExists(subSystemId);
        List<SubSystemUsersDO> users = subSystemUsersMapper.selectListBySubSystemId(subSystemId);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyList();
        }
        Map<Long, AdminUserDO> mainUserMap = convertMap(
                adminUserMapper.selectList(AdminUserDO::getId, convertSet(users, SubSystemUsersDO::getMainUserId)),
                AdminUserDO::getId);
        return users.stream().map(user -> {
            SubSystemUsersSimpleRespVO vo = new SubSystemUsersSimpleRespVO();
            vo.setId(user.getId());
            AdminUserDO mainUser = mainUserMap.get(user.getMainUserId());
            if (mainUser != null) {
                vo.setNickname(mainUser.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SubSystemTeamRespVO> buildRespList(List<SubSystemTeamDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemTeamDO::getSubSystemId)),
                SubSystemDO::getId);
        java.util.Set<Long> deptIds = list.stream()
                .map(SubSystemTeamDO::getDeptId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptDO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap()
                : convertMap(deptMapper.selectBatchIds(deptIds), DeptDO::getId);
        return list.stream().map(team -> {
            SubSystemTeamRespVO vo = BeanUtils.toBean(team, SubSystemTeamRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(team.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            DeptDO dept = deptMap.get(team.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            // 车间编号取班组自身字段，不再从车间对照兜底
            vo.setWorkshopCode(team.getWorkshopCode());
            return vo;
        }).collect(Collectors.toList());
    }

    private SubSystemTeamRespVO buildResp(SubSystemTeamDO team) {
        List<SubSystemTeamRespVO> list = buildRespList(Collections.singletonList(team));
        return list.isEmpty() ? BeanUtils.toBean(team, SubSystemTeamRespVO.class) : list.get(0);
    }

    private void fillTeamLeaderName(SubSystemTeamSaveReqVO reqVO) {
        if (reqVO.getTeamLeaderId() == null) {
            reqVO.setTeamLeaderName(null);
            return;
        }
        SubSystemUsersDO user = subSystemUsersMapper.selectById(reqVO.getTeamLeaderId());
        if (user == null || !ObjectUtil.equal(user.getSubSystemId(), reqVO.getSubSystemId())) {
            throw exception(SUB_SYSTEM_TEAM_LEADER_INVALID);
        }
        AdminUserDO mainUser = adminUserMapper.selectById(user.getMainUserId());
        reqVO.setTeamLeaderName(mainUser != null ? mainUser.getNickname() : null);
    }

    /** 部门可选；有值时只校验部门存在，不再要求已做车间对照 */
    private void validateDeptIfPresent(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (deptMapper.selectById(deptId) == null) {
            throw exception(SUB_SYSTEM_WORKSHOP_DEPT_INVALID);
        }
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

    private SubSystemTeamDO validateSubSystemTeamExists(Long id) {
        SubSystemTeamDO team = subSystemTeamMapper.selectById(id);
        if (team == null) {
            throw exception(SUB_SYSTEM_TEAM_NOT_EXISTS);
        }
        // 可管系统范围校验：受限角色只能操作授权系统的班组
        subSystemAccessService.checkAccessible(team.getSubSystemId());
        return team;
    }

    private void validateTeamNotAssigned(SubSystemTeamDO team) {
        Long count = subSystemUsersMapper.selectCountBySubSystemIdAndTeamId(
                team.getSubSystemId(), team.getTeamCode());
        if (count != null && count > 0) {
            throw exception(SUB_SYSTEM_TEAM_HAS_USERS);
        }
    }

    /** 车间编号必填 */
    private String requireWorkshopCode(String workshopCode) {
        String code = StrUtil.trimToNull(workshopCode);
        if (code == null) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR, "车间编号不能为空");
        }
        return code;
    }

    /**
     * 新增成功后按车间 + 班组编码再查一次。编码用的是刚提交的班组名称，因为对方把名称写进了 TEAM.teamId。
     * 查不到就不写本地。
     */
    private String requireExternalTeamCode(Long queryApiSubSystemId, String workshopCode, String teamName) {
        String code = StrUtil.trim(teamName);
        Map<String, String> existing = subSystemApiConfigService
                .queryExternalTeams(queryApiSubSystemId, workshopCode, code);
        if (!existing.containsKey(code)) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR,
                    "业务系统已新增班组，但未查到班组编码【" + code + "】，JUMP 未保存。请不要再次新建。");
        }
        return code;
    }

    /** 新增同步前必须查到叶子上绑定的班组查询。对方已有同编码则不再新增，避免 upsert 改掉已有班组。 */
    private void assertExternalTeamCodeFree(Long queryApiSubSystemId, String workshopCode, String teamCode) {
        Map<String, String> existing = subSystemApiConfigService
                .queryExternalTeams(queryApiSubSystemId, workshopCode, teamCode);
        if (existing.containsKey(StrUtil.trim(teamCode))) {
            throw exception(SUB_SYSTEM_EMPLOYEE_API_ERROR,
                    "业务系统已存在班组编码【" + StrUtil.trim(teamCode) + "】，不再同步新增");
        }
    }

    private void validateTeamDuplicate(Long subSystemId, String teamName, String teamCode, Long id) {
        SubSystemTeamDO team = subSystemTeamMapper.selectBySubSystemIdAndTeamName(subSystemId, teamName);
        if (team != null && !ObjectUtil.equal(team.getId(), id)) {
            throw exception(SUB_SYSTEM_TEAM_NAME_DUPLICATE, teamName);
        }
        team = subSystemTeamMapper.selectBySubSystemIdAndTeamCode(subSystemId, teamCode);
        if (team != null && !ObjectUtil.equal(team.getId(), id)) {
            throw exception(SUB_SYSTEM_TEAM_CODE_DUPLICATE, teamCode);
        }
    }

}
