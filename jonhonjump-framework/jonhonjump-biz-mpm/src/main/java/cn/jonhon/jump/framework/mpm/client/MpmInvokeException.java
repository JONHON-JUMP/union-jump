package cn.jonhon.jump.framework.mpm.client;

/**
 * 接口平台调用 MPM 失败，或未取得可解析的 MPM 业务响应时抛出的异常。
 *
 * <p>{@link #getCode()} 保存接口平台返回的状态码；平台没有返回状态码时为 {@code null}。
 * 该状态码与 MPM 业务响应 DTO 中的 {@code code} 不是同一个字段。
 */
public class MpmInvokeException extends RuntimeException {

    /** 接口平台返回的状态码；平台无结果或反射调用异常时可能为空。 */
    private final String code;

    /**
     * 创建调用异常。
     *
     * @param code 接口平台状态码；未取得状态码时传 {@code null}
     * @param message 供 MES 日志和异常处理使用的错误说明
     */
    public MpmInvokeException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取接口平台返回的状态码。
     *
     * @return 状态码；接口平台未返回状态码时为 {@code null}
     */
    public String getCode() {
        return code;
    }
}
