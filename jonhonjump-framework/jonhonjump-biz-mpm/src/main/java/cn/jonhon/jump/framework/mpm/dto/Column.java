package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 参数表中的单个列定义。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Column {
    /**
     * 列显示名称
     */
    private String name;
    /**
     * 参数列的内部字段名，可用于识别数据行中的对应值。
     */
    private String columnName;
    /**
     * 是否为检验参数：{@code 0} 表示否，{@code 1} 表示是。
     */
    private Integer isInspection;
    /**
     * 参数单位
     */
    private String unit;
    /** 新响应中的参数列顺序。 */
    @JsonProperty("cSequence")
    private Integer cSequence;
}
