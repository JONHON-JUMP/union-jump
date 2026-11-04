package cn.jonhon.jump.module.system.controller.admin.dept;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.datapermission.core.util.DataPermissionUtils;
import cn.jonhon.jump.framework.security.core.util.SecurityFrameworkUtils;
import cn.jonhon.jump.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.jonhon.jump.module.system.controller.admin.dept.vo.dept.DeptRespVO;
import cn.jonhon.jump.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.dept.DeptDO;
import cn.jonhon.jump.module.system.enums.permission.RoleCodeEnum;
import cn.jonhon.jump.module.system.service.dept.DeptService;
import cn.jonhon.jump.module.system.service.permission.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部门")
@RestController
@RequestMapping("/system/dept")
@Validated
public class DeptController {

    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;

    @PostMapping("create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptSaveReqVO createReqVO) {
        Long deptId = deptService.createDept(createReqVO);
        return success(deptId);
    }

    @PutMapping("update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptSaveReqVO updateReqVO) {
        deptService.updateDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除部门")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDeptList(@RequestParam("ids") List<Long> ids) {
        deptService.deleteDeptList(ids);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptRespVO>> getDeptList(DeptListReqVO reqVO) {
        List<DeptDO> list = deptService.getDeptList(reqVO);
        return success(BeanUtils.toBean(list, DeptRespVO.class));
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取部门精简信息列表", description = "只包含被开启的部门，按当前用户数据权限过滤")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptList() {
        // 先忽略数据权限取全量，用于补齐父级节点；最后再按当前用户部门范围过滤
        List<DeptDO> all = DataPermissionUtils.executeIgnore(() -> deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus())));
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            return success(BeanUtils.toBean(all, DeptSimpleRespVO.class));
        }
        // 超管/租户管理员不受部门数据权限限制
        if (permissionService.hasAnyRoles(loginUserId,
                RoleCodeEnum.SUPER_ADMIN.getCode(), RoleCodeEnum.TENANT_ADMIN.getCode())) {
            return success(BeanUtils.toBean(all, DeptSimpleRespVO.class));
        }
        DeptDataPermissionRespDTO permission = permissionService.getDeptDataPermission(loginUserId);
        if (Boolean.TRUE.equals(permission.getAll())) {
            return success(BeanUtils.toBean(all, DeptSimpleRespVO.class));
        }
        Set<Long> allowed = permission.getDeptIds() == null
                ? new HashSet<>() : new HashSet<>(permission.getDeptIds());
        Map<Long, DeptDO> byId = new HashMap<>();
        all.forEach(dept -> byId.put(dept.getId(), dept));
        // “部门权限及以下部门”：允许选择权限部门的所有下级
        Set<Long> assignable = new HashSet<>(allowed);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (DeptDO dept : all) {
                if (dept.getParentId() != null && assignable.contains(dept.getParentId())
                        && assignable.add(dept.getId())) {
                    changed = true;
                }
            }
        }
        // 目标部门可能没有父级权限：把祖先节点一并带出来用于树结构，但标记 disabled
        Set<Long> include = new HashSet<>(assignable);
        for (Long deptId : assignable) {
            DeptDO current = byId.get(deptId);
            int depth = 0;
            while (current != null && current.getParentId() != null
                    && current.getParentId() != 0 && depth++ < 64) {
                include.add(current.getParentId());
                current = byId.get(current.getParentId());
            }
        }
        List<DeptSimpleRespVO> result = new ArrayList<>(include.size());
        for (DeptDO dept : all) {
            if (!include.contains(dept.getId())) {
                continue;
            }
            DeptSimpleRespVO vo = BeanUtils.toBean(dept, DeptSimpleRespVO.class);
            // 祖先节点只用于展示层级，不能选；权限部门及其下级才可选
            vo.setDisabled(!assignable.contains(dept.getId()));
            result.add(vo);
        }
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptRespVO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        return success(BeanUtils.toBean(dept, DeptRespVO.class));
    }

}
