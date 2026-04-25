package cn.jonhon.jump.module.mes.dal.mysql.pro.workrecord;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.mes.dal.dataobject.pro.workrecord.MesProWorkRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 当前绑定状态（快照） Mapper
 *
 * @author 中航光电
 */
@Mapper
public interface MesProWorkRecordMapper extends BaseMapperX<MesProWorkRecordDO> {

    default MesProWorkRecordDO selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<MesProWorkRecordDO>()
                .eq(MesProWorkRecordDO::getUserId, userId));
    }

}
