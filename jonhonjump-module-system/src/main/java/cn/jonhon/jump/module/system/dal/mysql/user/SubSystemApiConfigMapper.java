package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SubSystemApiConfigMapper extends BaseMapperX<SubSystemApiConfigDO> {

    default SubSystemApiConfigDO selectBySubSystemId(Long subSystemId) {
        return selectOne(new LambdaQueryWrapperX<SubSystemApiConfigDO>()
                .eq(SubSystemApiConfigDO::getSubSystemId, subSystemId));
    }

    /**
     * 已配置且启用的系统 ID 列表（人员管理页左侧卡片只显示这些系统）
     */
    default List<SubSystemApiConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SubSystemApiConfigDO>()
                .eq(SubSystemApiConfigDO::getStatus, 0)
                .orderByAsc(SubSystemApiConfigDO::getSubSystemId));
    }

}
