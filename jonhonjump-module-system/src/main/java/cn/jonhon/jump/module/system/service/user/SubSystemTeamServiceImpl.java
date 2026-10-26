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
        createReqVO.setWorkshopCode(StrUtil.trimToNull(createReqVO.getWorkshopCode()));
        validateTeamDuplicate(createReqVO.getSubSystemId(), createReqVO.getTeamName(),
                createReqVO.getTeamCode(), null);
        fillTeamLeaderName(createReqVO);

        SubSystemTeamDO team = BeanUtils.toBean(createReqVO, SubSystemTeamDO.class);
        subSystemTeamMapper.insert(team);
        return team.getId();
    }

    @Override
    public void updateSubSystemTeam(SubSystemTeamSaveReqVO updateReqVO) {
        SubSystemTeamDO team = validateSubSystemTeamExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDeptIfPresent(updateReqVO.getDeptId());
        updateReqVO.setWorkshopCode(StrUtil.trimToNull(updateReqVO.getWorkshopCode()));
        validateTeamDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getTeamName(),
                updateReqVO.getTeamCode(), updateReqVO.getId());
        fillTeamLeaderName(updateReqVO);

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
