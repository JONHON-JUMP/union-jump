package cn.jonhon.jump.module.system.dal.mysql.bizsystem;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.bizsystem.BizSystemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BizSystemMapper extends BaseMapperX<BizSystemDO> {

    default PageResult<BizSystemDO> selectPage(BizSystemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BizSystemDO>()
                .likeIfPresent(BizSystemDO::getSystemName, reqVO.getSystemName())
                .likeIfPresent(BizSystemDO::getIp, reqVO.getIp())
                .eqIfPresent(BizSystemDO::getPort, reqVO.getPort())
                .eqIfPresent(BizSystemDO::getFramework, reqVO.getFramework())
                .orderByAsc(BizSystemDO::getSortNo)
                .orderByAsc(BizSystemDO::getId));
    }

    default BizSystemDO selectByIpAndPort(String ip, Integer port) {
        return selectOne(new LambdaQueryWrapperX<BizSystemDO>()
                .eq(BizSystemDO::getIp, ip)
                .eq(BizSystemDO::getPort, port));
    }

}
