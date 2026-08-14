package cn.jonhon.jump.module.system.controller.admin.faq;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqRespVO;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.faq.FaqDO;
import cn.jonhon.jump.module.system.enums.faq.FaqStatusEnum;
import cn.jonhon.jump.module.system.service.faq.FaqService;
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

@Tag(name = "管理后台 - 常见 QA")
@RestController
@RequestMapping("/system/faq")
@Validated
public class FaqController {

    @Resource
    private FaqService faqService;

    @PostMapping("/create")
    @Operation(summary = "创建常见 QA")
    @PreAuthorize("@ss.hasPermission('system:faq:create')")
    public CommonResult<Long> createFaq(@Valid @RequestBody FaqSaveReqVO createReqVO) {
        return success(faqService.createFaq(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改常见 QA")
    @PreAuthorize("@ss.hasPermission('system:faq:update')")
    public CommonResult<Boolean> updateFaq(@Valid @RequestBody FaqSaveReqVO updateReqVO) {
        faqService.updateFaq(updateReqVO);
        return success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布常见 QA")
    @PreAuthorize("@ss.hasPermission('system:faq:update')")
    public CommonResult<Boolean> publishFaq(@RequestParam("id") Long id) {
        faqService.publishFaq(id);
        return success(true);
    }

    @PutMapping("/revoke")
    @Operation(summary = "撤回常见 QA")
    @PreAuthorize("@ss.hasPermission('system:faq:update')")
    public CommonResult<Boolean> revokeFaq(@RequestParam("id") Long id) {
        faqService.revokeFaq(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除常见 QA（业务软删除）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:faq:delete')")
    public CommonResult<Boolean> deleteFaq(@RequestParam("id") Long id) {
        faqService.deleteFaq(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除常见 QA")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:faq:delete')")
    public CommonResult<Boolean> deleteFaqList(@RequestParam("ids") List<Long> ids) {
        faqService.deleteFaqList(ids);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取常见 QA 分页（可按草稿/已发布/已删除筛选）")
    @PreAuthorize("@ss.hasPermission('system:faq:query')")
    public CommonResult<PageResult<FaqRespVO>> getFaqPage(@Validated FaqPageReqVO pageReqVO) {
        PageResult<FaqDO> pageResult = faqService.getFaqPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FaqRespVO.class));
    }

    @GetMapping("/workbench-page")
    @Operation(summary = "获得工作台常见 QA 分页", description = "普通用户仅可见已发布")
    public CommonResult<PageResult<FaqRespVO>> getFaqWorkbenchPage(@Validated FaqPageReqVO pageReqVO) {
        pageReqVO.setStatus(FaqStatusEnum.PUBLISHED.getStatus());
        PageResult<FaqDO> pageResult = faqService.getFaqPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FaqRespVO.class));
    }

    @GetMapping("/app-get")
    @Operation(summary = "获得常见 QA 详情", description = "供工作台查看已发布的常见 QA")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<FaqRespVO> getAppFaq(@RequestParam("id") Long id) {
        FaqDO faq = faqService.getPublishedFaq(id);
        return success(BeanUtils.toBean(faq, FaqRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得常见 QA")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:faq:query')")
    public CommonResult<FaqRespVO> getFaq(@RequestParam("id") Long id) {
        FaqDO faq = faqService.getFaq(id);
        return success(BeanUtils.toBean(faq, FaqRespVO.class));
    }

}
