package cn.jonhon.jump.module.rm.recipechange.job;

import cn.jonhon.jump.framework.quartz.core.handler.JobHandler;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeRetryService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 工艺变更通知自动重试任务处理器
 *
 * 在任务管理页面配置 Handler 名称 recipeChangeRetryJobHandler 和执行周期后，由 Quartz 调用
 */
@Component("recipeChangeRetryJobHandler")
public class RecipeChangeRetryJobHandler implements JobHandler {

    /**
     * 工艺变更通知重试管理服务
     */
    @Resource
    private RecipeChangeRetryService recipeChangeRetryService;

    /**
     * 执行发送失败通知的自动重试
     *
     * @param param 任务管理页面配置的参数，当前不需要业务参数
     * @return 本次任务执行结果
     */
    @Override
    public String execute(String param) {
        recipeChangeRetryService.executeScheduledRetry();
        return "工艺变更通知自动重试执行完成";
    }

}
