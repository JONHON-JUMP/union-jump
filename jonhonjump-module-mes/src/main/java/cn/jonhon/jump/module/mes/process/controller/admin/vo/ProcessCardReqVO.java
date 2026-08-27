package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "MES管理 - 工艺卡片查看 请求参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessCardReqVO {

    @Schema(description = "物料编码", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String prtno;

    @Schema(description = "工艺规程号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "工艺规程号不能为空")
    private String accno;

}
