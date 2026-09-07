package cn.jonhon.jump.module.rm.recipechange.controller.mpm;

import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeRespVO;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeNoticeReceiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * 不调用接收服务。无论校验失败、接收异常或成功，均返回 {@link RecipeChangeNoticeRespVO}，
     * 以保证 MPM 侧可按响应体 {@code code} 稳定判断结果。
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
        try {
            String notifyId = recipeChangeNoticeReceiveService.receiveRecipeChangeNotice(reqVO);
            return new RecipeChangeNoticeRespVO(200, "接收成功", notifyId);
        } catch (Exception exception) {
            // 接收服务的事务会随异常回滚；此处保留完整堆栈，并向 MPM 返回固定响应结构。
            log.error("接收 MPM 工艺变更通知失败，notifyId={}, workshopCode={}", reqVO.getNotifyId(), reqVO.getWorkshopCode(), exception);
            return new RecipeChangeNoticeRespVO(500, "工艺变更通知接收失败", null);
        }
    }

}
