package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_ecn.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeEcnDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("ecn_number")
    private String ecnNumber;

    @JsonProperty("ecn_name")
    private String ecnName;

    @JsonProperty("ecn_link")
    private String ecnLink;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("ecn_user")
    private String ecnUser;

    @JsonProperty("ecn_description")
    private String ecnDescription;

    @JsonProperty("ecn_type")
    private String ecnType;

    @JsonProperty("ecn_ecntype")
    private String ecnEcntype;

    @JsonProperty("ecn_username")
    private String ecnUsername;

    @JsonProperty("hasaversion")
    private String hasaversion;

    @JsonProperty("ecn_mold")
    private String ecnMold;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("container")
    private String container;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("ecn_scenario")
    private String ecnScenario;

    @JsonProperty("ecn_objtype")
    private String ecnObjtype;

    @JsonProperty("createuser")
    private String createuser;

    @JsonProperty("createfulluser")
    private String createfulluser;

    @JsonProperty("objcreatetime")
    private String objcreatetime;

    @JsonProperty("objmodifytime")
    private String objmodifytime;

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

