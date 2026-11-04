package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - 同步域账号/卡号/ERP号到业务系统结果 Response VO")
@Data
@Accessors(chain = true)
public class SubSystemUserAccountSyncRespVO {

    @Schema(description = "对方系统用户名（注册时标记拼接车间则为 车间编号_工号）", example = "5500_00078")
    private String userCode;

    @Schema(description = "本次同步的字段", example = "[\"域账号\",\"刷卡卡号\"]")
    private List<String> syncedFields;

    @Schema(description = "结果说明", example = "已同步 域账号、刷卡卡号 到对方账号 5500_00078")
    private String message;

}
