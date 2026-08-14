package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 子系统用户导入结果")
@Data
@Builder
public class SubSystemUserImportRespVO {

    @Schema(description = "新建绑定成功的标识列表")
    private List<String> createKeys;

    @Schema(description = "更新绑定成功的标识列表")
    private List<String> updateKeys;

    @Schema(description = "失败集合，key 为行标识，value 为原因")
    private Map<String, String> failureKeys;

}
