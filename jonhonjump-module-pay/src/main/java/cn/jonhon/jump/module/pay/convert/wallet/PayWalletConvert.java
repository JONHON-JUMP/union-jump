package cn.jonhon.jump.module.pay.convert.wallet;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.pay.controller.admin.wallet.vo.wallet.PayWalletRespVO;
import cn.jonhon.jump.module.pay.controller.app.wallet.vo.wallet.AppPayWalletRespVO;
import cn.jonhon.jump.module.pay.dal.dataobject.wallet.PayWalletDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletConvert {

    PayWalletConvert INSTANCE = Mappers.getMapper(PayWalletConvert.class);

    AppPayWalletRespVO convert(PayWalletDO bean);

    PayWalletRespVO convert02(PayWalletDO bean);

    PageResult<PayWalletRespVO> convertPage(PageResult<PayWalletDO> page);

}
