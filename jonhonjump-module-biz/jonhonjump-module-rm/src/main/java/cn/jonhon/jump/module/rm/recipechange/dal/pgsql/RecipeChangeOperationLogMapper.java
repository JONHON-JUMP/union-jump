package cn.jonhon.jump.module.rm.recipechange.dal.pgsql;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeOperationLogDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工艺变更通知操作流水的数据访问接口
 */
@Mapper
public interface RecipeChangeOperationLogMapper extends BaseMapperX<RecipeChangeOperationLogDO> {

    /**
     * 新增工艺变更操作流水
     *
     * @param operationLog 待新增的操作流水
     * @return 成功插入的记录数
     */
    int insertOperationLog(RecipeChangeOperationLogDO operationLog);

    /**
     * 分页查询指定工艺变更通知的操作日志
     *
     * @param page 分页对象
     * @param noticeId 通知主键
     * @return 按操作时间正序排列的操作日志
     */
    Page<RecipeChangeOperationLogDO> selectOperationLogPage(Page<RecipeChangeOperationLogDO> page, @Param("noticeId") Long noticeId);

}
