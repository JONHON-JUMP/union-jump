package cn.jonhon.jump.module.rm.recipechange.controller.mes;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeProcessingAcquireRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeDingTalkAlarmLogRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mes.vo.RecipeChangeMesCallbackReqVO;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeMesCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;

/**
 * MES 工艺变更消息回调和状态查询接口
 */
@Tag(name = "MES - 工艺变更消息消费")
@RestController
@RequestMapping("/api/mes-recipe-change")
public class RecipeChangeMesCallbackController {

    @Resource
    private RecipeChangeMesCallbackService recipeChangeMesCallbackService;

    /**
     * 原子领取通知的 MES 处理权。重复消息未领取成功时不得执行 MES 本地业务。
     */
    @PostMapping("/processing-acquire")
    @Operation(summary = "领取工艺变更消息处理权")
    @PermitAll
    public CommonResult<RecipeChangeProcessingAcquireRespVO> acquireProcessing(@Parameter(required = true) @RequestParam String notifyId, @Parameter(required = true) @RequestParam String workshopCode) {
        return success(recipeChangeMesCallbackService.acquireProcessing(notifyId, workshopCode));
    }

    /**
     * 接收 MES 对工艺变更消息处理成功或失败的回调。
     * 请求必须携带领取接口返回的处理令牌，避免旧消费者的迟到回调覆盖当前处理结果。
     */
    @PostMapping("/callback")
    @Operation(summary = "回调工艺变更消息处理结果")
    @PermitAll
    public CommonResult<Boolean> callbackProcessResult(@RequestBody RecipeChangeMesCallbackReqVO callbackReqVO) {
        recipeChangeMesCallbackService.callbackProcessResult(callbackReqVO);
        return success(true);
    }

    /**
     * 接收 MES 钉钉告警处理器发送完成后的日志记录请求
     */
    @PostMapping("/dingtalk-alarm-log")
    @Operation(summary = "记录工艺变更钉钉告警发送结果")
    @PermitAll
    public CommonResult<RecipeChangeDingTalkAlarmLogRespVO> recordDingTalkAlarmLog(@RequestBody RecipeChangeDingTalkAlarmLogReqVO dingTalkAlarmLogReqVO) {
        return success(recipeChangeMesCallbackService.recordDingTalkAlarmLog(dingTalkAlarmLogReqVO));
    }

}
