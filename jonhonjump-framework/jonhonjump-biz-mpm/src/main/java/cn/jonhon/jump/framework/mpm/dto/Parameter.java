package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** 检验项关联的一张参数表，包含列定义和数据行。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter {
    /**
     * 参数表名称
     */
    private String name;
    /**
     * 参数表编号
     */
    private String numberParam;
    /**
     * 参数表的列定义，用于解释 {@code rows} 中的字段。
     */
    private List<Column> columns;
    /**
     * 参数表的数据行。每个 Map 表示一行，键是参数字段名，值是该字段的字符串值。
     * 接口文档将 {@code rSequence} 定义为整数；为满足 MES 对参数行统一使用字符串值的要求，
     * 该值在此 Map 中以字符串形式读取。
     */
    private List<Map<String, String>> rows;
    /** 参数表顺序。 */
    @JsonProperty("pSequence")
    private Integer pSequence;
}
