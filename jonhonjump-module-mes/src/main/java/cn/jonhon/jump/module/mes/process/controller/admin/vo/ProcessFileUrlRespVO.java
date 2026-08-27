package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "MES管理 - MPM工艺文件地址 响应参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessFileUrlRespVO {

    @Schema(description = "工艺文件地址")
    private String url;
}
