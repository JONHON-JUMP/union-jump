package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_opInspectionLink.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmOpInspectionLinkDTO {

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

    @JsonProperty("fversion")
    private String fversion;

    @JsonProperty("fiteration")
    private String fiteration;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("fcontainer")
    private String fcontainer;

    @JsonProperty("ccontainer")
    private String ccontainer;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("inspectionitemtype")
    private String inspectionitemtype;

    @JsonProperty("cversion")
    private String cversion;

    @JsonProperty("citeration")
    private String citeration;

    @JsonProperty("cstate")
    private String cstate;

    @JsonProperty("inspectionfrequency")
    private String inspectionfrequency;

    @JsonProperty("inspectiontype")
    private String inspectiontype;

    @JsonProperty("requirement")
    private String requirement;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

