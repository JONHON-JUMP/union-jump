package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * MPM changeContent: caoe_pp.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoePpDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("pp_number")
    private String ppNumber;

    @JsonProperty("pp_name")
    private String ppName;

    @JsonProperty("pp_version")
    private String ppVersion;

    @JsonProperty("pp_iteration")
    private String ppIteration;

    @JsonProperty("pp_user")
    private String ppUser;

    @JsonProperty("pp_type")
    private String ppType;

    @JsonProperty("pp_view")
    private String ppView;

    @JsonProperty("pp_container")
    private String ppContainer;

    @JsonProperty("pp_link")
    private String ppLink;

    @JsonProperty("pp_remark")
    private String ppRemark;

    @JsonProperty("pp_category")
    private String ppCategory;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("pp_state")
    private String ppState;

    @JsonProperty("pp_desc")
    private String ppDesc;

    @JsonProperty("pp_username")
    private String ppUsername;

    @JsonProperty("processregulationlink")
    private String processregulationlink;

    @JsonProperty("pp_optimization")
    private String ppOptimization;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("typicalpp")
    private String typicalpp;

    @JsonProperty("procedurenum")
    private String procedurenum;

    @JsonProperty("proceduretype")
    private String proceduretype;

    @JsonProperty("protype")
    private String protype;

    @JsonProperty("proapplication")
    private String proapplication;

    @JsonProperty("isvirtual")
    private String isvirtual;

    @JsonProperty("isauxiliary")
    private String isauxiliary;

    @JsonProperty("productiontype")
    private String productiontype;

    @JsonProperty("ordernum")
    private String ordernum;

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

    @JsonProperty("classpath")
    private String classpath;

    @JsonProperty("attrclassjson")
    private String attrclassjson;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

    @JsonProperty("caoe_ppOpLink")
    private List<CaoePpOpLinkDTO> caoePpOpLink;

}

