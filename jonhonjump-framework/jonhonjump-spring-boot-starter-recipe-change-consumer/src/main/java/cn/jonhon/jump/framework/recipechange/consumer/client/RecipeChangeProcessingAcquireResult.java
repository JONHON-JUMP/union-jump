package cn.jonhon.jump.framework.recipechange.consumer.client;

/**
 * JUMP 返回的消息处理权领取结果。
 *
 * acquired 为 {@code true} 时 MES 才能处理业务；processingNotRequired 为 {@code true}
 * 时消息已处于成功或人工终止终态；两者均为 {@code false} 表示其他消费者持有有效处理租约。
 */
public class RecipeChangeProcessingAcquireResult {

    private final boolean acquired;
    private final boolean processingNotRequired;
    private final String processingToken;

    public RecipeChangeProcessingAcquireResult(boolean acquired, boolean processingNotRequired, String processingToken) {
        this.acquired = acquired;
        this.processingNotRequired = processingNotRequired;
        this.processingToken = processingToken;
    }

    public boolean isAcquired() {
        return acquired;
    }

    public boolean isProcessingNotRequired() {
        return processingNotRequired;
    }

    public String getProcessingToken() {
        return processingToken;
    }

}
