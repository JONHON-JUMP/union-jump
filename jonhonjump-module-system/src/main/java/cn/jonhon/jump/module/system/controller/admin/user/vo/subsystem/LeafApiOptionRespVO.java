package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 可选的接口系统")
@Data
public class LeafApiOptionRespVO {

    private Long subSystemId;
    private String systemName;
    private String apiType;
    private List<LeafApiEndpointOptionVO> endpoints;

}
