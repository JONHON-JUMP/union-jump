package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/** 单道工序及该工序下的检验项。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Operation {
    /**
     * 工序名称
     */
    private String opName;
    /**
     * 工序编号
     */
    private String opNumber;
    /**
     * 工序的大版本号，按 MPM 返回的字符串保留。
     */
    private String opVersion;
    /**
     * 工序的小版本号，按 MPM 返回的字符串保留。
     */
    private String opIteration;
    /** 工序检验类型，例如自检、专检。 */
    private String opInspectionType;
    /** 工序检验时机；接口未提供时为 null。 */
    private String opInspectionTiming;
    /** 工序检验要求。 */
    private String opJyyq;
    /**
     * 属于当前工序的检验项列表。
     */
    private List<InspectionItem> inspectionItems;
    /** 工序直接关联的参数表，与检验项内的参数表分开。 */
    private List<Parameter> parameters;
}
