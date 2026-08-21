package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "MES管理 - 正式工艺 请求参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormalProcessReqVO {

    @Schema(description = "对象类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String objType;

    @Schema(description = "对象编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> objNumbers;

    @Schema(description = "是否查询最新版", requiredMode = Schema.RequiredMode.REQUIRED)
    private String isLatest;

}
