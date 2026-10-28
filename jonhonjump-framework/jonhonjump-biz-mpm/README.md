# 各车间 MES 使用 MPM 检验项及参数查询 Jar

## 一、前置条件：接入接口平台

各车间 MES 必须先引入接口平台 Jar `jump-api-route`，并在 `application.yml` 中完成接口平台配置。具体依赖和配置方法请联系王豪威。完成后，再接入下述 MPM Jar。

调用链路：**MES 业务代码 → MPM Jar→ 接口平台 Jar（路由调用）→ 接口平台服务（转发请求）→ MPM 接口**。返回结果按相反方向传回，MPM Jar 将响应解析为 DTO 后交给 MES。

## 二、引入 MPM Jar

根据 MES 的依赖管理方式，选择以下一种方式引入 MPM Jar。

**有 Maven 私库：**在 MES 项目的 `pom.xml` 中直接引入坐标，版本号使用私库中发布的版本：

```xml
<dependency>
    <groupId>cn.jonhon.jump.boot</groupId>
    <artifactId>jonhonjump-biz-mpm</artifactId>
    <version>2026.10.23.1</version>
</dependency>
```

**没有 Maven 私库：**在 MES 项目的 `pom.xml` 中通过 `systemPath` 引入提供的 MPM Jar。实际 Jar 路径由各车间 MES 自行设置，不在本说明中指定：

```xml
<dependency>
    <groupId>cn.jonhon.jump.boot</groupId>
    <artifactId>jonhonjump-biz-mpm</artifactId>
    <version>2026.10.23.1</version>
    <scope>system</scope>
    <systemPath>${mpm.jar.path}</systemPath>
</dependency>
```

各 MES 自行定义 `mpm.jar.path`，使其指向本机 MPM Jar 文件。MPM Jar 的 Maven POM 不声明接口平台 Jar 依赖；接口平台 Jar 及其所需依赖须按前置条件独立引入。

## 三、调用方式

在 MES 的 Spring 业务类中使用 `@Autowired` 注入 `MpmClient`，然后调用：

```java
import cn.jonhon.jump.framework.mpm.client.MpmClient;
import cn.jonhon.jump.framework.mpm.dto.MpmInspectionRequestDTO;
import cn.jonhon.jump.framework.mpm.dto.MpmInspectionReponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MesInspectionService {

    @Autowired
    private MpmClient mpmClient;

    public MpmInspectionReponseDTO query(MpmInspectionRequestDTO request) {
        return mpmClient.queryOpInspectionAndParams(request);
    }
}
```

- `request` 类型：`cn.jonhon.jump.framework.mpm.dto.MpmInspectionRequestDTO`。
- 请求字段：`ppNumber`（二级工艺计划编号，必填）、`opNumber`（二级工序编号；传 `null`、空字符串或空白字符串时，Jar 按 `""` 发送，查询整本工艺的所有工序）。
- 返回类型：`cn.jonhon.jump.framework.mpm.dto.MpmInspectionReponseDTO`，已解析为 Java 对象。

MPM Jar 通过 Spring Boot 自动配置创建 `MpmClient`。业务类需要由 Spring 管理，例如标注 `@Service`；仅声明 `private MpmClient mpmClient;` 不会自动注入，需像上面一样加 `@Autowired`。无需手动 `new`。

## 四、调用结果

| 字段或结果 | 含义 | MES 处理方式 |
| --- | --- | --- |
| `success` | MPM 业务是否成功 | 按业务规则判断成功或失败 |
| `code`、`message` | MPM 业务结果码和说明 | 失败时用于提示或记录 |
| `result` | 工序列表；内含检验项、参数、列及参数行；参数行类型为 `List<Map<String, String>>` | 直接通过 DTO 的 getter 读取 |
| `token`、`notifyIcon` | MPM 响应中的其他字段 | 按需读取 |
| `MpmInvokeException` | 接口平台调用失败，或未取得有效的 MPM 响应 | 捕获异常；`getCode()` 可读取平台结果码 |

接口平台调用成功只表示请求到达 MPM；MPM 业务是否成功以返回 DTO 的 `success`、`code` 为准。
