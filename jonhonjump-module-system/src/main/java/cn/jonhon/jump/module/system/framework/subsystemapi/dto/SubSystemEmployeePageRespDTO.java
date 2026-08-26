package cn.jonhon.jump.module.system.framework.subsystemapi.dto;

import lombok.Data;

import java.util.List;

/**
 * 子系统人员分页响应 DTO
 */
@Data
public class SubSystemEmployeePageRespDTO {

    /** 总条数 */
    private Long total;
    /** 本页数据 */
    private List<SubSystemEmployeeDTO> list;

    public SubSystemEmployeePageRespDTO() {
    }

    public SubSystemEmployeePageRespDTO(Long total, List<SubSystemEmployeeDTO> list) {
        this.total = total;
        this.list = list;
    }

}
