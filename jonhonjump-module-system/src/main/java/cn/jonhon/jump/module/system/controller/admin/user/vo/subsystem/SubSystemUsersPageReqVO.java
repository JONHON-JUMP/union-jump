package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外部系统用户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubSystemUsersPageReqVO extends PageParam {

    @Schema(description = "外部系统 ID", example = "1")
    private Long subSystemId;

    @Schema(description = "主数据人员 ID", example = "1")
    private Long mainUserId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "车间编号", example = "WS01")
    private String workshopId;

    @Schema(description = "班组名称", example = "一车间甲班")
    private String teamName;

    @Schema(description = "用户姓名", example = "张三")
    private String nickname;

    @Schema(description = "展示状态：unlinked未关联 / 0正常 / 1禁用（未关联按 main_user_id 为空判断）", example = "0")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
