package cn.jonhon.jump.module.mes.process.controller.admin;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
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

    @GetMapping("/query/document-pdf")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "预览MPM的010工艺PDF")
    public ResponseEntity<byte[]> queryDocumentPdf(@RequestParam("accno") String accno) {
        byte[] pdf = processService.queryDocumentPdf(accno);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=process.pdf")
                .header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }

    @PostMapping("/query/card")
    @PermitAll
    @TenantIgnore // 独立公共查询页，工艺数据来自共享外部系统
    @Operation(summary = "查看工艺卡片")
    @Parameter(name = "queryCard", description = "查看工艺卡片", required = true)
    public CommonResult<List<ProcessCardRespVO>> queryCard(@RequestBody @Valid ProcessCardReqVO reqVO) {
        return success(processService.queryCard(reqVO));
    }

    @PostMapping("/query/file-url")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "获取MPM工艺文件地址")
    public CommonResult<ProcessFileUrlRespVO> queryFileUrl(
            @RequestBody @Valid ProcessFileUrlReqVO reqVO) {
        return success(processService.queryFileUrl(reqVO));
    }

}
