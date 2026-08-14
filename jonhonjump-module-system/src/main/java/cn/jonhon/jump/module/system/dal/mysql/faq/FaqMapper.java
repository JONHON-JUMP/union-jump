package cn.jonhon.jump.module.system.dal.mysql.faq;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.jonhon.jump.module.system.controller.admin.faq.vo.FaqPageReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.faq.FaqDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FaqMapper extends BaseMapperX<FaqDO> {

    default PageResult<FaqDO> selectPage(FaqPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FaqDO>()
                .likeIfPresent(FaqDO::getTitle, reqVO.getTitle())
                .eqIfPresent(FaqDO::getCategory, reqVO.getCategory())
                .eqIfPresent(FaqDO::getStatus, reqVO.getStatus())
                .likeIfPresent(FaqDO::getPublisherName, reqVO.getPublisherName())
                .likeIfPresent(FaqDO::getDeptName, reqVO.getDeptName())
                .betweenIfPresent(FaqDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FaqDO::getSort)
                .orderByDesc(FaqDO::getId));
    }

}
