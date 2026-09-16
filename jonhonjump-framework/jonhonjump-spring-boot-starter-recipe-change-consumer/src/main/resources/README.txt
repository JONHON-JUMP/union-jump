# jonhonjump-spring-boot-starter-recipe-change-consumer

## 简介

工艺变更消息消费者 Spring Boot Starter，供 MES 项目接收 JUMP 按车间分发的工艺变更消息。

Starter 使用独立的 RabbitMQ 连接和消息拓扑，不复用 MES 项目的 `spring.rabbitmq` 配置。消息消费采用手动确认模式，处理流程如下：

1. 从配置的全部车间工艺变更主队列消费消息；
2. 调用 JUMP 领取处理权；
3. JUMP 返回“无需处理”时，直接确认消息；
4. 领取成功后调用 MES 提供的本地工艺变更处理器；
5. 将处理成功或失败结果回调 JUMP；
6. 处理失败时调用 MES 提供的钉钉告警处理器，并拒绝消息进入车间专属延迟重试队列；
7. 延迟队列消息到期后回流主队列，再次消费。

MES 项目启用 Starter 后，必须提供 `RecipeChangeMessageProcessor` 和 `RecipeChangeDingTalkAlarmProcessor` 的 Bean；否则应用启动时会明确报错。

## Maven 坐标

```xml
<dependency>
    <groupId>cn.jonhon.jump.boot</groupId>
    <artifactId>jonhonjump-spring-boot-starter-recipe-change-consumer</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

```yaml
jonhonjump:
  recipe-change:
    consumer:
      # 是否启用工艺变更消息消费，默认 false
      enabled: true

      # 当前 MES 需要消费的车间；配置几个，就监听几个车间的专属主队列
      # 不再支持 workshop-code 单数配置
      workshop-codes:
        - "3500"
        - "5600"

      # JUMP 服务地址，不要以 / 结尾
      jump-base-url: http://127.0.0.1:48080
      connect-timeout-millis: 10000
      read-timeout-millis: 20000

      # 必须与 JUMP 使用的主交换机、队列和路由键保持一致
      exchange: EXCHANGE_RECIPE_CHANGE
      queue-name-prefix: QUEUE_RECIPE_CHANGE_
      routing-key-prefix: RECIPE_CHANGE_ROUTE_

      # 失败消息的延迟重试拓扑
      retry-exchange: EXCHANGE_RECIPE_CHANGE_RETRY
      retry-routing-key-prefix: RECIPE_CHANGE_RETRY_ROUTE_
      retry-queue-name-prefix: QUEUE_RECIPE_CHANGE_RETRY_
      retry-delay-millis: 1200000

      # 可选：按车间覆盖重试队列版本和延迟时间；未配置的车间使用上方全局前缀和 TTL
      retry-queue-overrides:
        "3500":
          queue-name-prefix: QUEUE_RECIPE_CHANGE_RETRY_V2_
          delay-millis: 60000
        "5600":
          queue-name-prefix: QUEUE_RECIPE_CHANGE_RETRY_V3_
          delay-millis: 300000

      # 工艺变更专用 RabbitMQ 连接；不读取 spring.rabbitmq
      rabbitmq:
        host: 127.0.0.1
        port: 5672
        username: guest
        password: guest
        virtual-host: /
```

`workshop-codes` 为必填列表，Starter 会为每个车间声明主队列、延迟重试队列及对应绑定，并由同一个消息监听器消费全部主队列。MES 本地 `RecipeChangeMessageProcessor` 可根据消息内的 `workshopCode` 继续路由不同车间业务。

全局 `retry-queue-name-prefix` 和 `retry-delay-millis` 是默认值；`retry-queue-overrides` 可为已配置的单个车间覆盖延迟队列前缀和 TTL。JUMP 与 MES Starter 对同一车间的最终队列名称和 TTL 必须一致。若 JUMP 为某车间切换到版本化延迟队列，例如 `QUEUE_RECIPE_CHANGE_RETRY_V2_`，MES 也必须同步使用相同前缀和延迟时间。

延迟重试队列使用 `x-message-ttl`。RabbitMQ 不允许修改已有队列的该参数；调整某车间的延迟时间时，应为该车间切换新的延迟队列前缀版本。旧延迟队列只解除旧绑定、不删除，让其中已有消息自然 TTL 回流。
