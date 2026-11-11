package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_partDocLink.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoePartDocLinkDTO {

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

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("ftype")
    private String ftype;

    @JsonProperty("ctype")
    private String ctype;

    @JsonProperty("fpartversion")
    private String fpartversion;

    @JsonProperty("cdocversion")
    private String cdocversion;

    @JsonProperty("fcontainer")
    private String fcontainer;

    @JsonProperty("ccontainer")
    private String ccontainer;

    @JsonProperty("fiteration")
    private String fiteration;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

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

