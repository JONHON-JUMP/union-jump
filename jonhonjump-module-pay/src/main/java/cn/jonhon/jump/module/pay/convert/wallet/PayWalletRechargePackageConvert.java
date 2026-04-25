package cn.jonhon.jump.module.pay.convert.wallet;

import java.util.*;

import cn.jonhon.jump.framework.common.pojo.PageResult;

import cn.jonhon.jump.module.pay.controller.admin.wallet.vo.rechargepackage.WalletRechargePackageCreateReqVO;
import cn.jonhon.jump.module.pay.controller.admin.wallet.vo.rechargepackage.WalletRechargePackageRespVO;
import cn.jonhon.jump.module.pay.controller.admin.wallet.vo.rechargepackage.WalletRechargePackageUpdateReqVO;
import cn.jonhon.jump.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletRechargePackageConvert {

    PayWalletRechargePackageConvert INSTANCE = Mappers.getMapper(PayWalletRechargePackageConvert.class);

    PayWalletRechargePackageDO convert(WalletRechargePackageCreateReqVO bean);

    PayWalletRechargePackageDO convert(WalletRechargePackageUpdateReqVO bean);

    WalletRechargePackageRespVO convert(PayWalletRechargePackageDO bean);

    List<WalletRechargePackageRespVO> convertList(List<PayWalletRechargePackageDO> list);

    PageResult<WalletRechargePackageRespVO> convertPage(PageResult<PayWalletRechargePackageDO> page);

}
