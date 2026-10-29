package cn.jonhon.jump.framework.mpm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MPM 工序检验项及参数接口的完整业务响应。
 *
 * <p>接口平台调用成功后，客户端将平台结果中的 JSON 数据解析为本对象。
 * 数据按“工序 → 检验项 → 参数表 → 参数列和参数行”组织，
 * MES 可以直接读取对应字段，无需再次处理 {@code JsonNode}。
 * {@link JsonIgnoreProperties} 允许 MPM 后续增加字段而不影响当前字段的解析。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmInspectionReponseDTO {
    /**
     * MPM 业务处理是否成功；与接口平台调用状态分别判断。
     */
    private Boolean success;
    /**
     * MPM 业务状态码；{@code 200} 表示 MPM 业务处理成功。
     */
    private String code;
    /**
     * MPM 返回的业务提示或错误信息。
     */
    private String message;
    /**
     * 查询结果中的工序列表；未指定工序编号时可包含整本工艺的多个工序。
     */
    private List<Operation> result;
    /** MPM 响应中的 token 字段，按接口原值保留。 */
    private String token;
    /** MPM 响应中的 notifyIcon 字段；具体结构由 MPM 返回内容决定。 */
    private Object notifyIcon;

    /** 单道工序及该工序下的检验项。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Operation {
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

    /** 单个检验项及其关联的参数表。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InspectionItem {
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

    /** 检验项关联的一张参数表，包含列定义和数据行。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Parameter {
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

    /** 参数表中的单个列定义。 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Column {
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
}
