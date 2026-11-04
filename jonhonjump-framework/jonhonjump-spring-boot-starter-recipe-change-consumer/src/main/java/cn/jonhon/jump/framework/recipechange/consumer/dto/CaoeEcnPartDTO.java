package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_ecnPart.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeEcnPartDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("ecnpart_number")
    private String ecnpartNumber;

    @JsonProperty("ecnpart_name")
    private String ecnpartName;

    @JsonProperty("ecnpart_inmailstate")
    private String ecnpartInmailstate;

    @JsonProperty("ecnpart_description")
    private String ecnpartDescription;

    @JsonProperty("ecnpart_ecnnumber")
    private String ecnpartEcnnumber;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("ecnpart_version")
    private String ecnpartVersion;

    @JsonProperty("ecnpart_working")
    private String ecnpartWorking;

    @JsonProperty("ecnpart_usering")
    private String ecnpartUsering;

    @JsonProperty("islatestversion")
    private String islatestversion;

    @JsonProperty("latestpartversion")
    private String latestpartversion;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

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

