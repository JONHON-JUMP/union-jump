package cn.jonhon.jump.module.rm.recipechange.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工艺变更 MES 处理权配置。
 *
 * 配置前缀为 {@code jonhonjump.recipe-change}，与 RabbitMQ 连接配置分开，避免将业务处理租约误认为 MQ 参数。
 */
@ConfigurationProperties(prefix = "jonhonjump.recipe-change")
@Data
public class RecipeChangeProcessingProperties {

    /**
     * MES 领取工艺变更消息处理权后的有效时长，单位毫秒。
     *
     * 租约到期前，重复消息不得被其他消费者接管；到期后可由重新投递的消息取得新的处理令牌。
     */
    private long processingLeaseMillis = 1200000L;

    /** 校验处理租约必须为正数，避免零或负值导致消息刚领取就被其他消费者接管。 */
    public void validateProcessingLeaseMillis() {
        if (processingLeaseMillis <= 0) {
            throw new IllegalStateException("jonhonjump.recipe-change.processing-lease-millis 必须大于 0");
        }
    }
}
