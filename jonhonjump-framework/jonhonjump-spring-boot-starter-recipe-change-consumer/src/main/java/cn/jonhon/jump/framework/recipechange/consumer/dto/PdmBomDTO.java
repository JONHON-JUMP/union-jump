package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: pdmBom.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdmBomDTO {

    @JsonProperty("bomid")
    private String bomid;

    @JsonProperty("prntno")
    private String prntno;

    @JsonProperty("prntdesc")
    private String prntdesc;

    @JsonProperty("releaseno")
    private String releaseno;

    @JsonProperty("prtno")
    private String prtno;

    @JsonProperty("prtdesc")
    private String prtdesc;

    @JsonProperty("prtui")
    private String prtui;

    @JsonProperty("prtpqty")
    private String prtpqty;

    @JsonProperty("createtime")
    private String createtime;

    @JsonProperty("fmasterid")
    private String fmasterid;

    @JsonProperty("cmasterid")
    private String cmasterid;

    @JsonProperty("foid")
    private String foid;

    @JsonProperty("coid")
    private String coid;

    @JsonProperty("isv")
    private String isv;

    @JsonProperty("isscreen")
    private String isscreen;

    @JsonProperty("fcontainer")
    private String fcontainer;

    @JsonProperty("ccontainer")
    private String ccontainer;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("subpartselect")
    private String subpartselect;

    @JsonProperty("fiteration")
    private String fiteration;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

