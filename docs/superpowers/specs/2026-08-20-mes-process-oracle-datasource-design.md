# MES Process Oracle 从库与空 Mapper 设计

## 目标

为 MES 模块的工艺 `process` 包增加一个独立的 Oracle 动态数据源，并建立面向 `CAOE_PP` 表的 Mapper 扩展点。当前表结构未知，因此本次不定义字段映射、不编写 SQL，也不增加任何 Controller、Service 或业务功能。

## 数据源设计

在以下配置文件的 `spring.datasource.dynamic.datasource` 下新增名为 `oracle` 的数据源：

- `jonhonjump-server/src/main/resources/application-local.yaml`
- `jonhonjump-server/src/main/resources/application-dev.yaml`

两个环境使用一致的配置结构：

```yaml
oracle:
  lazy: true
  driver-class-name: oracle.jdbc.OracleDriver
  url: ${ORACLE_DATASOURCE_URL:}
  username: ${ORACLE_DATASOURCE_USERNAME:}
  password: ${ORACLE_DATASOURCE_PASSWORD:}
```

连接信息仅通过环境变量注入，不向仓库写入真实地址或凭据。`lazy: true` 使应用启动时不主动建立 Oracle 连接；现有 `master` 和 `slave` 数据源保持不变。

项目当前没有声明 Oracle JDBC 驱动。本次增加兼容 Java 8 的 `com.oracle.database.jdbc:ojdbc8` 依赖，保证未来首次调用 Oracle Mapper 时能够加载 `oracle.jdbc.OracleDriver`。

## Mapper 设计

在 `cn.jonhon.jump.module.mes.process.dal.oracle` 包中新建 `CaoePpMapper`：

```java
@Mapper
@DS("oracle")
public interface CaoePpMapper {
}
```

`@Mapper` 让 MyBatis 注册该接口，类级 `@DS("oracle")` 让未来新增到该 Mapper 的方法默认路由至 Oracle 数据源。接口暂时不继承 `BaseMapperX`，因为尚无 `CAOE_PP` 对应的数据对象类型；也不创建 Mapper XML，避免在表结构和查询需求未知时预设字段或 SQL。

## 运行行为与错误边界

- 未配置 Oracle 环境变量且没有调用该 Mapper 时，懒加载数据源不应影响应用启动。
- 后续调用 Mapper 前必须配置三个 Oracle 环境变量；连接失败由现有动态数据源和连接池机制报告。
- 当前 Mapper 没有方法，因此本次不存在查询、写入、事务或跨数据源事务行为。
- 数据源名称固定为 `oracle`，与 `@DS("oracle")` 完全一致。

## 验证

本次只增加配置、依赖和空接口，验证范围如下：

1. 静态检查 local/dev 两个配置文件均包含名称、懒加载、驱动和三个环境变量占位。
2. 静态检查 `CaoePpMapper` 同时包含 `@Mapper` 和 `@DS("oracle")`，且没有方法、DO、XML、Service 或 Controller。
3. 编译 MES 模块及其依赖，确认 Oracle 驱动依赖可解析、Mapper 注解可编译。
4. 运行 `git diff --check`，确认没有新增空白错误。

由于没有真实 Oracle 连接信息、表结构和 Mapper 方法，本次不执行数据库连通性测试，也不伪造查询测试。

## 范围约束

- 不修改现有 `master`、`slave` 的行为。
- 不新增或修改业务接口、Service、查询逻辑和页面。
- 不定义 `CAOE_PP` 字段、主键或实体映射。
- 不新增 Mapper 方法或 SQL。
- 不保存任何真实 Oracle 凭据。
