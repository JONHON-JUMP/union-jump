package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 物理删除单行。接口行是纯配置字典，被移除的行不留逻辑删残骸
     * （旧实现保存一次就整批逻辑删再重插，表里堆积大量 deleted=1 的死行，且行 ID 每次全变）。
     */
    @Delete("DELETE FROM sub_system_api_endpoint WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /** 物理删除某系统全部接口行（接入系统删除时级联清理） */
    @Delete("DELETE FROM sub_system_api_endpoint WHERE sub_system_id = #{subSystemId}")
    int physicalDeleteBySubSystemId(@Param("subSystemId") Long subSystemId);

}
