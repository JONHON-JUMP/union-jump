package cn.jonhon.jump.module.rm.recipechange.mq.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工艺变更通知完成落库后发布的领域事件
 *
 * 后续 RabbitMQ 分发模块订阅该事件，并在接收事务提交后执行消息分发
 */
@Getter
@AllArgsConstructor
public class RecipeChangeNoticeReceivedEvent {

    /** 已成功落库的工艺变更通知主键 */
    private final Long noticeId;

}
