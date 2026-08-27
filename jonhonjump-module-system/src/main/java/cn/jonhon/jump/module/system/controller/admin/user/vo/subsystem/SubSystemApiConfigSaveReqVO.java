package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 子系统人员接口配置创建/更新 Request VO")
@Data
public class SubSystemApiConfigSaveReqVO {

    @Schema(description = "主键编号")
    private Long id;

    @Schema(description = "外部系统 ID（选择已有业务系统时必填；与 systemName 二选一）", example = "3")
    private Long subSystemId;

    @Schema(description = "新建系统名称（手动接入、可不绑门户 OAuth；与 subSystemId 二选一）", example = "Camstar人员管理")
    @Size(max = 100, message = "系统名称长度不能超过 100 个字符")
    private String systemName;

    @Schema(description = "适配器类型：camstar=Camstar专用、http=通用HTTP", requiredMode = Schema.RequiredMode.REQUIRED, example = "camstar")
    @NotBlank(message = "适配器类型不能为空")
    private String apiType;

    @Schema(description = "接口基地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://127.0.0.1:8090")
    @NotBlank(message = "接口基地址不能为空")
    @Size(max = 255, message = "接口基地址长度不能超过 255 个字符")
    private String baseUrl;

    @Schema(description = "鉴权方式：none / cookie_sso", example = "cookie_sso")
    private String authType;

    @Schema(description = "鉴权配置 JSON", example = "{\"loginPath\":\"/BasicData/SSOLogin/SSOLoginIn\",\"token\":\"\",\"cookieName\":\"Nancal_Cam_SessionId\"}")
    @Size(max = 1024, message = "鉴权配置长度不能超过 1024 个字符")
    private String authConfig;

    @Schema(description = "查询接口 JSON", example = "{\"path\":\"/BasicData/Employee/getEmployeeInfo\",\"method\":\"POST\"}")
    @Size(max = 512, message = "查询接口长度不能超过 512 个字符")
    private String apiQuery;

    @Schema(description = "新增接口 JSON（人员 upsert）")
    @Size(max = 512, message = "新增接口长度不能超过 512 个字符")
    private String apiCreate;

    @Schema(description = "修改接口 JSON（人员 upsert）")
    @Size(max = 512, message = "修改接口长度不能超过 512 个字符")
    private String apiUpdate;

    @Schema(description = "删除接口 JSON")
    @Size(max = 512, message = "删除接口长度不能超过 512 个字符")
    private String apiDelete;

    @Schema(description = "班组下拉接口 JSON")
    @Size(max = 512, message = "班组下拉接口长度不能超过 512 个字符")
    private String apiTeamCombo;

    @Schema(description = "接口目录树 JSON（目录+叶子；叶子 purpose=create 供用户同步「新增人员」）")
    private String apiCatalog;

    @Schema(description = "参数映射 JSON（JUMP标准参数名→对方参数名）", example = "{\"userCode\":\"empNo\"}")
    @Size(max = 2048, message = "参数映射长度不能超过 2048 个字符")
    private String paramMapping;

    @Schema(description = "响应映射 JSON（http 适配器用）", example = "{\"successField\":\"code\",\"successValue\":\"200\",\"listPath\":\"data.list\",\"totalPath\":\"data.total\"}")
    @Size(max = 2048, message = "响应映射长度不能超过 2048 个字符")
    private String responseMapping;

    @Schema(description = "删除二次确认提示语", example = "删除将同时删除该用户在目标系统的域账号")
    @Size(max = 200, message = "删除提示语长度不能超过 200 个字符")
    private String deleteTip;

    @Schema(description = "连接超时（毫秒）", example = "10000")
    private Long connectTimeoutMs;

    @Schema(description = "读取超时（毫秒）", example = "30000")
    private Long readTimeoutMs;

    @Schema(description = "状态：0启用 1停用", example = "0")
    private Integer status;

}
