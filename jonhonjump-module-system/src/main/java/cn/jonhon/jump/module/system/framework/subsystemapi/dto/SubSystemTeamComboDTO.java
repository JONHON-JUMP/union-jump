package cn.jonhon.jump.module.system.framework.subsystemapi.dto;

import lombok.Data;

/**
 * 子系统班组下拉 DTO
 */
@Data
public class SubSystemTeamComboDTO {

    /** 班组编码 */
    private String teamCode;
    /** 班组名称 */
    private String teamName;

    public SubSystemTeamComboDTO() {
    }

    public SubSystemTeamComboDTO(String teamCode, String teamName) {
        this.teamCode = teamCode;
        this.teamName = teamName;
    }

}
