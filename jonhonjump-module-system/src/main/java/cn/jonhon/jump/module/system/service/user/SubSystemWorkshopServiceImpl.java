package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemWorkshopSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import cn.jonhon.jump.module.system.dal.mysql.dept.DeptMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemUsersMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemWorkshopMapper;
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

/**
 * 外部系统车间 Service 实现（JUMP 部门 ↔ 子系统车间映射）
 */
@Service
@Validated
public class SubSystemWorkshopServiceImpl implements SubSystemWorkshopService {

    @Resource
    private SubSystemWorkshopMapper subSystemWorkshopMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemUsersMapper subSystemUsersMapper;
    @Resource
    private DeptMapper deptMapper;

    @Override
    public PageResult<SubSystemWorkshopRespVO> getSubSystemWorkshopPage(SubSystemWorkshopPageReqVO pageReqVO) {
        if (pageReqVO.getSubSystemId() != null) {
            validateSubSystemExists(pageReqVO.getSubSystemId());
        }
        PageResult<SubSystemWorkshopDO> pageResult = subSystemWorkshopMapper.selectPage(pageReqVO);
        return new PageResult<>(buildRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public SubSystemWorkshopRespVO getSubSystemWorkshop(Long id) {
        SubSystemWorkshopDO workshop = validateSubSystemWorkshopExists(id);
        return buildResp(workshop);
    }

    @Override
    public Long createSubSystemWorkshop(SubSystemWorkshopSaveReqVO createReqVO) {
        validateSubSystemExists(createReqVO.getSubSystemId());
        validateDept(createReqVO.getDeptId());
        validateWorkshopDuplicate(createReqVO.getSubSystemId(), createReqVO.getWorkshopName(),
                createReqVO.getWorkshopCode(), null);

        SubSystemWorkshopDO workshop = BeanUtils.toBean(createReqVO, SubSystemWorkshopDO.class);
        subSystemWorkshopMapper.insert(workshop);
        return workshop.getId();
    }

    @Override
    public void updateSubSystemWorkshop(SubSystemWorkshopSaveReqVO updateReqVO) {
        SubSystemWorkshopDO workshop = validateSubSystemWorkshopExists(updateReqVO.getId());
        validateSubSystemExists(updateReqVO.getSubSystemId());
        validateDept(updateReqVO.getDeptId());
        validateWorkshopDuplicate(updateReqVO.getSubSystemId(), updateReqVO.getWorkshopName(),
                updateReqVO.getWorkshopCode(), updateReqVO.getId());

        SubSystemWorkshopDO updateObj = BeanUtils.toBean(updateReqVO, SubSystemWorkshopDO.class);
        updateObj.setSubSystemId(workshop.getSubSystemId());
        subSystemWorkshopMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemWorkshop(Long id) {
        SubSystemWorkshopDO workshop = validateSubSystemWorkshopExists(id);
        validateWorkshopNotAssigned(workshop);
        subSystemWorkshopMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubSystemWorkshopList(List<Long> ids) {
        ids.forEach(id -> {
            SubSystemWorkshopDO workshop = validateSubSystemWorkshopExists(id);
            validateWorkshopNotAssigned(workshop);
        });
        subSystemWorkshopMapper.deleteByIds(ids);
    }

    @Override
    public List<SubSystemWorkshopSimpleRespVO> getWorkshopSimpleList(Long subSystemId, Long deptId) {
        List<SubSystemWorkshopDO> list = Collections.emptyList();
        if (subSystemId != null) {
            if (subSystemMapper.selectById(subSystemId) == null) {
                throw exception(SUB_SYSTEM_NOT_EXISTS);
            }
            if (deptId != null) {
                list = subSystemWorkshopMapper.selectListBySubSystemIdAndDeptId(subSystemId, deptId);
            }
            if (CollUtil.isEmpty(list)) {
                list = subSystemWorkshopMapper.selectListBySubSystemId(subSystemId);
            }
        }
        if (CollUtil.isEmpty(list) && deptId != null) {
            list = subSystemWorkshopMapper.selectListByDeptId(deptId);
        }
        if (CollUtil.isEmpty(list)) {
            list = subSystemWorkshopMapper.selectList();
        }
        return distinctByCode(list);
    }

    private List<SubSystemWorkshopSimpleRespVO> distinctByCode(List<SubSystemWorkshopDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .filter(item -> item.getWorkshopCode() != null)
                .collect(Collectors.toMap(SubSystemWorkshopDO::getWorkshopCode, this::buildSimple, (a, b) -> a,
                        java.util.LinkedHashMap::new))
                .values().stream().collect(Collectors.toList());
    }

    @Override
    public SubSystemWorkshopSimpleRespVO getWorkshopByDept(Long subSystemId, Long deptId) {
        if (deptId == null) {
            return null;
        }
        // 先按 系统+部门；车间页常静默绑默认系统，再按部门兜底
        if (subSystemId != null) {
            List<SubSystemWorkshopDO> list = subSystemWorkshopMapper
                    .selectListBySubSystemIdAndDeptId(subSystemId, deptId);
            if (CollUtil.isNotEmpty(list)) {
                return buildSimple(list.get(0));
            }
        }
        List<SubSystemWorkshopDO> byDept = subSystemWorkshopMapper.selectListByDeptId(deptId);
        return CollUtil.isEmpty(byDept) ? null : buildSimple(byDept.get(0));
    }

    // ===================== 私有方法 =====================

    private SubSystemWorkshopSimpleRespVO buildSimple(SubSystemWorkshopDO workshop) {
        SubSystemWorkshopSimpleRespVO vo = new SubSystemWorkshopSimpleRespVO();
        vo.setId(workshop.getId());
        vo.setWorkshopCode(workshop.getWorkshopCode());
        vo.setWorkshopName(workshop.getWorkshopName());
        return vo;
    }

    private List<SubSystemWorkshopRespVO> buildRespList(List<SubSystemWorkshopDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> subSystemMap = convertMap(
                subSystemMapper.selectListByIds(convertSet(list, SubSystemWorkshopDO::getSubSystemId)),
                SubSystemDO::getId);
        Map<Long, DeptDO> deptMap = convertMap(
                deptMapper.selectBatchIds(convertSet(list, SubSystemWorkshopDO::getDeptId)),
                DeptDO::getId);
        return list.stream().map(workshop -> {
            SubSystemWorkshopRespVO vo = BeanUtils.toBean(workshop, SubSystemWorkshopRespVO.class);
            SubSystemDO subSystem = subSystemMap.get(workshop.getSubSystemId());
            if (subSystem != null) {
                vo.setClientName(subSystem.getSystemName());
            }
            DeptDO dept = deptMap.get(workshop.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private SubSystemWorkshopRespVO buildResp(SubSystemWorkshopDO workshop) {
        List<SubSystemWorkshopRespVO> list = buildRespList(Collections.singletonList(workshop));
        return list.isEmpty() ? BeanUtils.toBean(workshop, SubSystemWorkshopRespVO.class) : list.get(0);
    }

    private SubSystemWorkshopDO validateSubSystemWorkshopExists(Long id) {
        SubSystemWorkshopDO workshop = subSystemWorkshopMapper.selectById(id);
        if (workshop == null) {
            throw exception(SUB_SYSTEM_WORKSHOP_NOT_EXISTS);
        }
        return workshop;
    }

    private void validateSubSystemExists(Long subSystemId) {
        if (subSystemMapper.selectById(subSystemId) == null) {
            throw exception(SUB_SYSTEM_NOT_EXISTS);
        }
    }

    private void validateDept(Long deptId) {
        if (deptId != null && deptMapper.selectById(deptId) == null) {
            throw exception(SUB_SYSTEM_WORKSHOP_DEPT_INVALID);
        }
    }

    private void validateWorkshopDuplicate(Long subSystemId, String workshopName, String workshopCode, Long excludeId) {
        SubSystemWorkshopDO byCode = subSystemWorkshopMapper.selectBySubSystemIdAndWorkshopCode(subSystemId, workshopCode);
        if (byCode != null && ObjectUtil.notEqual(byCode.getId(), excludeId)) {
            throw exception(SUB_SYSTEM_WORKSHOP_CODE_DUPLICATE, workshopCode);
        }
        SubSystemWorkshopDO byName = subSystemWorkshopMapper.selectBySubSystemIdAndWorkshopName(subSystemId, workshopName);
        if (byName != null && ObjectUtil.notEqual(byName.getId(), excludeId)) {
            throw exception(SUB_SYSTEM_WORKSHOP_NAME_DUPLICATE, workshopName);
        }
    }

    private void validateWorkshopNotAssigned(SubSystemWorkshopDO workshop) {
        Long count = subSystemUsersMapper.selectCountBySubSystemIdAndWorkshopId(
                workshop.getSubSystemId(), workshop.getWorkshopCode());
        if (count != null && count > 0) {
            throw exception(SUB_SYSTEM_WORKSHOP_HAS_USERS);
        }
    }

}
