package cn.jonhon.jump.module.mes.dal.mysql.wm.returnvendor;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.wm.returnvendor.vo.MesWmReturnVendorPageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.returnvendor.MesWmReturnVendorDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 供应商退货单 Mapper
 */
@Mapper
public interface MesWmReturnVendorMapper extends BaseMapperX<MesWmReturnVendorDO> {

    default PageResult<MesWmReturnVendorDO> selectPage(MesWmReturnVendorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmReturnVendorDO>()
                .likeIfPresent(MesWmReturnVendorDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmReturnVendorDO::getName, reqVO.getName())
                .likeIfPresent(MesWmReturnVendorDO::getPurchaseOrderCode, reqVO.getPurchaseOrderCode())
                .eqIfPresent(MesWmReturnVendorDO::getVendorId, reqVO.getVendorId())
                .orderByDesc(MesWmReturnVendorDO::getId));
    }

    default Long selectCountByVendorId(Long vendorId) {
        return selectCount(MesWmReturnVendorDO::getVendorId, vendorId);
    }

    default MesWmReturnVendorDO selectByCode(String code) {
        return selectOne(MesWmReturnVendorDO::getCode, code);
    }

}
