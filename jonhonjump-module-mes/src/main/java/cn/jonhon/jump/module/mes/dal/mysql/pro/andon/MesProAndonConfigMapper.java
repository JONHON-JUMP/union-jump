package cn.jonhon.jump.module.mes.dal.mysql.pro.andon;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.controller.admin.pro.andon.vo.config.MesProAndonConfigPageReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.pro.andon.MesProAndonConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 安灯呼叫配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProAndonConfigMapper extends BaseMapperX<MesProAndonConfigDO> {

    default PageResult<MesProAndonConfigDO> selectPage(MesProAndonConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProAndonConfigDO>()
                .likeIfPresent(MesProAndonConfigDO::getReason, reqVO.getReason())
                .eqIfPresent(MesProAndonConfigDO::getLevel, reqVO.getLevel())
                .orderByDesc(MesProAndonConfigDO::getId));
    }

}
