package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;



import cn.jonhon.jump.framework.common.pojo.PageParam;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import lombok.EqualsAndHashCode;

import org.springframework.format.annotation.DateTimeFormat;



import java.time.LocalDateTime;



import static cn.jonhon.jump.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;



@Schema(description = "管理后台 - 外部系统岗位分页 Request VO")

@Data

@EqualsAndHashCode(callSuper = true)

public class SubSystemPostPageReqVO extends PageParam {



    @Schema(description = "外部系统 ID", example = "1")

    private Long subSystemId;



    @Schema(description = "岗位名称", example = "班组长")

    private String name;



    @Schema(description = "岗位编码", example = "team_leader")

    private String code;



    @Schema(description = "状态", example = "0")

    private Integer status;



    @Schema(description = "创建时间")

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)

    private LocalDateTime[] createTime;



}

