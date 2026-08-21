package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;

import javax.validation.Valid;
import java.util.List;

public interface ProcessService {
    /**
     * 查看工艺卡片
     * @param reqVO
     * @return
     */
    List<ProcessCardRespVO> queryCard(@Valid ProcessCardReqVO reqVO);
}
