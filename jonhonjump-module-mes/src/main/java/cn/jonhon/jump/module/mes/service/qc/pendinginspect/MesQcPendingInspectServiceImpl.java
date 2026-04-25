package cn.jonhon.jump.module.mes.service.qc.pendinginspect;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.util.MyBatisUtils;
import cn.jonhon.jump.module.mes.controller.admin.qc.pendinginspect.vo.MesQcPendingInspectPageReqVO;
import cn.jonhon.jump.module.mes.controller.admin.qc.pendinginspect.vo.MesQcPendingInspectRespVO;
import cn.jonhon.jump.module.mes.dal.mysql.qc.pendinginspect.MesQcPendingInspectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * MES 待检任务 Service 实现类
 */
@Service
@Validated
public class MesQcPendingInspectServiceImpl implements MesQcPendingInspectService {

    @Resource
    private MesQcPendingInspectMapper pendingInspectMapper;

    @Override
    public PageResult<MesQcPendingInspectRespVO> getPendingInspectPage(MesQcPendingInspectPageReqVO pageReqVO) {
        // 分页查询
        IPage<MesQcPendingInspectRespVO> page = MyBatisUtils.buildPage(pageReqVO);
        pendingInspectMapper.selectQcPendingPage(page,
                pageReqVO.getSourceDocCode(), pageReqVO.getQcType(), pageReqVO.getItemId());
        // 返回结果
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

}
