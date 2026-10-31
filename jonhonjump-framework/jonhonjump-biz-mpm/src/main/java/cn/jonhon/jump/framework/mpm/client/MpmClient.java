package cn.jonhon.jump.framework.mpm.client;

import cn.jonhon.jump.framework.mpm.dto.MpmInspectionRequestDTO;
import cn.jonhon.jump.framework.mpm.dto.MpmInspectionReponseDTO;
import cn.jonhon.jump.framework.mpm.dto.MpmHistoryInspectionRequestDTO;

/**
 * MES 调用 MPM 工序检验项及参数接口的业务入口。
 *
 * <p>由 {@code MpmAutoConfiguration} 注册为 Spring Bean，MES 可直接注入本接口。
 * 调用所需的接口平台 Jar 由 MES 工程单独引入；本模块不依赖其编译期类型。
 */
public interface MpmClient {

    /**
     * 根据二级工艺计划编号和可选的工序编号查询检验项及参数。
     *
     * <p>{@code opNumber} 为 {@code null}、空字符串或仅包含空白字符时，
     * 调用前会转换为空字符串，表示查询整本工艺的所有工序。
     * 方法直接返回已解析的 MPM 完整业务响应，无需 MES 再解析 JSON。
     *
     * @param request 查询条件；不能为空，且 {@code ppNumber} 和 {@code opNumber} 不能同时为空
     * @return 包含工序、检验项和参数信息的 MPM 业务响应
     * @throws IllegalArgumentException 查询条件为空或两个编号同时为空时抛出
     * @throws MpmInvokeException 接口平台调用失败、返回数据为空或响应无法解析时抛出
     */
    MpmInspectionReponseDTO queryOpInspectionAndParams(MpmInspectionRequestDTO request);

    /**
     * 按二级工艺计划编号和版本查询历史检验项及参数。
     *
     * @param request 历史查询条件；工艺计划编号和版本必填，工序编号可为空
     * @return 与最新版本接口相同结构的完整 MPM 业务响应
     * @throws IllegalArgumentException 请求为空或必填字段为空白时抛出
     * @throws MpmInvokeException 接口平台调用失败或响应无法解析时抛出
     */
    MpmInspectionReponseDTO queryHistoryOpInspectionAndParams(MpmHistoryInspectionRequestDTO request);
}
