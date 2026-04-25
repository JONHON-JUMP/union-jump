package cn.jonhon.jump.module.mes.dal.mysql.wm.stocktaking.plan;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.wm.stocktaking.plan.vo.param.MesWmStockTakingPlanParamPageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.stocktaking.plan.MesWmStockTakingPlanParamDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 盘点方案参数 Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesWmStockTakingPlanParamMapper extends BaseMapperX<MesWmStockTakingPlanParamDO> {

    default PageResult<MesWmStockTakingPlanParamDO> selectPage(MesWmStockTakingPlanParamPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmStockTakingPlanParamDO>()
                .eqIfPresent(MesWmStockTakingPlanParamDO::getPlanId, reqVO.getPlanId())
                .orderByAsc(MesWmStockTakingPlanParamDO::getId));
    }

    default List<MesWmStockTakingPlanParamDO> selectListByPlanId(Long planId) {
        return selectList(MesWmStockTakingPlanParamDO::getPlanId, planId);
    }

    default void deleteByPlanId(Long planId) {
        delete(MesWmStockTakingPlanParamDO::getPlanId, planId);
    }

}
