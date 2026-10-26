package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 已保存的叶子接口绑定")
@Data
public class LeafApiBindingRespVO {

    private String scene;
    private Long apiSubSystemId;
    private String apiSystemName;
    private String apiType;
    private Long endpointId;
    private String endpointName;
    private String endpointUrl;
    private String purpose;

}
