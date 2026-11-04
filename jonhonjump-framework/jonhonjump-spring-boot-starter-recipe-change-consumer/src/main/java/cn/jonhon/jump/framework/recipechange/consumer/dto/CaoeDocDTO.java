package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: caoe_doc.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaoeDocDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("doc_number")
    private String docNumber;

    @JsonProperty("doc_name")
    private String docName;

    @JsonProperty("doc_version")
    private String docVersion;

    @JsonProperty("doc_iteration")
    private String docIteration;

    @JsonProperty("doc_user")
    private String docUser;

    @JsonProperty("doc_type")
    private String docType;

    @JsonProperty("doc_stype")
    private String docStype;

    @JsonProperty("doc_jcxt")
    private String docJcxt;

    @JsonProperty("doc_jcxh")
    private String docJcxh;

    @JsonProperty("doc_yynr")
    private String docYynr;

    @JsonProperty("doc_link")
    private String docLink;

    @JsonProperty("createtime")
    private String createtime;

    @JsonProperty("doc_code")
    private String docCode;

    @JsonProperty("doc_state")
    private String docState;

    @JsonProperty("doc_des")
    private String docDes;

    @JsonProperty("guke")
    private String guke;

    @JsonProperty("doc_username")
    private String docUsername;

    @JsonProperty("doc_modtime")
    private String docModtime;

    @JsonProperty("see_version")
    private String seeVersion;

    @JsonProperty("doc_rel")
    private String docRel;

    @JsonProperty("primarydownlink")
    private String primarydownlink;

    @JsonProperty("signdownlink")
    private String signdownlink;

    @JsonProperty("productspecification")
    private String productspecification;

    @JsonProperty("mtltmrk")
    private String mtltmrk;

    @JsonProperty("otime")
    private String otime;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("plottasknumber")
    private String plottasknumber;

    @JsonProperty("rdpprojectnum")
    private String rdpprojectnum;

    @JsonProperty("procedurenum")
    private String procedurenum;

    @JsonProperty("proceduretype")
    private String proceduretype;

    @JsonProperty("typicaldoc")
    private String typicaldoc;

    @JsonProperty("createuser")
    private String createuser;

    @JsonProperty("createfulluser")
    private String createfulluser;

    @JsonProperty("objcreatetime")
    private String objcreatetime;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("responsibleunit")
    private String responsibleunit;

    @JsonProperty("related_material")
    private String relatedMaterial;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

