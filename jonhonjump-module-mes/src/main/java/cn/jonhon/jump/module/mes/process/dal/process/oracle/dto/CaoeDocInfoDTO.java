package cn.jonhon.jump.module.mes.process.dal.process.oracle.dto;

import lombok.Data;

@Data
public class CaoeDocInfoDTO {

    /** 文档原始链接。 */
    private String docLink;

    /**
     * 状态
     */
    private String docState;

    /**
     * 链接
     */
    private String oid;

    /**
     * 物料号
     */
    private String docNumber;

}
