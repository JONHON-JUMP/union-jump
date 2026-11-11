package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_tool.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeToolDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("tool_number")
    private String toolNumber;

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("tool_version")
    private String toolVersion;

    @JsonProperty("tool_iteration")
    private String toolIteration;

    @JsonProperty("tool_user")
    private String toolUser;

    @JsonProperty("tool_description")
    private String toolDescription;

    @JsonProperty("tool_ibapartment")
    private String toolIbapartment;

    @JsonProperty("tool_ibamodel")
    private String toolIbamodel;

    @JsonProperty("tool_link")
    private String toolLink;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("tool_state")
    private String toolState;

    @JsonProperty("tool_username")
    private String toolUsername;

    @JsonProperty("yfitemno")
    private String yfitemno;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("objtype")
    private String objtype;

    @JsonProperty("precision")
    private String precision;

    @JsonProperty("precision_unit")
    private String precisionUnit;

    @JsonProperty("range")
    private String range;

    @JsonProperty("working_range")
    private String workingRange;

    @JsonProperty("device_type")
    private String deviceType;

    @JsonProperty("skill_level")
    private String skillLevel;

    @JsonProperty("deadlinedate")
    private String deadlinedate;

    @JsonProperty("confirmrequireperiod")
    private String confirmrequireperiod;

    @JsonProperty("ismilitary")
    private String ismilitary;

    @JsonProperty("onelevel_plant")
    private String onelevelPlant;

    @JsonProperty("applicationarea")
    private String applicationarea;

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

