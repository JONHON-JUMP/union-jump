package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "MES管理 - 临时工艺 请求参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporaryProcessReqVO {

    @Schema(description = "物料号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料号不能为空")
    private String prtno;

    @Schema(description = "部门编码", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String plndept;

    @Schema(description = "工艺规程号", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String accno;

    @Schema(description = "是否返修订单0否；1是", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "是否返修订单不能为空")
    private String fxtype;
}
