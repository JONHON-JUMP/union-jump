package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_workFlow.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmWorkFlowDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("wf_processid")
    private String wfProcessid;

    @JsonProperty("wf_businessobjoid")
    private String wfBusinessobjoid;

    @JsonProperty("wf_template")
    private String wfTemplate;

    @JsonProperty("wf_type")
    private String wfType;

    @JsonProperty("wf_number")
    private String wfNumber;

    @JsonProperty("wf_name")
    private String wfName;

    @JsonProperty("wf_state")
    private String wfState;

    @JsonProperty("wf_owneruser")
    private String wfOwneruser;

    @JsonProperty("wf_ownerfulluser")
    private String wfOwnerfulluser;

    @JsonProperty("wf_starttime")
    private String wfStarttime;

    @JsonProperty("wf_link")
    private String wfLink;

    @JsonProperty("publishtime")
    private String publishtime;

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

