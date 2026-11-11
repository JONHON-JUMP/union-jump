package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_opPartLink.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeOpPartLinkDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("fmasterid")
    private String fmasterid;

    @JsonProperty("cmasterid")
    private String cmasterid;

    @JsonProperty("foid")
    private String foid;

    @JsonProperty("coid")
    private String coid;

    @JsonProperty("fnumber")
    private String fnumber;

    @JsonProperty("cnumber")
    private String cnumber;

    @JsonProperty("fname")
    private String fname;

    @JsonProperty("cname")
    private String cname;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("ibatype")
    private String ibatype;

    @JsonProperty("ibaxh")
    private String ibaxh;

    @JsonProperty("ibaremark")
    private String ibaremark;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("ibaismust")
    private String ibaismust;

    @JsonProperty("checkdimensions")
    private String checkdimensions;

    @JsonProperty("sizesequence")
    private String sizesequence;

    @JsonProperty("sizecolor")
    private String sizecolor;

    @JsonProperty("maparea")
    private String maparea;

    @JsonProperty("fcontainer")
    private String fcontainer;

    @JsonProperty("ccontainer")
    private String ccontainer;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("distribution")
    private String distribution;

    @JsonProperty("fversion")
    private String fversion;

    @JsonProperty("fiteration")
    private String fiteration;

    @JsonProperty("ftype")
    private String ftype;

    @JsonProperty("ctype")
    private String ctype;

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

