package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 子系统人员分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SubSystemEmployeePageReqVO extends PageParam {

    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "外部系统不能为空")
    private Long subSystemId;

    @Schema(description = "车间编码（过滤）", example = "4200")
    private String workshopCode;

    @Schema(description = "工号（模糊过滤）", example = "00078")
    private String userCode;

    @Schema(description = "姓名（模糊过滤）", example = "张三")
    private String userName;

}
