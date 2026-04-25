package cn.jonhon.jump.module.mes.dal.mysql.wm.outsourceissue;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.wm.outsourceissue.vo.MesWmOutsourceIssuePageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 外协发料单 Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesWmOutsourceIssueMapper extends BaseMapperX<MesWmOutsourceIssueDO> {

    default PageResult<MesWmOutsourceIssueDO> selectPage(MesWmOutsourceIssuePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmOutsourceIssueDO>()
                .likeIfPresent(MesWmOutsourceIssueDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmOutsourceIssueDO::getName, reqVO.getName())
                .eqIfPresent(MesWmOutsourceIssueDO::getVendorId, reqVO.getVendorId())
                .eqIfPresent(MesWmOutsourceIssueDO::getWorkOrderId, reqVO.getWorkOrderId())
                .betweenIfPresent(MesWmOutsourceIssueDO::getIssueDate, reqVO.getIssueDate())
                .orderByDesc(MesWmOutsourceIssueDO::getId));
    }

    default MesWmOutsourceIssueDO selectByCode(String code) {
        return selectOne(MesWmOutsourceIssueDO::getCode, code);
    }

    default Long selectCountByVendorId(Long vendorId) {
        return selectCount(MesWmOutsourceIssueDO::getVendorId, vendorId);
    }

}
