package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: pdmPart.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdmPartDTO {

    @JsonProperty("partid")
    private String partid;

    @JsonProperty("prtno")
    private String prtno;

    @JsonProperty("prtdesc")
    private String prtdesc;

    @JsonProperty("source")
    private String source;

    @JsonProperty("rcdchgdatd")
    private String rcdchgdatd;

    @JsonProperty("rcdchgctrlerd")
    private String rcdchgctrlerd;

    @JsonProperty("releaseno")
    private String releaseno;

    @JsonProperty("prttype")
    private String prttype;

    @JsonProperty("mtltmrk")
    private String mtltmrk;

    @JsonProperty("prtwt")
    private String prtwt;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("prtum")
    private String prtum;

    @JsonProperty("mtldmsw")
    private String mtldmsw;

    @JsonProperty("mtldmst")
    private String mtldmst;

    @JsonProperty("mtldmsl")
    private String mtldmsl;

    @JsonProperty("prodline")
    private String prodline;

    @JsonProperty("cplb")
    private String cplb;

    @JsonProperty("processline")
    private String processline;

    @JsonProperty("matbigclassify")
    private String matbigclassify;

    @JsonProperty("matsmallclassify")
    private String matsmallclassify;

    @JsonProperty("mtlspcf")
    private String mtlspcf;

    @JsonProperty("prtstd")
    private String prtstd;

    @JsonProperty("mtlstat")
    private String mtlstat;

    @JsonProperty("mtlsgrt")
    private String mtlsgrt;

    @JsonProperty("sectionarea")
    private String sectionarea;

    @JsonProperty("color")
    private String color;

    @JsonProperty("providelength")
    private String providelength;

    @JsonProperty("trademark")
    private String trademark;

    @JsonProperty("prtrevedit")
    private String prtrevedit;

    @JsonProperty("inmailstate")
    private String inmailstate;

    @JsonProperty("fullidentifier")
    private String fullidentifier;

    @JsonProperty("erpoldno")
    private String erpoldno;

    @JsonProperty("description")
    private String description;

    @JsonProperty("createtime")
    private String createtime;

    @JsonProperty("drwpartno")
    private String drwpartno;

    @JsonProperty("ecnno")
    private String ecnno;

    @JsonProperty("enditem")
    private String enditem;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("confcode1")
    private String confcode1;

    @JsonProperty("masterid")
    private String masterid;

    @JsonProperty("oid")
    private String oid;

    @JsonProperty("part_link")
    private String partLink;

    @JsonProperty("partview")
    private String partview;

    @JsonProperty("classpath")
    private String classpath;

    @JsonProperty("classpathid")
    private String classpathid;

    @JsonProperty("materialclassify")
    private String materialclassify;

    @JsonProperty("qualitylev")
    private String qualitylev;

    @JsonProperty("componentno")
    private String componentno;

    @JsonProperty("cavityno")
    private String cavityno;

    @JsonProperty("productdepartment")
    private String productdepartment;

    @JsonProperty("csdegree")
    private String csdegree;

    @JsonProperty("groupcode")
    private String groupcode;

    @JsonProperty("itemno")
    private String itemno;

    @JsonProperty("tsdhxx")
    private String tsdhxx;

    @JsonProperty("yhmtltmrk")
    private String yhmtltmrk;

    @JsonProperty("cgzmark")
    private String cgzmark;

    @JsonProperty("patternlev")
    private String patternlev;

    @JsonProperty("simparts")
    private String simparts;

    @JsonProperty("cladfilm")
    private String cladfilm;

    @JsonProperty("volume")
    private String volume;

    @JsonProperty("deptcode")
    private String deptcode;

    @JsonProperty("validtype")
    private String validtype;

    @JsonProperty("inworkingstate")
    private String inworkingstate;

    @JsonProperty("inuseringstate")
    private String inuseringstate;

    @JsonProperty("yfitemno")
    private String yfitemno;

    @JsonProperty("wtparttype")
    private String wtparttype;

    @JsonProperty("hasbom")
    private String hasbom;

    @JsonProperty("materialtype")
    private String materialtype;

    @JsonProperty("standtype")
    private String standtype;

    @JsonProperty("sparemodel")
    private String sparemodel;

    @JsonProperty("ownplant")
    private String ownplant;

    @JsonProperty("prioritytype")
    private String prioritytype;

    @JsonProperty("metacognition")
    private String metacognition;

    @JsonProperty("firstuserpp")
    private String firstuserpp;

    @JsonProperty("orignofmaterial")
    private String orignofmaterial;

    @JsonProperty("modelremark")
    private String modelremark;

    @JsonProperty("moulddemand")
    private String moulddemand;

    @JsonProperty("productapparea")
    private String productapparea;

    @JsonProperty("maturitygrade")
    private String maturitygrade;

    @JsonProperty("approvaltype")
    private String approvaltype;

    @JsonProperty("container")
    private String container;

    @JsonProperty("containername")
    private String containername;

    @JsonProperty("priupdatetime")
    private String priupdatetime;

    @JsonProperty("approvaluser")
    private String approvaluser;

    @JsonProperty("iteration")
    private String iteration;

    @JsonProperty("createuser")
    private String createuser;

    @JsonProperty("createfulluser")
    private String createfulluser;

    @JsonProperty("objcreatetime")
    private String objcreatetime;

    @JsonProperty("modifyfulluser")
    private String modifyfulluser;

    @JsonProperty("transactionid")
    private String transactionid;

    @JsonProperty("stateinnervalue")
    private String stateinnervalue;

    @JsonProperty("statedisplayvalue")
    private String statedisplayvalue;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("readflag")
    private String readflag;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

