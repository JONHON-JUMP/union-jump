package cn.jonhon.jump.framework.mpm.dto;

/**
 * MPM 工序检验项及参数接口的查询条件。
 *
 * <p>MES 创建本对象后传给 {@code MpmClient.queryOpInspectionAndParams}。
 * {@code ppNumber} 和 {@code opNumber} 不能同时为空；不指定 {@code opNumber} 时查询整本工艺的所有工序。
 */
public class MpmInspectionRequestDTO {

    /** 二级工艺计划编号；与工序编号不能同时为空。 */
    private String ppNumber;
    /** 二级工序编号；为空白时客户端按空字符串发送，查询整本工艺的所有工序。 */
    private String opNumber;

    /** 无参构造方法，供 JSON 框架或逐项设置字段时使用。 */
    public MpmInspectionRequestDTO() {
    }

    /**
     * 使用两个查询条件创建请求。
     *
     * @param ppNumber 二级工艺计划编号；与工序编号不能同时为空
     * @param opNumber 二级工序编号；不指定时可传 {@code null} 或空字符串
     */
    public MpmInspectionRequestDTO(String ppNumber, String opNumber) {
        this.ppNumber = ppNumber;
        this.opNumber = opNumber;
    }

    /** @return 二级工艺计划编号 */
    public String getPpNumber() {
        return ppNumber;
    }

    /** @param ppNumber 二级工艺计划编号；与工序编号不能同时为空 */
    public void setPpNumber(String ppNumber) {
        this.ppNumber = ppNumber;
    }

    /** @return 二级工序编号；未设置时可能为 {@code null} */
    public String getOpNumber() {
        return opNumber;
    }

    /** @param opNumber 二级工序编号；为空白时查询整本工艺的所有工序 */
    public void setOpNumber(String opNumber) {
        this.opNumber = opNumber;
    }
}
