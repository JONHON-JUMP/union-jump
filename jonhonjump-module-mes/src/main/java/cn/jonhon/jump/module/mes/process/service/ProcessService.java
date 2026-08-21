package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlRespVO;

import javax.validation.Valid;
import java.util.List;

public interface ProcessService {
    /**
     * 查看工艺卡片
     * @param reqVO
     * @return
     */
    List<ProcessCardRespVO> queryCard(@Valid ProcessCardReqVO reqVO);

    /**
     * 获取 MPM 工艺文件地址
     */
    ProcessFileUrlRespVO queryFileUrl(@Valid ProcessFileUrlReqVO reqVO);
}
