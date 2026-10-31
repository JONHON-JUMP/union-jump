package cn.jonhon.jump.framework.mpm.dto;

/**
 * 历史工序检验项及参数查询条件。
 *
 * <p>接口字段为 {@code ppNumber}、{@code opNumber} 和小写的 {@code version}。
 * 工艺计划编号和版本必填，工序编号为空时查询该版本的全部工序。
 */
public class MpmHistoryInspectionRequestDTO {

    /** 二级工艺计划编号。 */
    private String ppNumber;
    /** 二级工序编号；不指定时按空字符串发送。 */
    private String opNumber;
    /** 二级工艺版本。 */
    private String version;

    public MpmHistoryInspectionRequestDTO() {
    }

    public MpmHistoryInspectionRequestDTO(String ppNumber, String opNumber, String version) {
        this.ppNumber = ppNumber;
        this.opNumber = opNumber;
        this.version = version;
    }

    /** @return 二级工艺计划编号 */
    public String getPpNumber() {
        return ppNumber;
    }

    /** @param ppNumber 二级工艺计划编号，不能为空白 */
    public void setPpNumber(String ppNumber) {
        this.ppNumber = ppNumber;
    }

    /** @return 二级工序编号 */
    public String getOpNumber() {
        return opNumber;
    }

    /** @param opNumber 二级工序编号；可为空 */
    public void setOpNumber(String opNumber) {
        this.opNumber = opNumber;
    }

    /** @return 二级工艺版本 */
    public String getVersion() {
        return version;
    }

    /** @param version 二级工艺版本，不能为空白 */
    public void setVersion(String version) {
        this.version = version;
    }
}
