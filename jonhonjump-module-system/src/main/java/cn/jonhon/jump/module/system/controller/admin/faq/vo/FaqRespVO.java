package cn.jonhon.jump.module.system.controller.admin.faq.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 常见 QA Response VO")
@Data
public class FaqRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "分类")
    private Integer category;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "发布人")
    private String publisherName;

    @Schema(description = "发布部门")
    private String deptName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建者")
    private String creator;

}
