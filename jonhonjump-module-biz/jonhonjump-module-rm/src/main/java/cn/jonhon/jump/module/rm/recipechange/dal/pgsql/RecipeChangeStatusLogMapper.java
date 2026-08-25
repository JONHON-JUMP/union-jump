package cn.jonhon.jump.module.rm.recipechange.dal.pgsql;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeStatusLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工艺变更通知状态流转记录的数据访问接口
 */
@Mapper
public interface RecipeChangeStatusLogMapper extends BaseMapperX<RecipeChangeStatusLogDO> {

    /**
     * 新增工艺变更通知状态流水
     *
     * @param statusLog 待新增的状态流水
     * @return 成功插入的记录数
     */
    int insertStatusLog(RecipeChangeStatusLogDO statusLog);

}
