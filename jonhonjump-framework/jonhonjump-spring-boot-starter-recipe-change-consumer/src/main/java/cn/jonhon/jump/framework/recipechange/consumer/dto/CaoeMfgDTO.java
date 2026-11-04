package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_mfg.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeMfgDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("mfg_number")
    private String mfgNumber;

    @JsonProperty("mfg_name")
    private String mfgName;

    @JsonProperty("mfg_version")
    private String mfgVersion;

    @JsonProperty("mfg_iteration")
    private String mfgIteration;

    @JsonProperty("mfg_user")
    private String mfgUser;

    @JsonProperty("mfg_description")
    private String mfgDescription;

    @JsonProperty("mfg_container")
    private String mfgContainer;

    @JsonProperty("mfg_link")
    private String mfgLink;

    @JsonProperty("mfg_ibacaplevel")
    private String mfgIbacaplevel;

    @JsonProperty("mfg_ibacalc")
    private String mfgIbacalc;

    @JsonProperty("mfg_ibaoppameter")
    private String mfgIbaoppameter;

    @JsonProperty("mfg_ibaunpameter")
    private String mfgIbaunpameter;

    @JsonProperty("mfg_category")
    private String mfgCategory;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("mfg_state")
    private String mfgState;

    @JsonProperty("mfg_username")
    private String mfgUsername;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

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

