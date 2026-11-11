package cn.jonhon.jump.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 用户导入 Response VO")
@Data
@Builder
public class UserImportRespVO {

    @Schema(description = "导入成功的行数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer successCount;

    @Schema(description = "导入失败的行数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer failureCount;

    @Schema(description = "导入失败的用户集合，key 为行标识（第 N 行 用户名 / 业务用户名），value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureUsernames;

    @Schema(description = "新建的 JUMP 主用户名数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createUsernames;

    @Schema(description = "新建的业务账号，格式：用户名 / 业务用户名")
    @Builder.Default
    private List<String> businessCreated = new ArrayList<>();

    @Schema(description = "挂接的已有业务账号（花名册原本未挂人），格式：用户名 / 业务用户名")
    @Builder.Default
    private List<String> businessLinked = new ArrayList<>();

    @Schema(description = "更新的已有业务账号（已挂当前用户，本地已按表内数据对齐），格式：用户名 / 业务用户名")
    @Builder.Default
    private List<String> businessUpdated = new ArrayList<>();

}
