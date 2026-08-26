package cn.jonhon.jump.module.system.framework.subsystemapi.dto;

import lombok.Data;

/**
 * 子系统人员分页查询 DTO
 */
@Data
public class SubSystemEmployeeQueryDTO {

    /** 页码（从 1 开始） */
    private Integer page = 1;
    /** 每页条数 */
    private Integer rows = 10;
    /** 车间编码（过滤） */
    private String workshopCode;
    /** 工号（模糊过滤） */
    private String userCode;
    /** 姓名（模糊过滤） */
    private String userName;

}
