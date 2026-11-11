package cn.jonhon.jump.framework.recipechange.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工艺变更 RabbitMQ 消费者 Starter 的 MES 侧配置。
 *
 * RabbitMQ 连接、队列命名和 JUMP 通信参数均由 Starter 内置；MES 仅声明自己负责的车间。
 */
@ConfigurationProperties(prefix = "jonhonjump.recipe-change.consumer")
public class RecipeChangeConsumerProperties {

    /** 是否启用工艺变更消息消费功能。 */
    private boolean enabled = false;

    /** 当前 MES 需要消费的车间编码列表；配置几个车间，就监听几个车间的专属主队列。 */
    private List<String> workshopCodes = new ArrayList<>();

    /** 根据 Starter 内置的主队列前缀生成指定车间需要监听的队列名称。 */
    public String getQueueName(String workshopCode) {
        return RecipeChangeConsumerConstants.QUEUE_NAME_PREFIX + workshopCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getWorkshopCodes() {
        return workshopCodes;
    }

    public void setWorkshopCodes(List<String> workshopCodes) {
        this.workshopCodes = workshopCodes;
    }

    /** 校验并规范化 MES 需要监听的车间编码列表。 */
    public void validateWorkshopCodes() {
        if (workshopCodes == null || workshopCodes.isEmpty()) {
            throw new IllegalStateException("启用工艺变更消费者时必须至少配置一个 workshop-codes");
        }
        Set<String> normalizedWorkshopCodes = new LinkedHashSet<>();
        for (String workshopCode : workshopCodes) {
            if (isBlank(workshopCode)) {
                throw new IllegalStateException("workshop-codes 中不能包含空车间编码");
            }
            normalizedWorkshopCodes.add(workshopCode.trim());
        }
        workshopCodes = new ArrayList<>(normalizedWorkshopCodes);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
