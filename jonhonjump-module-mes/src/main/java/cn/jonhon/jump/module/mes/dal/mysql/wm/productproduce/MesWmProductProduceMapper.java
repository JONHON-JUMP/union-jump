package cn.jonhon.jump.module.mes.dal.mysql.wm.productproduce;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 生产入库单 Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesWmProductProduceMapper extends BaseMapperX<MesWmProductProduceDO> {

    default MesWmProductProduceDO selectByFeedbackId(Long feedbackId) {
        return selectOne(MesWmProductProduceDO::getFeedbackId, feedbackId);
    }

}
