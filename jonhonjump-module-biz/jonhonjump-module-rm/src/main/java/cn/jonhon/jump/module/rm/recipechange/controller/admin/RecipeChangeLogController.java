package cn.jonhon.jump.module.rm.recipechange.controller.admin;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageParam;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.excel.core.util.ExcelUtils;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.*;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeLogService;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

/**
 * 工艺变更通知管理与操作日志查看接口
 */
@Tag(name = "管理后台 - 工艺变更日志管理")
@RestController
@RequestMapping("/rm/recipe-change-log")
@Validated
public class RecipeChangeLogController {

    /**
     * 工艺变更通知日志管理服务
     */
    @Resource
    private RecipeChangeLogService recipeChangeLogService;
    @Resource
    private RecipeChangeRetryService recipeChangeRetryService;

    /**
     * 对选中的失败通知执行人工重发
     */
    @PostMapping("/manual-retry")
    @Operation(summary = "人工重发工艺变更通知")
    public CommonResult<RecipeChangeNoticeRetryRespVO> manualRetry(@RequestBody RecipeChangeNoticeRetryReqVO retryReqVO) {
        return success(recipeChangeRetryService.manualRetry(retryReqVO.getNoticeIds(), recipeChangeRetryService.getLoginUsername()));
    }

    /**
     * 批量标记异常工艺变更通知为已完成
     */
    @PostMapping("/batch-mark-complete")
    @Operation(summary = "批量标记工艺变更通知完成")
    public CommonResult<RecipeChangeNoticeRetryRespVO> batchMarkComplete(@RequestBody RecipeChangeNoticeRetryReqVO retryReqVO) {
        return success(recipeChangeRetryService.batchMarkComplete(retryReqVO.getNoticeIds(), recipeChangeRetryService.getLoginUsername()));
    }

    /**
     * 分页查询变更通知管理页面的列表数据
     *
     * @param pageReqVO 页面筛选和分页条件
     * @return 通知分页数据
     */
    @GetMapping("/notice-page")
    @Operation(summary = "分页查询工艺变更通知")
    public CommonResult<PageResult<RecipeChangeNoticePageRespVO>> getRecipeChangeNoticePage(@Validated RecipeChangeNoticePageReqVO pageReqVO) {
        return success(recipeChangeLogService.getRecipeChangeNoticePage(pageReqVO));
    }

    /**
     * 按当前页面筛选条件导出全部工艺变更通知 Excel
     * 未传筛选条件时导出全部未删除通知，不导出 changeContent 大字段
     */
    @GetMapping("/export-notice-excel")
    @Operation(summary = "导出工艺变更通知 Excel")
    public void exportRecipeChangeNoticeExcel(@Validated RecipeChangeNoticePageReqVO pageReqVO,
                                               HttpServletResponse response) throws IOException {
        // 文件名追加导出时间，避免用户连续下载时浏览器覆盖同名文件
        String filename = "工艺变更通知数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ".xlsx";
        // 导出服务不使用 pageNo 和 pageSize，仅按当前筛选条件查询全部匹配记录
        ExcelUtils.write(response, filename, "工艺变更通知数据", RecipeChangeNoticeExportRespVO.class,
                recipeChangeLogService.getRecipeChangeNoticeExportList(pageReqVO));
    }

    /**
     * 查询操作列中“内容查询”所需的原始工艺变更内容
     *
     * @param noticeId 通知主键
     * @return 通知内容
     */
    @GetMapping("/notice-content")
    @Operation(summary = "查询工艺变更通知内容")
    @Parameter(name = "noticeId", description = "通知主键", required = true, example = "1")
    public CommonResult<RecipeChangeNoticeContentRespVO> getRecipeChangeNoticeContent(@RequestParam("noticeId") Long noticeId) {
        return success(recipeChangeLogService.getRecipeChangeNoticeContent(noticeId));
    }

    /**
     * 分页查询操作日志查看页面的列表数据
     *
     * @param noticeId 通知主键
     * @return 操作日志分页数据
     */
    @GetMapping("/operation-log-page")
    @Operation(summary = "分页查询工艺变更操作日志")
    @Parameter(name = "noticeId", description = "通知主键", required = true, example = "1")
    public CommonResult<PageResult<RecipeChangeOperationLogPageRespVO>> getRecipeChangeOperationLogPage(@RequestParam("noticeId") Long noticeId, @Validated PageParam pageParam) {
        return success(recipeChangeLogService.getRecipeChangeOperationLogPage(noticeId, pageParam));
    }

}
