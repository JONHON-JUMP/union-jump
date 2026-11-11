# jonhonjump-spring-boot-starter-recipe-change-consumer

## 简介

工艺变更消息消费者 Spring Boot Starter，供 MES 项目接收 JUMP 按车间分发的工艺变更消息。

Starter 使用内置的 JUMP 通信参数、RabbitMQ 连接参数和主队列命名规则；这些基础设施参数随 Starter jar 统一发布。MES 项目只需声明本实例负责的车间，不需要配置 JUMP 地址、HTTP 超时、RabbitMQ 信息、交换机、主队列或延迟重试队列。

RabbitMQ 交换机、主队列、延迟重试队列、绑定和延迟时间均由 JUMP 注册和管理。Starter 只监听车间主队列；消费失败时执行 NACK，由 JUMP 已注册的死信和延迟队列拓扑完成后续重试。

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
      workshop-codes:
        - "5601"
        - "5600"
```

`workshop-codes` 为必填列表。Starter 会根据内置的主队列命名规则监听对应车间的队列，MES 本地 `RecipeChangeMessageProcessor` 可根据消息内的 `workshopCode` 继续路由不同车间业务。

JUMP 或 RabbitMQ 基础设施参数发生变化时，需要重新发布 Starter jar 并由 MES 替换依赖；无需修改 MES 项目的 YAML。
