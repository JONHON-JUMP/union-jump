package cn.jonhon.jump.module.system.controller.admin.notice.vo;

import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessageAttachmentVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 通知公告创建/修改 Request VO")
@Data
public class NoticeSaveReqVO {

    @Schema(description = "岗位公告编号", example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 50, message = "公告标题不能超过50个字符")
    private String title;

    @Schema(description = "公告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    @NotNull(message = "公告类型不能为空")
    private Integer type;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "半生编码")
    private String content;

    @Schema(description = "状态，参见 NoticeStatusEnum：0草稿 1已发布 2已删除", example = "0")
    private Integer status;

    @Schema(description = "附件列表")
    private List<NotifyMessageAttachmentVO> attachments;

}
