package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeRetryRespVO;

import java.util.List;

/**
 * 工艺变更通知重试管理服务
 */
public interface RecipeChangeRetryService {

    /**
     * 执行定时自动重试
     */
    void executeScheduledRetry();

    /**
     * 获取当前登录管理员的账号，用于人工处理操作日志。
     *
     * @return 当前登录管理员 username；未获取到时返回 UNKNOWN
     */
    String getLoginUsername();

    /**
     * 执行人工重发
     */
    RecipeChangeNoticeRetryRespVO manualRetry(List<Long> noticeIds, String username);

    /**
     * 批量标记异常工艺变更通知为已完成
     *
     * @param noticeIds 需要标记完成的通知主键列表
     * @param username 当前登录管理员账号
     * @return 每条通知的标记完成结果
     */
    RecipeChangeNoticeRetryRespVO batchMarkComplete(List<Long> noticeIds, String username);

}
