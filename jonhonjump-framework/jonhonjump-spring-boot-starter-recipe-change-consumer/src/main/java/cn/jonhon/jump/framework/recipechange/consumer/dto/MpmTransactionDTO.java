package cn.jonhon.jump.framework.recipechange.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * MPM changeContent: mpm_transaction.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MpmTransactionDTO {

    @JsonProperty("uniqueid")
    private String uniqueid;

    @JsonProperty("processid")
    private String processid;

    @JsonProperty("publishtime")
    private String publishtime;

    @JsonProperty("transactionid_priupdate")
    private String transactionidPriupdate;

    @JsonProperty("senduser")
    private String senduser;

    @JsonProperty("sendfulluser")
    private String sendfulluser;

    @JsonProperty("sourcesys")
    private String sourcesys;

}

