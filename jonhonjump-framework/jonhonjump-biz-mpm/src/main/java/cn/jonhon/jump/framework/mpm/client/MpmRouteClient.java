package cn.jonhon.jump.framework.mpm.client;

import cn.jonhon.jump.framework.mpm.dto.MpmInspectionRequestDTO;
import cn.jonhon.jump.framework.mpm.dto.MpmInspectionReponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@link MpmClient} 的默认实现，通过 MES 独立引入的接口平台 Jar 调用 MPM。
 *
 * <p>先校验并补齐请求参数，再调用接口平台；只有平台返回成功且携带数据时，
 * 才将平台结果中的 JSON 数据解析为 {@link MpmInspectionReponseDTO}。
 * 本类在编译期不引用接口平台类型，接口平台 Jar 由使用方 MES 单独提供。
 */
public class MpmRouteClient implements MpmClient {

    /** 解析接口平台返回的 MPM JSON 数据。 */
    private final ObjectMapper objectMapper;
    /** 调用接口平台的适配入口。 */
    private final RouteGateway routeGateway;

    /**
     * 创建使用默认接口平台调用方式的客户端。
     *
     * @param objectMapper 用于解析 MPM 业务响应的 Jackson 映射器
     */
    public MpmRouteClient(ObjectMapper objectMapper) {
        this(objectMapper, new ReflectiveRouteGateway());
    }

    /**
     * 创建指定调用入口的客户端；用于隔离业务处理与接口平台调用。
     *
     * @param objectMapper 解析 MPM 业务响应的 Jackson 映射器
     * @param routeGateway 接口平台调用入口
     */
    MpmRouteClient(ObjectMapper objectMapper, RouteGateway routeGateway) {
        this.objectMapper = objectMapper;
        this.routeGateway = routeGateway;
    }

    @Override
    public MpmInspectionReponseDTO queryOpInspectionAndParams(MpmInspectionRequestDTO request) {
        if (request == null || (isBlank(request.getPpNumber()) && isBlank(request.getOpNumber()))) {
            throw new IllegalArgumentException("ppNumber 和 opNumber 不能同时为空");
        }
        // 平台接口要求传入 opNumber；未指定工序时发送空字符串以查询所有工序。
        MpmInspectionRequestDTO routeRequest = isBlank(request.getOpNumber())
                ? new MpmInspectionRequestDTO(request.getPpNumber(), "") : request;
        // 先检查平台调用结果，再解析其中的 MPM 业务响应，避免把平台错误当作业务数据。
        PlatformResult result = routeGateway.invoke(routeRequest);
        if (result == null) {
            throw new MpmInvokeException(null, "接口平台未返回调用结果");
        }
        if (!"200".equals(result.code)) {
            throw new MpmInvokeException(result.code, "接口平台调用 MPM 失败: " + result.message);
        }
        if (isBlank(result.data)) {
            throw new MpmInvokeException(result.code, "MPM 响应为空");
        }
        try {
            return objectMapper.readValue(result.data, MpmInspectionReponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new MpmInvokeException(result.code, "MPM 响应不是预期的 JSON 对象");
        }
    }

    /**
     * 判断字符串是否未提供有效内容。
     *
     * @param value 待判断的字符串
     * @return {@code null}、空字符串或仅含空白字符时返回 {@code true}
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 将接口平台调用细节与请求校验、响应解析逻辑隔离。 */
    interface RouteGateway {
        /**
         * 发起接口平台调用。
         *
         * @param request 已校验且补齐 {@code opNumber} 的请求
         * @return 平台调用结果；平台未返回对象时可能为 {@code null}
         */
        PlatformResult invoke(MpmInspectionRequestDTO request);
    }

    /** 接口平台结果中当前客户端使用的状态码、提示信息和业务数据。 */
    static final class PlatformResult {
        /** 接口平台调用状态码，成功值为 {@code 200}。 */
        final String code;
        /** 接口平台返回的提示或错误信息。 */
        final String message;
        /** MPM 业务响应的 JSON 字符串。 */
        final String data;

        /** 创建从接口平台结果中提取出的字段集合。 */
        PlatformResult(String code, String message, String data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

    /**
     * 运行时通过反射调用接口平台 Jar。
     *
     * <p>在 MES 实际调用时才加载平台类与调用标识，避免 MPM Jar 在 POM 中引入平台依赖。
     * 若平台 Jar 缺失或签名不匹配，转换为 {@link MpmInvokeException} 供 MES 处理。
     */
    private static final class ReflectiveRouteGateway implements RouteGateway {

        /**
         * 调用接口平台，并提取状态码、消息和 MPM 业务数据。
         *
         * @param request 已补齐工序编号的 MPM 请求对象
         * @return 接口平台结果；平台方法直接返回 {@code null} 时也返回 {@code null}
         * @throws MpmInvokeException 接口平台类或方法缺失、调用异常或无法访问时抛出
         */
        @Override
        public PlatformResult invoke(MpmInspectionRequestDTO request) {
            try {
                Class<?> idType = Class.forName("cn.jonhon.route.InvokeId");
                Object invokeId = idType.getField("QueryOpInspectionAndParamsV1").get(null);
                Class<?> serviceType = Class.forName("cn.jonhon.route.ThirdPartyRouteService");
                Method invoke = serviceType.getMethod("invoke", idType, Object.class);
                Object response = invoke.invoke(null, invokeId, request);
                if (response == null) {
                    return null;
                }
                Class<?> resultType = response.getClass();
                return new PlatformResult(
                        (String) resultType.getMethod("getCode").invoke(response),
                        (String) resultType.getMethod("getMsg").invoke(response),
                        (String) resultType.getMethod("getData").invoke(response));
            } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException e) {
                throw new MpmInvokeException(null, "缺少接口平台 Jar，请先引入，引入方式请联系王豪威");
            } catch (InvocationTargetException e) {
                throw new MpmInvokeException(null, "接口平台调用异常: " + e.getTargetException().getMessage());
            } catch (IllegalAccessException e) {
                throw new MpmInvokeException(null, "无法访问接口平台 Jar 的调用方法");
            }
        }
    }
}
