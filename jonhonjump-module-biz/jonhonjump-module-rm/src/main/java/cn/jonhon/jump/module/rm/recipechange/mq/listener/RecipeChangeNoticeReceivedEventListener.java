package cn.jonhon.jump.module.rm.recipechange.mq.listener;

import cn.jonhon.jump.module.rm.recipechange.mq.event.RecipeChangeNoticeReceivedEvent;
import cn.jonhon.jump.module.rm.recipechange.service.RecipeChangeNoticeDispatchService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

/**
 * 工艺变更通知接收完成事件监听器
 */
@Component
public class RecipeChangeNoticeReceivedEventListener {

    /** 工艺变更通知分发服务 */
    @Resource
    private RecipeChangeNoticeDispatchService recipeChangeNoticeDispatchService;

    /**
     * 在通知接收事务提交后发送 RabbitMQ 消息
     *
     * 仅在主记录、状态流水和操作流水已提交时分发，避免 MES 收到无法查询的通知
     *
     * @param event 已接收的工艺变更通知事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecipeChangeNoticeReceived(RecipeChangeNoticeReceivedEvent event) {
        // 事件只携带主键，分发服务会重新查询已提交的通知数据后再发送消息
        recipeChangeNoticeDispatchService.dispatchRecipeChangeNotice(event.getNoticeId());
    }

}
