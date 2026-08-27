package cn.jonhon.jump.module.mes.process.dal.process.oracle.dto;

import lombok.Data;

@Data
public class ProcessOperationDTO {

    /**
     * 工序名称
     */
    private String cname;
    /**
     * 工序编码
     */
    private String cnumber;
    /**
     * 序号
     */
    private String label;
    /**
     * 工序版本
     */
    private String cversion;
}
