package cn.jonhon.jump.module.mes.dal.mysql.wm.stocktaking.task;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.wm.stocktaking.task.vo.result.MesWmStockTakingTaskResultPageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.stocktaking.task.MesWmStockTakingTaskResultDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 盘点结果 Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesWmStockTakingTaskResultMapper extends BaseMapperX<MesWmStockTakingTaskResultDO> {

    default PageResult<MesWmStockTakingTaskResultDO> selectPage(MesWmStockTakingTaskResultPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmStockTakingTaskResultDO>()
                .eqIfPresent(MesWmStockTakingTaskResultDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getLocationId, reqVO.getLocationId())
                .eqIfPresent(MesWmStockTakingTaskResultDO::getAreaId, reqVO.getAreaId())
                .orderByDesc(MesWmStockTakingTaskResultDO::getId));
    }

    default void deleteByTaskId(Long taskId) {
        delete(MesWmStockTakingTaskResultDO::getTaskId, taskId);
    }

}
