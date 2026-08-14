package cn.jonhon.jump.framework.portalsubsystem.oauth;

import lombok.Data;

/**
 * 主系统用户基本信息（OAuth user.read）。
 */
@Data
public class PortalUserInfo {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String mobile;
    private Integer sex;
    private String avatar;
    private Long deptId;

}
