package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_inspection.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmInspectionDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("number_insp")
    private String numberInsp;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("iteration")
    private String iteration;

    @JsonProperty("inspectiondimension")
    private String inspectiondimension;

    @JsonProperty("dimensiontype")
    private String dimensiontype;

    @JsonProperty("featuretype")
    private String featuretype;

    @JsonProperty("inspectiontool")
    private String inspectiontool;

    @JsonProperty("inspectiontoolnumber")
    private String inspectiontoolnumber;

    @JsonProperty("link")
    private String link;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("state")
    private String state;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("createuser")
    private String createuser;

    @JsonProperty("createfulluser")
    private String createfulluser;

    @JsonProperty("objcreatetime")
    private String objcreatetime;

    @JsonProperty("modifyuser")
    private String modifyuser;

    @JsonProperty("modifyfulluser")
    private String modifyfulluser;

    @JsonProperty("objmodifytime")
    private String objmodifytime;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("contentcriteria")
    private String contentcriteria;

    @JsonProperty("inspectionmethod")
    private String inspectionmethod;

    @JsonProperty("objtype")
    private String objtype;

    @JsonProperty("color")
    private String color;

    @JsonProperty("figurearea")
    private String figurearea;

    @JsonProperty("dimensionmark")
    private String dimensionmark;

    @JsonProperty("upperdimensionlimit")
    private String upperdimensionlimit;

    @JsonProperty("lowerdimensionlimit")
    private String lowerdimensionlimit;

    @JsonProperty("dimensionalaccuracy")
    private String dimensionalaccuracy;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

