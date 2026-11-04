package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 可选的具体接口")
@Data
public class LeafApiEndpointOptionVO {

    private Long id;
    private String name;
    private String purpose;
    private String purposeLabel;
    private String url;
    private String method;
    private Boolean enabled;

}
