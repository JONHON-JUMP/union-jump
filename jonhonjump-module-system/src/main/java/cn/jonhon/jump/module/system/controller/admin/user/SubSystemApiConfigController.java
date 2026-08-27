package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestRespVO;
import cn.jonhon.jump.module.system.service.user.SubSystemApiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 子系统人员接口配置")
@RestController
@RequestMapping("/system/sub-system-api-config")
@Validated
public class SubSystemApiConfigController {

    @Resource
    private SubSystemApiConfigService subSystemApiConfigService;

    @GetMapping("/list")
    @Operation(summary = "获得子系统人员接口配置列表")
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:list')")
    public CommonResult<List<SubSystemApiConfigRespVO>> getApiConfigList() {
        return success(subSystemApiConfigService.getApiConfigList());
    }

    @GetMapping("/get")
    @Operation(summary = "获得子系统人员接口配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:list')")
    public CommonResult<SubSystemApiConfigRespVO> getApiConfig(@RequestParam("id") Long id) {
        return success(subSystemApiConfigService.getApiConfig(id));
    }

    @GetMapping("/get-by-sub-system")
    @Operation(summary = "按外部系统获得接口配置（页面左侧切换系统用）")
    @Parameter(name = "subSystemId", description = "外部系统编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:list')")
    public CommonResult<SubSystemApiConfigRespVO> getBySubSystem(@RequestParam("subSystemId") Long subSystemId) {
        List<SubSystemApiConfigRespVO> list = subSystemApiConfigService.getApiConfigList();
        return success(list.stream()
                .filter(c -> c.getSubSystemId().equals(subSystemId))
                .findFirst().orElse(null));
    }

    @PostMapping("/create")
    @Operation(summary = "创建子系统人员接口配置")
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:create')")
    public CommonResult<Long> createApiConfig(@Valid @RequestBody SubSystemApiConfigSaveReqVO createReqVO) {
        return success(subSystemApiConfigService.createApiConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新子系统人员接口配置")
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:update')")
    public CommonResult<Boolean> updateApiConfig(@Valid @RequestBody SubSystemApiConfigSaveReqVO updateReqVO) {
        subSystemApiConfigService.updateApiConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除子系统人员接口配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:delete')")
    public CommonResult<Boolean> deleteApiConfig(@RequestParam("id") Long id) {
        subSystemApiConfigService.deleteApiConfig(id);
        return success(true);
    }

    @GetMapping("/test-connection")
    @Operation(summary = "测试接口连通性（调一次查询接口）")
    @Parameter(name = "id", description = "配置编号", required = true)
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:list')")
    public CommonResult<String> testConnection(@RequestParam("id") Long id) {
        return success(subSystemApiConfigService.testConnection(id));
    }

    @PostMapping("/test")
    @Operation(summary = "本页调试指定人员接口（HTTP）")
    @PreAuthorize("@ss.hasPermission('sub-system:apiconfig:list')")
    public CommonResult<SubSystemApiTestRespVO> testInvoke(@Valid @RequestBody SubSystemApiTestReqVO reqVO) {
        return success(subSystemApiConfigService.testInvoke(reqVO));
    }

}
