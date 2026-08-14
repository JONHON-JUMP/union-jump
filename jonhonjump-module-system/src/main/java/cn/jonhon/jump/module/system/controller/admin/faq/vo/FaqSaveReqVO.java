package cn.jonhon.jump.module.system.controller.admin.faq.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 常见 QA 创建/修改 Request VO")
@Data
public class FaqSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类不能为空")
    private Integer category;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100个字符")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    private String content;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类")
    private Integer status;

}
