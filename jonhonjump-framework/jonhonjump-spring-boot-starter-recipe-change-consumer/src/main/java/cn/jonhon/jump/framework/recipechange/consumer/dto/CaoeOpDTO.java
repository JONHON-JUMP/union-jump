package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * MPM changeContent: caoe_op.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeOpDTO {

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("op_number")
    private String opNumber;

    @JsonProperty("op_name")
    private String opName;

    @JsonProperty("op_version")
    private String opVersion;

    @JsonProperty("op_iteration")
    private String opIteration;

    @JsonProperty("op_user")
    private String opUser;

    @JsonProperty("op_type")
    private String opType;

    @JsonProperty("op_view")
    private String opView;

    @JsonProperty("op_longdescription")
    private String opLongdescription;

    @JsonProperty("op_description")
    private String opDescription;

    @JsonProperty("op_gxdm")
    private String opGxdm;

    @JsonProperty("op_jyyq")
    private String opJyyq;

    @JsonProperty("op_link")
    private String opLink;

    @JsonProperty("op_setuptime")
    private String opSetuptime;

    @JsonProperty("op_queuetime")
    private String opQueuetime;

    @JsonProperty("op_labortime")
    private String opLabortime;

    @JsonProperty("op_processingtime")
    private String opProcessingtime;

    @JsonProperty("op_waitingtime")
    private String opWaitingtime;

    @JsonProperty("op_teardowntime")
    private String opTeardowntime;

    @JsonProperty("op_movetime")
    private String opMovetime;

    @JsonProperty("op_lagtime")
    private String opLagtime;

    @JsonProperty("op_std8time")
    private String opStd8time;

    @JsonProperty("op_ibapartment")
    private String opIbapartment;

    @JsonProperty("op_ibaremark")
    private String opIbaremark;

    @JsonProperty("op_category")
    private String opCategory;

    @JsonProperty("op_special")
    private String opSpecial;

    @JsonProperty("op_key")
    private String opKey;

    @JsonProperty("op_isv")
    private String opIsv;

    @JsonProperty("op_cgdes")
    private String opCgdes;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("op_label")
    private String opLabel;

    @JsonProperty("op_state")
    private String opState;

    @JsonProperty("op_czqjc")
    private String opCzqjc;

    @JsonProperty("op_jylx")
    private String opJylx;

    @JsonProperty("op_username")
    private String opUsername;

    @JsonProperty("ismulprogram")
    private String ismulprogram;

    @JsonProperty("numcontrolpro")
    private String numcontrolpro;

    @JsonProperty("diffdegree")
    private String diffdegree;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("prorequirements")
    private String prorequirements;

    @JsonProperty("ismainpro")
    private String ismainpro;

    @JsonProperty("isauxiliary")
    private String isauxiliary;

    @JsonProperty("ismultimedia")
    private String ismultimedia;

    @JsonProperty("media_range")
    private String mediaRange;

    @JsonProperty("inspection")
    private String inspection;

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

    @JsonProperty("op_czqjc_add")
    private String opCzqjcAdd;

    @JsonProperty("prorequirements_add")
    private String prorequirementsAdd;

    @JsonProperty("op_longdescription_add")
    private String opLongdescriptionAdd;

    @JsonProperty("op_longdescription_mpm")
    private String opLongdescriptionMpm;

    @JsonProperty("op_czqjc_mpm")
    private String opCzqjcMpm;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

    @JsonProperty("caoe_opPartLink")
    private List<CaoeOpPartLinkDTO> caoeOpPartLink;

    @JsonProperty("caoe_opCsLink")
    private List<CaoeOpCsLinkDTO> caoeOpCsLink;

    @JsonProperty("caoe_opDocLink")
    private List<CaoeOpDocLinkDTO> caoeOpDocLink;

    @JsonProperty("caoe_opMfgLink")
    private List<CaoeOpMfgLinkDTO> caoeOpMfgLink;

    @JsonProperty("mpm_opResourceLink")
    private List<MpmOpResourceLinkDTO> mpmOpResourceLink;

    @JsonProperty("mpm_opMPartLink")
    private List<MpmOpMPartLinkDTO> mpmOpMPartLink;

    @JsonProperty("mpm_opWorkHour")
    private List<MpmOpWorkHourDTO> mpmOpWorkHour;

    @JsonProperty("mpm_opInspectionLink")
    private List<MpmOpInspectionLinkDTO> mpmOpInspectionLink;

}

