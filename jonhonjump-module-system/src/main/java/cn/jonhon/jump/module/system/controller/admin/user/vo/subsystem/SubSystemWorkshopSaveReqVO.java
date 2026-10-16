package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;
import cn.jonhon.jump.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 业务系统子表叶子创建/更新 Request VO")
@Data
public class SubSystemWorkshopSaveReqVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属业务系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务系统不能为空")
    private Long subSystemId;

    @Schema(description = "叶子编号（车间编号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5501")
    @NotBlank(message = "叶子编号不能为空")
    @Size(max = 50, message = "叶子编号长度不能超过 50 个字符")
    private String workshopCode;

    @Schema(description = "叶子名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "环控管路交付中心")
    @NotBlank(message = "叶子名称不能为空")
    @Size(max = 100, message = "叶子名称长度不能超过 100 个字符")
    private String workshopName;

    @Schema(description = "产品线（分组展示）", example = "环控")
    @Size(max = 50, message = "产品线长度不能超过 50 个字符")
    private String productLine;

    @Schema(description = "排序", example = "10")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
