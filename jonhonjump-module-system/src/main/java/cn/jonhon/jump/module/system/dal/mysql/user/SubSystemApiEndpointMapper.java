package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface SubSystemApiEndpointMapper extends BaseMapperX<SubSystemApiEndpointDO> {

    default List<SubSystemApiEndpointDO> selectListBySubSystemId(Long subSystemId) {
        return selectList(new LambdaQueryWrapperX<SubSystemApiEndpointDO>()
                .eq(SubSystemApiEndpointDO::getSubSystemId, subSystemId)
                .orderByAsc(SubSystemApiEndpointDO::getSort)
                .orderByAsc(SubSystemApiEndpointDO::getId));
    }

    default SubSystemApiEndpointDO selectBySubSystemIdAndPurpose(Long subSystemId, String purpose) {
        return selectOne(new LambdaQueryWrapperX<SubSystemApiEndpointDO>()
                .eq(SubSystemApiEndpointDO::getSubSystemId, subSystemId)
                .eq(SubSystemApiEndpointDO::getPurpose, purpose));
    }

    /** 指定用途接口已启用的系统 ID 列表（接口目标下拉用） */
    default List<Long> selectEnabledSubSystemIdsByPurpose(String purpose) {
        return selectList(new LambdaQueryWrapperX<SubSystemApiEndpointDO>()
                .eq(SubSystemApiEndpointDO::getPurpose, purpose)
                .eq(SubSystemApiEndpointDO::getEnabled, 1))
                .stream()
                .map(SubSystemApiEndpointDO::getSubSystemId)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 配置保存级联：先逻辑删该系统全部接口行 */
    default void deleteListBySubSystemId(Long subSystemId) {
        delete(new LambdaQueryWrapperX<SubSystemApiEndpointDO>()
                .eq(SubSystemApiEndpointDO::getSubSystemId, subSystemId));
    }

}
