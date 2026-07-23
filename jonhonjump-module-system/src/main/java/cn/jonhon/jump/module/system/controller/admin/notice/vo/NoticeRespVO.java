package cn.jonhon.jump.module.system.controller.admin.notice.vo;

import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessageAttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 通知公告信息 Response VO")
@Data
public class NoticeRespVO {

    @Schema(description = "通知公告序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    private String title;

    @Schema(description = "公告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    private Integer type;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "半生编码")
    private String content;

    @Schema(description = "状态，0草稿 1已发布 2已删除", example = "1")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    @Schema(description = "创建者", example = "1")
    private String creator;

    @Schema(description = "发布人", example = "刘爽")
    private String publisherName;

    @Schema(description = "发布部门", example = "党委干部部人力资源部")
    private String deptName;

    @Schema(description = "附件列表")
    private List<NotifyMessageAttachmentVO> attachments;

}
