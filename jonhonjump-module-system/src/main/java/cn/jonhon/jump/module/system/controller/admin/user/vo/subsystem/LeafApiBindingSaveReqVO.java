package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 保存叶子接口绑定")
@Data
public class LeafApiBindingSaveReqVO {

    @Schema(description = "业务系统 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务系统不能为空")
    private Long bizSubSystemId;

    @Schema(description = "叶子编号", example = "5503")
    private String workshopCode;

    @Valid
    private List<LeafApiBindingItemReqVO> items;

}
