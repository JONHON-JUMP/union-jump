package cn.jonhon.jump.module.erp.service.statistics;

import cn.jonhon.jump.module.erp.dal.mysql.statistics.ErpSaleStatisticsMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 销售统计 Service 实现类
 *
 * @author 芋道源码
 */
@Service
public class ErpSaleStatisticsServiceImpl implements ErpSaleStatisticsService {

    @Resource
    private ErpSaleStatisticsMapper saleStatisticsMapper;

    @Override
    public BigDecimal getSalePrice(LocalDateTime beginTime, LocalDateTime endTime) {
        return saleStatisticsMapper.getSalePrice(beginTime, endTime);
    }

}
