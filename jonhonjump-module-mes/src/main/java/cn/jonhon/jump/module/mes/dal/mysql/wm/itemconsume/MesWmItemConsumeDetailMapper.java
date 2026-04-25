package cn.jonhon.jump.module.mes.dal.mysql.wm.itemconsume;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 物料消耗记录明细 Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesWmItemConsumeDetailMapper extends BaseMapperX<MesWmItemConsumeDetailDO> {

    default List<MesWmItemConsumeDetailDO> selectListByConsumeId(Long consumeId) {
        return selectList(MesWmItemConsumeDetailDO::getConsumeId, consumeId);
    }

    default List<MesWmItemConsumeDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmItemConsumeDetailDO::getLineId, lineId);
    }

}
