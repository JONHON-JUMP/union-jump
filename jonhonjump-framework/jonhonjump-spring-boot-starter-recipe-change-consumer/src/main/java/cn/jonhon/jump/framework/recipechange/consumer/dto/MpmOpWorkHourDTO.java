package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_opWorkHour.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmOpWorkHourDTO {

    @JsonProperty("op_masterid")
    private String opMasterid;

    @JsonProperty("op_oid")
    private String opOid;

    @JsonProperty("op_number")
    private String opNumber;

    @JsonProperty("op_name")
    private String opName;

    @JsonProperty("op_version")
    private String opVersion;

    @JsonProperty("op_iteration")
    private String opIteration;

    @JsonProperty("wh_masterid")
    private String whMasterid;

    @JsonProperty("wh_oid")
    private String whOid;

    @JsonProperty("wh_number")
    private String whNumber;

    @JsonProperty("wh_name")
    private String whName;

    @JsonProperty("wh_version")
    private String whVersion;

    @JsonProperty("wh_iteration")
    private String whIteration;

    @JsonProperty("wh_remark")
    private String whRemark;

    @JsonProperty("wh_state")
    private String whState;

    @JsonProperty("wh_oper")
    private String whOper;

    @JsonProperty("wh_pre")
    private String whPre;

    @JsonProperty("wh_createuser")
    private String whCreateuser;

    @JsonProperty("wh_createfulluser")
    private String whCreatefulluser;

    @JsonProperty("wh_createtime")
    private String whCreatetime;

    @JsonProperty("wh_modifyuser")
    private String whModifyuser;

    @JsonProperty("wh_modifyfulluser")
    private String whModifyfulluser;

    @JsonProperty("wh_modifytime")
    private String whModifytime;

    @JsonProperty("wh_container")
    private String whContainer;

    @JsonProperty("wh_containername")
    private String whContainername;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("condition_no")
    private String conditionNo;

    @JsonProperty("norm_condition")
    private String normCondition;

    @JsonProperty("norm_qty")
    private String normQty;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

