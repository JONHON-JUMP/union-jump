package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingListRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiOptionRespVO;
import cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum;

import java.util.List;

public interface LeafApiBindingService {

    LeafApiBindingListRespVO getLeafBindings(Long bizSubSystemId, String workshopCode);

    List<LeafApiOptionRespVO> getApiOptions();

    void saveLeafBindings(LeafApiBindingSaveReqVO reqVO);

    /**
     * 按业务叶子和页面调用解析接口系统。未绑定返回 null。
     */
    Long findApiSubSystemId(Long bizSubSystemId, String workshopCode, LeafApiSceneEnum scene);

    /**
     * 与 {@link #findApiSubSystemId} 相同，未绑定时报可读错误。
     */
    Long requireApiSubSystemId(Long bizSubSystemId, String workshopCode, LeafApiSceneEnum scene);

}
