package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.controller.mpm.vo.RecipeChangeNoticeReqVO;

/**
 * 工艺变更通知接收服务
 */
public interface RecipeChangeNoticeReceiveService {

    /**
     * 校验接收工艺变更通知所必需的业务字段
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 校验失败信息；校验通过时返回 {@code null}
     */
    String validateRequiredFields(RecipeChangeNoticeReqVO reqVO);

    /**
     * 接收并持久化一条 MPM 工艺变更通知
     *
     * 同一个 {@code notifyId} 仅会创建一条通知及其初始日志，重复调用直接返回已有标识
     *
     * @param reqVO MPM 推送的工艺变更通知内容
     * @return 已接收通知的唯一标识
     */
    String receiveRecipeChangeNotice(RecipeChangeNoticeReqVO reqVO);

}
