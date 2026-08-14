package cn.jonhon.jump.framework.portalsubsystem.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 主系统统一响应体（芋道 CommonResult）。
 */
@Data
public class PortalApiResult<T> {

    private Integer code;
    private String msg;
    private T data;

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    public static <T> T extractData(PortalApiResult<T> result, String action) {
        if (result == null) {
            throw new cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException(action + "失败：响应为空");
        }
        if (!result.isSuccess()) {
            throw new cn.jonhon.jump.framework.portalsubsystem.exception.PortalSubSystemException(
                    action + "失败：" + result.getMsg());
        }
        return result.getData();
    }

    public static String textValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

}
