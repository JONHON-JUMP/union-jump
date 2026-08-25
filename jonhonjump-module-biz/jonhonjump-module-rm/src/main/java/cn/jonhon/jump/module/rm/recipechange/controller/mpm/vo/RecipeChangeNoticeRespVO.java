package cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * MPM 工艺变更通知接口的响应参数
 */
@Schema(description = "MPM - 工艺变更通知响应")
@Data
@AllArgsConstructor
public class RecipeChangeNoticeRespVO {

    /** 业务响应码，接收成功时为 200，必填字段缺失时为 400 */
    @Schema(description = "响应结果码，成功为 200，必填字段缺失为 400", example = "200")
    private Integer code;

    /** 业务响应说明，接收成功时为 {@code success}，失败时说明具体缺失字段 */
    @Schema(description = "响应结果说明", example = "success")
    private String msg;

    /** 本次已接收通知的唯一标识，即请求中的 {@code notifyId}；校验失败时为 {@code null} */
    @Schema(description = "已接收的通知唯一标识")
    private String data;

}
