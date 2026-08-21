package cn.jonhon.jump.module.mes.process.controller.admin;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlRespVO;
import cn.jonhon.jump.module.mes.process.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.List;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

@Tag(name = "MES管理 - 工艺")
@RestController
@RequestMapping("/mes/process")
@Validated
@Slf4j
public class ProcessController {

    @Resource
    private ProcessService processService;

    @PostMapping("/query/card")
    @Operation(summary = "查看工艺卡片")
    @Parameter(name = "queryCard", description = "查看工艺卡片", required = true)
    public CommonResult<List<ProcessCardRespVO>> queryCard(@RequestBody @Valid ProcessCardReqVO reqVO) {
        return success(processService.queryCard(reqVO));
    }

    @PostMapping("/query/file-url")
    @Operation(summary = "获取MPM工艺文件地址")
    public CommonResult<ProcessFileUrlRespVO> queryFileUrl(
            @RequestBody @Valid ProcessFileUrlReqVO reqVO) {
        return success(processService.queryFileUrl(reqVO));
    }

}
