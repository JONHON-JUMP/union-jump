package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 单个检验项及其关联的参数表。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InspectionItem {
    /**
     * 检验项目编号
     */
    private String numberInsp;
    /**
     * 检验项名称
     */
    private String name;
    /**
     * 检验项的大版本号。
     */
    private String version;
    /**
     * 检验项的小版本号。
     */
    private String iteration;
    /**
     * MPM 返回的内容判定依据。
     */
    private String contentCriteria;
    /**
     * MPM 返回的检验方法。
     */
    private String inspectionMethod;
    /**
     * 备注
     */
    private String remark;
    /**
     * 检验频次
     */
    private String inspectionFrequency;
    /**
     * 检验类型
     */
    private String inspectionType;
    /** 检验项的检验时机；接口未提供时为 null。 */
    private String inspectionTiming;
    /**
     * MPM 返回的检验要求描述。
     */
    private String requirement;
    /**
     * 当前检验项关联的参数表列表。
     */
    private List<Parameter> parameters;
    /** 检验项在当前工序中的顺序。 */
    @JsonProperty("iSequence")
    private Integer iSequence;
}
