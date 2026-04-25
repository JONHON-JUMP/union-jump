package cn.jonhon.jump.module.mes.dal.mysql.pro.route;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 工艺路线 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProRouteMapper extends BaseMapperX<MesProRouteDO> {

    default PageResult<MesProRouteDO> selectPage(MesProRoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProRouteDO>()
                .likeIfPresent(MesProRouteDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProRouteDO::getName, reqVO.getName())
                .eqIfPresent(MesProRouteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProRouteDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProRouteDO::getId));
    }

    default MesProRouteDO selectByCode(String code) {
        return selectOne(MesProRouteDO::getCode, code);
    }

    default List<MesProRouteDO> selectListByStatus(Integer status) {
        return selectList(MesProRouteDO::getStatus, status);
    }

}
