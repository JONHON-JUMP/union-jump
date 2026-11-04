package cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 叶子接口绑定列表")
@Data
public class LeafApiBindingListRespVO {

    @Schema(description = "可配置的页面调用")
    private List<LeafApiBindingSceneRespVO> scenes;

    @Schema(description = "当前叶子已保存的绑定")
    private List<LeafApiBindingRespVO> bindings;

}
