package cn.jonhon.jump.module.system.dal.mysql.permission;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.permission.vo.menucolor.MenuColorPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MenuColorMapper extends BaseMapperX<MenuColorDO> {

    default PageResult<MenuColorDO> selectPage(MenuColorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MenuColorDO>()
                .likeIfPresent(MenuColorDO::getName, reqVO.getName())
                .likeIfPresent(MenuColorDO::getMesCategory, reqVO.getMesCategory())
                .eqIfPresent(MenuColorDO::getStatus, reqVO.getStatus())
                .orderByAsc(MenuColorDO::getSort)
                .orderByDesc(MenuColorDO::getId));
    }

    default List<MenuColorDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<MenuColorDO>()
                .eqIfPresent(MenuColorDO::getStatus, status)
                .orderByAsc(MenuColorDO::getSort)
                .orderByDesc(MenuColorDO::getId));
    }

    default MenuColorDO selectByName(String name) {
        return selectOne(MenuColorDO::getName, name);
    }

    default List<MenuColorDO> selectListByIds(Collection<Long> ids) {
        return selectList(MenuColorDO::getId, ids);
    }

}
