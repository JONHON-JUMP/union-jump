package cn.jonhon.jump.module.system.controller.admin.user.vo.portal;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;

import lombok.Data;

import lombok.NoArgsConstructor;



@Schema(description = "管理后台 - 用户门户默认打开系统 Response VO")

@Data

@NoArgsConstructor

@AllArgsConstructor

public class UserPortalDefaultRespVO {



    @Schema(description = "默认打开的外部子系统编号，null 表示统一门户主页")

    private Long subSystemId;



    @Schema(description = "默认打开系统：main 为统一门户主页，否则为外部系统 clientId（运行时切换标识）")

    private String defaultSystem;



    @Schema(description = "是否已保存过个人配置")

    private Boolean configured;



}

