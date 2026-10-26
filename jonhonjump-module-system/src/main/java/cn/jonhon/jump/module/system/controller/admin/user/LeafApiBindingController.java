package cn.jonhon.jump.module.system.controller.admin.user;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingListRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiOptionRespVO;
import cn.jonhon.jump.module.system.service.user.LeafApiBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 业务叶子接口绑定")
@RestController
@RequestMapping("/system/sub-system-api-binding")
@Validated
public class LeafApiBindingController {

    private static final String READ = "@ss.hasPermission('sub-system:workshop:list')"
            + " or @ss.hasPermission('sub-system:workshop:update')"
            + " or @ss.hasPermission('sub-system:user:update')"
            + " or @ss.hasPermission('sub-system:role:update')"
            + " or @ss.hasPermission('system:user:update')";

    @Resource
    private LeafApiBindingService leafApiBindingService;

    @GetMapping("/list")
    @Operation(summary = "某个业务叶子已绑定的页面调用")
    @Parameter(name = "bizSubSystemId", description = "业务系统 ID", required = true)
    @PreAuthorize(READ)
    public CommonResult<LeafApiBindingListRespVO> list(@RequestParam("bizSubSystemId") Long bizSubSystemId,
                                                       @RequestParam(value = "workshopCode", required = false) String workshopCode) {
        return success(leafApiBindingService.getLeafBindings(bizSubSystemId, workshopCode));
    }

    @GetMapping("/options")
    @Operation(summary = "可选的接口系统及其具体接口")
    @PreAuthorize(READ)
    public CommonResult<List<LeafApiOptionRespVO>> options() {
        return success(leafApiBindingService.getApiOptions());
    }

    @PutMapping("/save")
    @Operation(summary = "保存业务叶子的接口绑定（一对多：每个页面调用一条）")
    @PreAuthorize("@ss.hasPermission('sub-system:workshop:update')")
    public CommonResult<Boolean> save(@Valid @RequestBody LeafApiBindingSaveReqVO reqVO) {
        leafApiBindingService.saveLeafBindings(reqVO);
        return success(true);
    }

}
