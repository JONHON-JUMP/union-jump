package cn.jonhon.jump.framework.recipechange.consumer.core;

/**
 * MES 项目实现的工艺变更业务处理扩展点
 */
public interface RecipeChangeMessageProcessor {

    /**
     * 执行 MES 本地的工艺变更业务处理
     * 方法正常返回表示处理成功，抛出异常表示处理失败
     *
     * @param message JUMP 投递的工艺变更消息
     * @throws Exception MES 业务处理失败时抛出，Starter 将回调失败、触发告警并重新入队
     */
    void process(RecipeChangeMessageDTO message) throws Exception;

}
