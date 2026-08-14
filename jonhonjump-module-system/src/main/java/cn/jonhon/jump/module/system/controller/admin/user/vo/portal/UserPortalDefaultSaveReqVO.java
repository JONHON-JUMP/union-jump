package cn.jonhon.jump.module.system.controller.admin.user.vo.portal;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;



@Schema(description = "管理后台 - 用户门户默认打开系统保存 Request VO")

@Data

public class UserPortalDefaultSaveReqVO {



    @Schema(description = "默认打开的外部子系统编号，null 表示统一门户主页")

    private Long subSystemId;



}

