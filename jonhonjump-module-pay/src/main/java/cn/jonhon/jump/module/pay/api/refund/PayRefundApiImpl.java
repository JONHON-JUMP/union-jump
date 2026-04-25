package cn.jonhon.jump.module.pay.api.refund;

import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.jonhon.jump.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.jonhon.jump.module.pay.dal.dataobject.refund.PayRefundDO;
import cn.jonhon.jump.module.pay.service.refund.PayRefundService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 退款单 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class PayRefundApiImpl implements PayRefundApi {

    @Resource
    private PayRefundService payRefundService;

    @Override
    public Long createRefund(PayRefundCreateReqDTO reqDTO) {
        return payRefundService.createRefund(reqDTO);
    }

    @Override
    public PayRefundRespDTO getRefund(Long id) {
        PayRefundDO refund = payRefundService.getRefund(id);
        return BeanUtils.toBean(refund, PayRefundRespDTO.class);
    }

}
