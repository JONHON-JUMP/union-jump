package cn.jonhon.jump.module.system.controller.admin.notify.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 我的站内信详情 Response VO")
@Data
public class NotifyMessageDetailRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "标题", example = "关于计划财务部财务经理岗位的借调启事")
    private String title;

    @Schema(description = "通知类型", example = "2")
    private Integer templateType;

    @Schema(description = "发布人", example = "刘爽")
    private String publisherName;

    @Schema(description = "发布部门", example = "党委干部部人力资源部")
    private String deptName;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "附件列表")
    private List<NotifyMessageAttachmentVO> attachments;

    @Schema(description = "是否已读", example = "false")
    private Boolean readStatus;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

}
