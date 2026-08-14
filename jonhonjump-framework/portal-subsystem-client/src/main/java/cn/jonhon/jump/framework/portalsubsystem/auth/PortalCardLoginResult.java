package cn.jonhon.jump.framework.portalsubsystem.auth;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 机端刷卡校验响应（主系统 card-login）。
 */
@Data
public class PortalCardLoginResult {

    private Long subSystemId;
    private String clientId;
    private String username;
    private String nickname;
    private String status;
    private String workshopId;
    private String teamId;
    private Long mainUserId;
    private List<String> roleCodes = new ArrayList<>();

}
