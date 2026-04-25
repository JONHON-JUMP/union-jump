package cn.jonhon.jump.module.member.dal.mysql.config;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.member.dal.dataobject.config.MemberConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分设置 Mapper
 *
 * @author QingX
 */
@Mapper
public interface MemberConfigMapper extends BaseMapperX<MemberConfigDO> {
}
