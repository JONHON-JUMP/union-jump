package cn.jonhon.jump.framework.recipechange.consumer.config;

import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeJUMPClient;
import cn.jonhon.jump.framework.recipechange.consumer.client.RecipeChangeJUMPHttpClient;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeDingTalkAlarmProcessor;
import cn.jonhon.jump.framework.recipechange.consumer.core.RecipeChangeMessageProcessor;
import cn.jonhon.jump.framework.recipechange.consumer.listener.RecipeChangeMessageListener;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 工艺变更消息消费者 Starter 自动装配
 * 当 MES 应用通过配置显式启用 Starter 后，本类负责创建专用 RabbitMQ 连接工厂、JSON 消息转换器、手动确认监听容器、JUMP HTTP 客户端和消费监听器
 * Starter 只使用 jonhonjump.recipe-change.consumer.rabbitmq 创建的专用连接，不读取或修改 MES 项目的 spring.rabbitmq 连接配置
 */
@AutoConfiguration
@ConditionalOnClass(SimpleRabbitListenerContainerFactory.class)
@ConditionalOnProperty(prefix = "jonhonjump.recipe-change.consumer", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RecipeChangeConsumerProperties.class)
public class RecipeChangeConsumerAutoConfiguration {

    /**
     * 显式注册工艺变更消费者监听的全部车间专属队列名称
     *
     * @param recipeChangeConsumerProperties Starter 配置，提供队列前缀和车间编码列表
     * @return 当前 MES 需要监听的全部 RabbitMQ 主队列名称
     */
    @Bean("recipeChangeConsumerQueueNames")
    public String[] recipeChangeConsumerQueueNames(RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // Starter 只校验 MES 要监听的车间；队列、交换机和绑定全部由 JUMP 注册。
        recipeChangeConsumerProperties.validateWorkshopCodes();
        // 通过显式命名 Bean 向 @RabbitListener 提供所有队列名；列表中每个车间均会被同一个 MES 实例消费。
        return recipeChangeConsumerProperties.getWorkshopCodes().stream()
                .map(recipeChangeConsumerProperties::getQueueName)
                .toArray(String[]::new);
    }

    /**
     * 创建工艺变更消费者专用 RabbitMQ 连接工厂
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含专用 RabbitMQ 连接参数
     * @return 仅供工艺变更消费者使用的 RabbitMQ 连接工厂
     */
    @Bean("recipeChangeConnectionFactory")
    @ConditionalOnMissingBean(name = "recipeChangeConnectionFactory")
    public ConnectionFactory recipeChangeConnectionFactory() {
        // 连接参数由 Starter 内置；基础设施变更时通过重新发布 Starter 统一更新 MES。
        // 创建带连接复用能力的连接工厂，减少同一车间持续消费时重复建立 TCP 连接的开销
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        // 设置专用 RabbitMQ 服务地址
        connectionFactory.setHost(RecipeChangeConsumerConstants.RABBITMQ_HOST);
        // 设置专用 RabbitMQ 服务端口
        connectionFactory.setPort(RecipeChangeConsumerConstants.RABBITMQ_PORT);
        // 设置专用 RabbitMQ 登录用户名
        connectionFactory.setUsername(RecipeChangeConsumerConstants.RABBITMQ_USERNAME);
        // 设置专用 RabbitMQ 登录密码
        connectionFactory.setPassword(RecipeChangeConsumerConstants.RABBITMQ_PASSWORD);
        // 设置专用 RabbitMQ 虚拟主机
        connectionFactory.setVirtualHost(RecipeChangeConsumerConstants.RABBITMQ_VIRTUAL_HOST);
        // 返回命名连接工厂，供工艺变更监听容器通过限定名称注入
        return connectionFactory;
    }

