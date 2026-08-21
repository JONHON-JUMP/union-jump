package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "MES管理 - 工艺卡片查看 响应明细参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessCardDetailsRespVO {

    @Schema(description = "序号")
    private Long idx;

    @Schema(description = "工序名称")
    private String name;

    @Schema(description = "工序编码")
    private String code;

    @Schema(description = "工序号")
    private String no;

    @Schema(description = "工艺文件url")
    private String url;

}
