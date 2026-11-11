package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_plant.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoePlantDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("plant_number")
    private String plantNumber;

    @JsonProperty("plant_name")
    private String plantName;

    @JsonProperty("plant_version")
    private String plantVersion;

    @JsonProperty("plant_iteration")
    private String plantIteration;

    @JsonProperty("plant_user")
    private String plantUser;

    @JsonProperty("plant_description")
    private String plantDescription;

    @JsonProperty("plant_container")
    private String plantContainer;

    @JsonProperty("plant_link")
    private String plantLink;

    @JsonProperty("plant_ibaaline")
    private String plantIbaaline;

    @JsonProperty("plant_category")
    private String plantCategory;

    @JsonProperty("plant_external")
    private String plantExternal;

    @JsonProperty("plant_ibaisv")
    private String plantIbaisv;

    @JsonProperty("plant_ibatmodel")
    private String plantIbatmodel;

    @JsonProperty("plant_ibawctype")
    private String plantIbawctype;

    @JsonProperty("plant_ibastationcode")
    private String plantIbastationcode;

    @JsonProperty("plant_address")
    private String plantAddress;

    @JsonProperty("plant_city")
    private String plantCity;

    @JsonProperty("plant_states")
    private String plantStates;

    @JsonProperty("plant_country")
    private String plantCountry;

    @JsonProperty("plant_zipcode")
    private String plantZipcode;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("plant_state")
    private String plantState;

    @JsonProperty("plant_username")
    private String plantUsername;

    @JsonProperty("prioritylevel")
    private String prioritylevel;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("objtype")
    private String objtype;

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

