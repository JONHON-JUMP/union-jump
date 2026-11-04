package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_ecnAfterLink.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmEcnAfterLinkDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("wf_processid")
    private String wfProcessid;

    @JsonProperty("wf_businessobjoid")
    private String wfBusinessobjoid;

    @JsonProperty("review_oid")
    private String reviewOid;

    @JsonProperty("review_type")
    private String reviewType;

    @JsonProperty("review_number")
    private String reviewNumber;

    @JsonProperty("review_name")
    private String reviewName;

    @JsonProperty("review_version")
    private String reviewVersion;

    @JsonProperty("review_iteration")
    private String reviewIteration;

    @JsonProperty("review_state")
    private String reviewState;

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

