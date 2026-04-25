package cn.jonhon.jump.module.trade.convert.aftersale;

import cn.jonhon.jump.module.trade.dal.dataobject.aftersale.AfterSaleLogDO;
import cn.jonhon.jump.module.trade.service.aftersale.bo.AfterSaleLogCreateReqBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AfterSaleLogConvert {

    AfterSaleLogConvert INSTANCE = Mappers.getMapper(AfterSaleLogConvert.class);

    AfterSaleLogDO convert(AfterSaleLogCreateReqBO bean);

}
