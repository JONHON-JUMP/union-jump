package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Schema(description = "MES管理 - MPM工艺文件地址 请求参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessFileUrlReqVO {

    @Schema(description = "MPM工序对象oid", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序oid不能为空")
    private String oid;
}
