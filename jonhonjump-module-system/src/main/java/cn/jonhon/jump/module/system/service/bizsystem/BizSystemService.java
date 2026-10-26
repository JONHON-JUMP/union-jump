package cn.jonhon.jump.module.system.service.bizsystem;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.bizsystem.BizSystemDO;

import javax.validation.Valid;
import java.util.List;

public interface BizSystemService {

    Long createBizSystem(@Valid BizSystemSaveReqVO createReqVO);

    void updateBizSystem(@Valid BizSystemSaveReqVO updateReqVO);

    void deleteBizSystem(Long id);

    void deleteBizSystemList(List<Long> ids);

    BizSystemDO getBizSystem(Long id);

    PageResult<BizSystemDO> getBizSystemPage(BizSystemPageReqVO pageReqVO);

}
