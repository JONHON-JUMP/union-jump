package cn.jonhon.jump.module.system.controller.admin.notify.vo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 站内信附件")
@Data
public class NotifyMessageAttachmentVO {

    @Schema(description = "附件名称", example = "通知.docx")
    private String name;

    @Schema(description = "附件地址", example = "https://example.com/a.docx")
    private String url;

    @Schema(description = "附件大小（字节）", example = "633344")
    private Long size;

}
