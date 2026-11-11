package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * MPM changeContent: caoe_ppOpLink.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoePpOpLinkDTO {

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

    @JsonProperty("label")
    private String label;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("ftype")
    private String ftype;

    @JsonProperty("ctype")
    private String ctype;

    @JsonProperty("fversion")
    private String fversion;

    @JsonProperty("cversion")
    private String cversion;

    @JsonProperty("fcontainer")
    private String fcontainer;

    @JsonProperty("ccontainer")
    private String ccontainer;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

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

    @JsonProperty("caoe_op")
    private List<CaoeOpDTO> caoeOp;

}

