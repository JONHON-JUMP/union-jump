package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 一条叶子接口绑定")
@Data
public class LeafApiBindingItemReqVO {

    @Schema(description = "页面调用", example = "account_sync")
    private String scene;

    @Schema(description = "接口所在系统。为空表示解除该条绑定")
    private Long apiSubSystemId;

    @Schema(description = "具体接口。为空表示解除该条绑定")
    private Long endpointId;

}
