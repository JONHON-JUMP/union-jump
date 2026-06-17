package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import cn.jonhon.jump.framework.common.enums.CommonStatusEnum;

import cn.jonhon.jump.framework.common.validation.InEnum;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



import javax.validation.constraints.NotBlank;

import javax.validation.constraints.NotNull;

import javax.validation.constraints.Size;



@Schema(description = "管理后台 - 外部系统岗位创建/更新 Request VO")

@Data

public class SubSystemPostSaveReqVO {



    @Schema(description = "岗位编号")

    private Long id;



    @Schema(description = "外部系统 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")

    @NotNull(message = "外部系统不能为空")

    private Long subSystemId;



    @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "班组长")

    @NotBlank(message = "岗位名称不能为空")

    @Size(max = 30, message = "岗位名称长度不能超过 30 个字符")

    private String name;



    @Schema(description = "岗位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "team_leader")

    @NotBlank(message = "岗位编码不能为空")

    @Size(max = 100, message = "岗位编码长度不能超过 100 个字符")

    private String code;



    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")

    @NotNull(message = "显示顺序不能为空")

    private Integer sort;



    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")

    @NotNull(message = "状态不能为空")

    @InEnum(value = CommonStatusEnum.class, message = "状态必须是 {value}")

    private Integer status;



}

