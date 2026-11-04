package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * MPM changeContent: recipe_change_content.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeChangeContentDTO {

    @JsonProperty("mpm_transaction")
    private MpmTransactionDTO mpmTransaction;

    @JsonProperty("mpm_workFlow")
    private List<MpmWorkFlowDTO> mpmWorkFlow;

    @JsonProperty("caoe_ecn")
    private List<CaoeEcnDTO> caoeEcn;

    @JsonProperty("caoe_ecnPart")
    private List<CaoeEcnPartDTO> caoeEcnPart;

    @JsonProperty("mpm_ecnBeforeLink")
    private List<MpmEcnBeforeLinkDTO> mpmEcnBeforeLink;

    @JsonProperty("mpm_ecnAfterLink")
    private List<MpmEcnAfterLinkDTO> mpmEcnAfterLink;

    @JsonProperty("pdmPart")
    private List<PdmPartDTO> pdmPart;

    @JsonProperty("pdmBom")
    private List<PdmBomDTO> pdmBom;

    @JsonProperty("caoe_partPpLink")
    private List<CaoePartPpLinkDTO> caoePartPpLink;

    @JsonProperty("caoe_partDocLink")
    private List<CaoePartDocLinkDTO> caoePartDocLink;

    @JsonProperty("caoe_doc")
    private List<CaoeDocDTO> caoeDoc;

    @JsonProperty("caoe_docDocLink")
    private List<CaoeDocDocLinkDTO> caoeDocDocLink;

    @JsonProperty("caoe_tool")
    private List<CaoeToolDTO> caoeTool;

    @JsonProperty("mpm_resourcePartLink")
    private List<MpmResourcePartLinkDTO> mpmResourcePartLink;

    @JsonProperty("caoe_mfg")
    private List<CaoeMfgDTO> caoeMfg;

    @JsonProperty("caoe_plant")
    private List<CaoePlantDTO> caoePlant;

    @JsonProperty("caoe_mfgPlantLink")
    private List<CaoeMfgPlantLinkDTO> caoeMfgPlantLink;

    @JsonProperty("mpm_inspectionResourceLink")
    private List<MpmInspectionResourceLinkDTO> mpmInspectionResourceLink;

    @JsonProperty("mpm_inspection")
    private List<MpmInspectionDTO> mpmInspection;

    @JsonProperty("caoe_pp")
    private List<CaoePpDTO> caoePp;

}