    /**
     * 创建工艺变更消费者专用 JSON 消息转换器
     *
     * @return Starter 默认 JSON 消息转换器
     */
    @Bean("recipeChangeMessageConverter")
    @ConditionalOnMissingBean(name = "recipeChangeMessageConverter")
    public MessageConverter recipeChangeMessageConverter() {
        // 创建命名 Jackson 转换器，避免使用 MES 其他 RabbitMQ 业务的全局消息转换器
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建专用监听容器工厂并启用手动确认模式
     * 失败消息由监听器显式 basicNack 重新入队，避免框架自动确认造成消息丢失
     *
     * @param recipeChangeConnectionFactory 工艺变更消费者专用 RabbitMQ 连接工厂
     * @param recipeChangeMessageConverter 工艺变更消费者专用 JSON 消息转换器
     * @return 仅供工艺变更消息监听器使用的容器工厂
     */
    @Bean("recipeChangeRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory recipeChangeRabbitListenerContainerFactory(
            @Qualifier("recipeChangeConnectionFactory") ConnectionFactory recipeChangeConnectionFactory,
            @Qualifier("recipeChangeMessageConverter") MessageConverter recipeChangeMessageConverter) {
        // 创建本 Starter 专用监听容器工厂，避免改变 MES 现有其他 RabbitMQ 监听器的确认策略
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 注入专用连接工厂，监听器只会建立到工艺变更配置指定的 RabbitMQ Broker
        factory.setConnectionFactory(recipeChangeConnectionFactory);
        // 强制手动确认，只有监听器明确 basicAck 才会从队列移除消息
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 队列仅由 JUMP 在首次分发对应车间消息前注册；MES 先启动时等待监听容器自动恢复，不能因此启动失败。
        factory.setMissingQueuesFatal(false);
        // 未捕获异常时默认重新入队，作为监听器显式 nack 逻辑之外的最后保护
        factory.setDefaultRequeueRejected(true);
        // 注入专用 JSON 转换器，确保消息体稳定转换为 RecipeChangeMessageDTO
        factory.setMessageConverter(recipeChangeMessageConverter);
        // 返回配置完成的专用监听容器工厂
        return factory;
    }

    /**
     * 创建调用 JUMP 接口的 HTTP 客户端，MES 项目可通过自定义同类型 Bean 替换实现
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含 JUMP 服务地址和超时参数
     * @param recipeChangeRestTemplate Starter 创建的 HTTP 调用模板
     * @return 默认 JUMP HTTP 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public RecipeChangeJUMPClient recipeChangeJUMPClient(RestTemplate recipeChangeRestTemplate) {
        // 组合 HTTP 调用模板与 Starter 配置，创建用于查询状态和上报回调的默认客户端
        return new RecipeChangeJUMPHttpClient(recipeChangeRestTemplate);
    }

    /**
     * 创建具备连接和读取超时控制的 JUMP HTTP 调用模板
     *
     * @param recipeChangeConsumerProperties Starter 配置，包含连接和读取超时时间
     * @return 仅供工艺变更 JUMP 客户端使用的 RestTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "recipeChangeRestTemplate")
    public RestTemplate recipeChangeRestTemplate() {
        // 创建底层 HTTP 请求工厂，以便分别配置连接和读取超时
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 设置建立到 JUMP 服务 TCP 连接的最长等待时间
        requestFactory.setConnectTimeout(RecipeChangeConsumerConstants.CONNECT_TIMEOUT_MILLIS);
        // 设置连接建立后等待 JUMP 响应数据的最长时间
        requestFactory.setReadTimeout(RecipeChangeConsumerConstants.READ_TIMEOUT_MILLIS);
        // 使用上述请求工厂创建 RestTemplate，避免影响 MES 应用已有的 RestTemplate Bean
        return new RestTemplate(requestFactory);
    }

    /**
     * 注册消息监听器，MES 项目必须提供工艺变更处理器和钉钉告警服务
     *
     * @param recipeChangeMessageProcessorProvider MES 本地工艺变更业务处理器提供器
     * @param recipeChangeJUMPClient JUMP 状态查询和处理结果回调客户端
     * @param recipeChangeDingTalkAlarmProcessorProvider MES 钉钉告警处理器提供器
     * @param recipeChangeConsumerProperties Starter 运行配置
     * @return 用于监听车间专属工艺变更队列的消息监听器
     */
    @Bean
    public RecipeChangeMessageListener recipeChangeMessageListener(ObjectProvider<RecipeChangeMessageProcessor> recipeChangeMessageProcessorProvider,
                                                                   RecipeChangeJUMPClient recipeChangeJUMPClient,
                                                                   ObjectProvider<RecipeChangeDingTalkAlarmProcessor> recipeChangeDingTalkAlarmProcessorProvider,
                                                                   RecipeChangeConsumerProperties recipeChangeConsumerProperties) {
        // 延迟获取 MES 业务处理器，使 Starter 工程自身不会因缺少业务实现产生 IDEA 注入警告
        RecipeChangeMessageProcessor recipeChangeMessageProcessor = recipeChangeMessageProcessorProvider.getIfAvailable();
        // 消费功能启用后没有业务处理器时直接终止启动，防止消息被错误消费或确认
        if (recipeChangeMessageProcessor == null) {
            throw new IllegalStateException("启用工艺变更消费者时必须提供 RecipeChangeMessageProcessor 实现");
        }
        // 延迟获取 MES 钉钉告警实现，使 Starter 工程自身不会因缺少业务实现产生 IDEA 注入警告
        RecipeChangeDingTalkAlarmProcessor recipeChangeDingTalkAlarmProcessor = recipeChangeDingTalkAlarmProcessorProvider.getIfAvailable();
        // 消费功能启用后没有钉钉告警实现时终止启动，确保车间处理失败能够按要求触发钉钉告警
        if (recipeChangeDingTalkAlarmProcessor == null) {
            throw new IllegalStateException("启用工艺变更消费者时必须提供 RecipeChangeDingTalkAlarmProcessor 实现");
        }
        // 创建监听器并注入业务处理、JUMP 通信、必需告警实现和运行配置
        return new RecipeChangeMessageListener(recipeChangeMessageProcessor, recipeChangeJUMPClient, recipeChangeDingTalkAlarmProcessor, recipeChangeConsumerProperties);
    }

}
