package cn.jonhon.jump.module.rm.recipechange.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工艺变更 RabbitMQ 基础设施配置
 */
@Configuration
@EnableConfigurationProperties(RecipeChangeRabbitMQProperties.class)
public class RecipeChangeRabbitMQConfig {

    /**
     * 创建仅供工艺变更分发使用的 RabbitMQ 连接工厂
     *
     * @param properties 工艺变更 RabbitMQ 配置属性
     * @return 工艺变更专用 RabbitMQ 连接工厂
     */
    @Bean("recipeChangeConnectionFactory")
    public ConnectionFactory recipeChangeConnectionFactory(RecipeChangeRabbitMQProperties properties) {
        // 创建具备连接复用能力的工艺变更专用连接工厂，不影响系统全局 ConnectionFactory
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        // 设置工艺变更专用 RabbitMQ 服务地址
        connectionFactory.setHost(properties.getHost());
        // 设置工艺变更专用 RabbitMQ 服务端口
        connectionFactory.setPort(properties.getPort());
        // 设置工艺变更专用 RabbitMQ 登录用户名
        connectionFactory.setUsername(properties.getUsername());
        // 设置工艺变更专用 RabbitMQ 登录密码
        connectionFactory.setPassword(properties.getPassword());
        // 设置工艺变更专用 RabbitMQ 虚拟主机
        connectionFactory.setVirtualHost(properties.getVirtualHost());
        // 开启关联确认，分发服务可据此等待本次消息的 Broker 发布确认
        connectionFactory.setPublisherConfirmType(properties.getPublisherConfirmType());
        // 开启发送端返回能力，与 mandatory 配合感知无法路由的消息
        connectionFactory.setPublisherReturns(properties.isPublisherReturns());
        return connectionFactory;
    }

    /**
     * 创建仅供工艺变更消息分发使用的 RabbitTemplate
     *
     * @param recipeChangeConnectionFactory 工艺变更专用 RabbitMQ 连接工厂
     * @param recipeChangeMessageConverter 工艺变更专用 JSON 消息转换器
     * @param properties 工艺变更 RabbitMQ 配置属性
     * @return 工艺变更专用 RabbitMQ 消息发送模板
     */
    @Bean("recipeChangeRabbitTemplate")
    public RabbitTemplate recipeChangeRabbitTemplate(
            @Qualifier("recipeChangeConnectionFactory") ConnectionFactory recipeChangeConnectionFactory,
            @Qualifier("recipeChangeMessageConverter") MessageConverter recipeChangeMessageConverter,
            RecipeChangeRabbitMQProperties properties) {
        // 仅使用工艺变更专用连接工厂创建发送模板，避免复用系统其他业务的 RabbitTemplate
        RabbitTemplate recipeChangeRabbitTemplate = new RabbitTemplate(recipeChangeConnectionFactory);
        // 使用工艺变更专用 JSON 转换器，确保消息格式与 MES Starter 的 JSON 消费转换器一致
        recipeChangeRabbitTemplate.setMessageConverter(recipeChangeMessageConverter);
        // 无法路由时将消息返回发送端，使分发服务能够识别投递异常
        recipeChangeRabbitTemplate.setMandatory(properties.isMandatory());
        return recipeChangeRabbitTemplate;
    }

    /**
     * 创建仅供工艺变更消息发送使用的 JSON 消息转换器
     *
     * @return 工艺变更专用 JSON 消息转换器
     */
    @Bean("recipeChangeMessageConverter")
    public MessageConverter recipeChangeMessageConverter() {
        // 独立创建 Jackson 转换器，避免使用或修改系统其他 RabbitMQ 业务的消息转换器
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建仅用于工艺变更 Exchange、队列和绑定声明的 RabbitAdmin
     *
     * @param recipeChangeConnectionFactory 工艺变更专用 RabbitMQ 连接工厂
     * @return 工艺变更专用 RabbitMQ 管理对象
     */
    @Bean("recipeChangeAmqpAdmin")
    public RabbitAdmin recipeChangeAmqpAdmin(
            @Qualifier("recipeChangeConnectionFactory") ConnectionFactory recipeChangeConnectionFactory) {
        // 使用工艺变更专用连接声明基础设施，避免在系统其他 RabbitMQ Broker 上创建队列和交换机
        return new RabbitAdmin(recipeChangeConnectionFactory);
    }

    /**
     * 声明工艺变更直连交换机
     *
     * JUMP 在首次向某车间分发通知前动态声明该车间的主队列和绑定
     *
     * @param properties 工艺变更 RabbitMQ 配置属性
     * @return 工艺变更直连交换机
     */
    @Bean
    public DirectExchange recipeChangeDirectExchange(RecipeChangeRabbitMQProperties properties,
                                                     @Qualifier("recipeChangeAmqpAdmin") RabbitAdmin recipeChangeAmqpAdmin) {
        // 第一个参数读取配置中的 Exchange 名称，保证发送端和 MES Starter 使用同一个名称
        // 第二个参数 true 表示 Exchange 持久化，RabbitMQ 重启后仍会保留
        // 第三个参数 false 表示没有队列绑定时不自动删除 Exchange，等待 MES 后续接入
        DirectExchange recipeChangeDirectExchange = new DirectExchange(properties.getExchange(), true, false);
        // 限定该 Exchange 仅由工艺变更专用 RabbitAdmin 声明，避免系统全局 RabbitAdmin 重复或跨 Broker 声明
        recipeChangeDirectExchange.setAdminsThatShouldDeclare(recipeChangeAmqpAdmin);
        return recipeChangeDirectExchange;
    }

    /**
     * 声明工艺变更延迟重试直连交换机
     *
     * 主队列拒绝车间处理失败消息时，RabbitMQ 将消息死信到该交换机
     * 延迟重试队列的 TTL 到期后会再将消息死信回正常工艺变更交换机
     *
     * @param properties 工艺变更 RabbitMQ 配置属性
     * @return 工艺变更延迟重试直连交换机
     */
    @Bean
    public DirectExchange recipeChangeRetryDirectExchange(RecipeChangeRabbitMQProperties properties,
                                                          @Qualifier("recipeChangeAmqpAdmin") RabbitAdmin recipeChangeAmqpAdmin) {
        // 创建持久化且不自动删除的延迟重试交换机，保证 Broker 重启后重试基础设施仍然存在
        DirectExchange recipeChangeRetryDirectExchange = new DirectExchange(properties.getRetryExchange(), true, false);
        // 限定该 Exchange 仅由工艺变更专用 RabbitAdmin 声明，避免系统全局 RabbitAdmin 重复或跨 Broker 声明
        recipeChangeRetryDirectExchange.setAdminsThatShouldDeclare(recipeChangeAmqpAdmin);
        return recipeChangeRetryDirectExchange;
    }

}
