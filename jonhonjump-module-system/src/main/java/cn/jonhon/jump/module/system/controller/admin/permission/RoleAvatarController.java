package cn.jonhon.jump.module.system.controller.admin.permission;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarRespVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.roleavatar.RoleAvatarSimpleRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleAvatarDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.RoleDO;
import cn.jonhon.jump.module.system.dal.mysql.permission.RoleMapper;
import cn.jonhon.jump.module.system.service.permission.RoleAvatarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;

@Tag(name = "管理后台 - 角色头像")
@RestController
@RequestMapping("/system/role-avatar")
@Validated
public class RoleAvatarController {

    @Resource
    private RoleAvatarService roleAvatarService;
    @Resource
    private RoleMapper roleMapper;

    @PostMapping("/create")
    @Operation(summary = "创建角色头像")
    @PreAuthorize("@ss.hasPermission('system:role-avatar:create')")
    public CommonResult<Long> createRoleAvatar(@Valid @RequestBody RoleAvatarSaveReqVO createReqVO) {
        return success(roleAvatarService.createRoleAvatar(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改角色头像")
    @PreAuthorize("@ss.hasPermission('system:role-avatar:update')")
    public CommonResult<Boolean> updateRoleAvatar(@Valid @RequestBody RoleAvatarSaveReqVO updateReqVO) {
        roleAvatarService.updateRoleAvatar(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色头像")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:role-avatar:delete')")
    public CommonResult<Boolean> deleteRoleAvatar(@RequestParam("id") Long id) {
        roleAvatarService.deleteRoleAvatar(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除角色头像")
    @PreAuthorize("@ss.hasPermission('system:role-avatar:delete')")
    public CommonResult<Boolean> deleteRoleAvatarList(@RequestParam("ids") List<Long> ids) {
        roleAvatarService.deleteRoleAvatarList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得角色头像")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:role-avatar:query')")
    public CommonResult<RoleAvatarRespVO> getRoleAvatar(@RequestParam("id") Long id) {
        RoleAvatarDO roleAvatar = roleAvatarService.getRoleAvatar(id);
        RoleAvatarRespVO respVO = BeanUtils.toBean(roleAvatar, RoleAvatarRespVO.class);
        fillRoleName(respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得角色头像分页")
    @PreAuthorize("@ss.hasPermission('system:role-avatar:query')")
    public CommonResult<PageResult<RoleAvatarRespVO>> getRoleAvatarPage(@Valid RoleAvatarPageReqVO pageReqVO) {
        PageResult<RoleAvatarDO> pageResult = roleAvatarService.getRoleAvatarPage(pageReqVO);
        PageResult<RoleAvatarRespVO> result = BeanUtils.toBean(pageResult, RoleAvatarRespVO.class);
        fillRoleNames(result.getList());
        return success(result);
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用的角色头像精简列表")
    public CommonResult<List<RoleAvatarSimpleRespVO>> getRoleAvatarSimpleList() {
        List<RoleAvatarDO> list = roleAvatarService.getRoleAvatarSimpleList();
        List<RoleAvatarSimpleRespVO> result = BeanUtils.toBean(list, RoleAvatarSimpleRespVO.class);
        fillSimpleRoleNames(result);
        return success(result);
    }

    private void fillRoleNames(List<RoleAvatarRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, RoleDO> roleMap = convertMap(
                roleMapper.selectList(RoleDO::getCode, list.stream().map(RoleAvatarRespVO::getRoleCode).collect(Collectors.toSet())),
                RoleDO::getCode);
        list.forEach(item -> {
            RoleDO role = roleMap.get(item.getRoleCode());
            if (role != null) {
                item.setRoleName(role.getName());
            }
        });
    }

    private void fillRoleName(RoleAvatarRespVO respVO) {
        if (respVO == null) {
            return;
        }
        RoleDO role = roleMapper.selectByCode(respVO.getRoleCode());
        if (role != null) {
            respVO.setRoleName(role.getName());
        }
    }

    private void fillSimpleRoleNames(List<RoleAvatarSimpleRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, RoleDO> roleMap = convertMap(
                roleMapper.selectList(RoleDO::getCode, list.stream().map(RoleAvatarSimpleRespVO::getRoleCode).collect(Collectors.toSet())),
                RoleDO::getCode);
        list.forEach(item -> {
            RoleDO role = roleMap.get(item.getRoleCode());
            if (role != null) {
                item.setRoleName(role.getName());
            }
        });
    }

}
