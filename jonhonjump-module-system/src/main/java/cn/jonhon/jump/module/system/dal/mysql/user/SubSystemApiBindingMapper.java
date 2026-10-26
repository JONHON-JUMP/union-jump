package cn.jonhon.jump.module.system.dal.mysql.user;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SubSystemApiBindingMapper extends BaseMapperX<SubSystemApiBindingDO> {

    default List<SubSystemApiBindingDO> selectListByLeaf(Long bizSubSystemId, String workshopCode) {
        return selectList(new LambdaQueryWrapperX<SubSystemApiBindingDO>()
                .eq(SubSystemApiBindingDO::getBizSubSystemId, bizSubSystemId)
                .eq(SubSystemApiBindingDO::getWorkshopCode, workshopCode));
    }

    default SubSystemApiBindingDO selectByLeafAndScene(Long bizSubSystemId, String workshopCode, String scene) {
        return selectOne(new LambdaQueryWrapperX<SubSystemApiBindingDO>()
                .eq(SubSystemApiBindingDO::getBizSubSystemId, bizSubSystemId)
                .eq(SubSystemApiBindingDO::getWorkshopCode, workshopCode)
                .eq(SubSystemApiBindingDO::getScene, scene));
    }

}
