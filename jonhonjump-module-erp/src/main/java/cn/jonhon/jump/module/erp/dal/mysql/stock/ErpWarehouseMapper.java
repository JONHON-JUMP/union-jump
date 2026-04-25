package cn.jonhon.jump.module.erp.dal.mysql.stock;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.jonhon.jump.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 仓库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpWarehouseMapper extends BaseMapperX<ErpWarehouseDO> {

    default PageResult<ErpWarehouseDO> selectPage(ErpWarehousePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpWarehouseDO>()
                .likeIfPresent(ErpWarehouseDO::getName, reqVO.getName())
                .eqIfPresent(ErpWarehouseDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpWarehouseDO::getId));
    }

    default ErpWarehouseDO selectByDefaultStatus() {
        return selectOne(ErpWarehouseDO::getDefaultStatus, true);
    }

    default List<ErpWarehouseDO> selectListByStatus(Integer status) {
        return selectList(ErpWarehouseDO::getStatus, status);
    }

}