package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * MPM 工序检验项及参数接口的完整业务响应。
 *
 * <p>接口平台调用成功后，客户端将平台结果中的 JSON 数据解析为本对象。
 * 数据按“工序 → 检验项 → 参数表 → 参数列和参数行”组织，
 * MES 可以直接读取对应字段，无需再次处理 {@code JsonNode}。
 * {@link JsonIgnoreProperties} 允许 MPM 后续增加字段而不影响当前字段的解析。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmInspectionReponseDTO {
    /**
     * MPM 业务处理是否成功；与接口平台调用状态分别判断。
     */
    private Boolean success;
    /**
     * MPM 业务状态码；{@code 200} 表示 MPM 业务处理成功。
     */
    private String code;
    /**
     * MPM 返回的业务提示或错误信息。
     */
    private String message;
    /**
     * 查询结果中的工序列表；未指定工序编号时可包含整本工艺的多个工序。
     */
    private List<Operation> result;
    /** MPM 响应中的 token 字段，按接口原值保留。 */
    private String token;
    /** MPM 响应中的 notifyIcon 字段；具体结构由 MPM 返回内容决定。 */
    private Object notifyIcon;
}
