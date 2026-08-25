package cn.jonhon.jump.module.rm.recipechange.controller.mpm;

import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeRespVO;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeNoticeReceiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

/**
 * MPM 工艺变更通知接收接口
 * <p>
 * 负责接收 MPM 推送的工艺变更，并将通知交给接收服务持久化处理
 */
@Tag(name = "MPM - 工艺变更通知")
@RestController
@RequestMapping("/api/mpm-recipe-change")
@Validated
public class RecipeChangeNoticeController {

    /**
     * 工艺变更通知接收服务
     */
    @Resource
    private RecipeChangeNoticeReceiveService recipeChangeNoticeReceiveService;

    /**
     * 接收一条 MPM 工艺变更通知
     * <p>
     * 首先校验 {@code notifyId} 和 {@code workshopCode}；任一为空时直接返回失败响应，
     * 不调用接收服务。成功时严格按接口约定返回 HTTP 200，且响应体中的 {@code code} 固定为 200
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 本次接收结果，{@code data} 为已接收的通知唯一标识
     */
    @PostMapping("/notify")
    @Operation(summary = "接收工艺变更通知")
    @PermitAll
    public RecipeChangeNoticeRespVO notifyRecipeChange(@RequestBody RecipeChangeNoticeReqVO reqVO) {
        // 校验必填字段
        String validationFailureMessage = recipeChangeNoticeReceiveService.validateRequiredFields(reqVO);
        if (validationFailureMessage != null) {
            return new RecipeChangeNoticeRespVO(400, validationFailureMessage, null);
        }
        // 接收通知
        String notifyId = recipeChangeNoticeReceiveService.receiveRecipeChangeNotice(reqVO);
        return new RecipeChangeNoticeRespVO(200, "接收成功", notifyId);
    }

}
