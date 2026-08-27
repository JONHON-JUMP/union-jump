package cn.jonhon.jump.module.rm.recipechange.config;

import lombok.Data;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 工艺变更 RabbitMQ 配置属性
 * 配置前缀为 application-local.yaml 中的 jonhonjump.recipe-change.mq
 * 将交换机和路由键前缀放在配置文件中，部署到不同环境时无需修改 Java 代码
 */
@ConfigurationProperties(prefix = "jonhonjump.recipe-change.mq")
@Data
public class RecipeChangeRabbitMQProperties {

    /**
     * 工艺变更专用 RabbitMQ 服务地址
     * 不读取 spring.rabbitmq，避免与系统其他业务的 RabbitMQ 连接混用
     */
    private String host;

    /**
     * 工艺变更专用 RabbitMQ 服务端口
     */
    private int port = 5672;

    /**
     * 工艺变更专用 RabbitMQ 登录用户名
     */
    private String username;

    /**
     * 工艺变更专用 RabbitMQ 登录密码
     */
    private String password;

    /**
     * 工艺变更专用 RabbitMQ 虚拟主机
     */
    private String virtualHost = "/";

    /**
     * 工艺变更消息发布确认模式
     * 使用 correlated 使发送服务能够按 CorrelationData 等待本次消息的 Broker 确认
     */
    private CachingConnectionFactory.ConfirmType publisherConfirmType = CachingConnectionFactory.ConfirmType.CORRELATED;

    /**
     * 是否启用发送端返回机制
     * 与 mandatory 配合用于感知消息无法路由至车间队列的情况
     */
    private boolean publisherReturns = true;

    /**
     * 是否要求消息无法路由时返回发送端
     */
    private boolean mandatory = true;

    /**
     * 工艺变更直连交换机名称
     * 发送方 JUMP 将消息投递到该 Exchange，Exchange 再依据路由键将消息转发至 MES 队列
     */
    private String exchange;

    /**
     * 按车间编码生成路由键时使用的前缀
     * 例如配置值为 RECIPE_CHANGE_ROUTE_，车间编码为 5600 时生成 RECIPE_CHANGE_ROUTE_5600
     */
    private String routingKeyPrefix;

    /**
     * 按车间编码生成队列名称时使用的前缀
     * 例如配置值为 QUEUE_RECIPE_CHANGE_，车间编码为 5600 时生成 QUEUE_RECIPE_CHANGE_5600
     */
    private String queueNamePrefix;

    /**
     * 工艺变更延迟重试直连交换机名称
     * 主队列中的车间处理失败消息会死信到该交换机，再路由至对应车间的延迟重试队列
     */
    private String retryExchange;

    /**
     * 按车间编码生成延迟重试路由键时使用的前缀
     * 例如配置值为 RECIPE_CHANGE_RETRY_ROUTE_，车间编码为 5600 时生成 RECIPE_CHANGE_RETRY_ROUTE_5600
     */
    private String retryRoutingKeyPrefix;

    /**
     * 按车间编码生成延迟重试队列名称时使用的前缀
     * 例如配置值为 QUEUE_RECIPE_CHANGE_RETRY_，车间编码为 5600 时生成 QUEUE_RECIPE_CHANGE_RETRY_5600
     */
    private String retryQueueNamePrefix;

    /**
     * 延迟重试队列中的消息存活时间，超时后自动死信回主交换机，单位毫秒
     */
    private long retryDelayMillis;

    /**
     * 指定车间的版本化重试队列覆盖配置。
     * 未配置的车间继续使用全局 V1 队列前缀和延迟时长；已配置的车间由 JUMP 自动声明并绑定对应版本队列。
     */
    private Map<String, RetryQueueOverride> retryQueueOverrides = new HashMap<>();

    /**
     * 获取车间当前生效的重试队列名称，确保 JUMP 与 Starter 可使用同一个版本化队列。
     */
    public String getRetryQueueName(String workshopCode) {
        RetryQueueOverride override = getRetryQueueOverride(workshopCode);
        String prefix = override != null && override.getQueueNamePrefix() != null && !override.getQueueNamePrefix().trim().isEmpty()
                ? override.getQueueNamePrefix() : retryQueueNamePrefix;
        return prefix + workshopCode;
    }

    /**
     * 获取车间当前生效的重试延迟时长。
     */
    public long getRetryDelayMillis(String workshopCode) {
        RetryQueueOverride override = getRetryQueueOverride(workshopCode);
        return override != null && override.getDelayMillis() != null ? override.getDelayMillis() : retryDelayMillis;
    }

    /**
     * 安全读取车间覆盖项。
     * retry-queue-overrides 是可选配置；未配置或配置为空时 Spring 可能保留空 Map 或绑定为 null，
     * 两种情况都必须回退全局默认值，不能影响既有车间的 MQ 声明。
     */
    private RetryQueueOverride getRetryQueueOverride(String workshopCode) {
        return retryQueueOverrides == null ? null : retryQueueOverrides.get(workshopCode);
    }

    /** 单个车间的版本化重试队列覆盖项。 */
    @Data
    public static class RetryQueueOverride {
        /** 例如 QUEUE_RECIPE_CHANGE_RETRY_V2_ */
        private String queueNamePrefix;
        /** 队列 x-message-ttl，单位毫秒 */
        private Long delayMillis;
    }

}
