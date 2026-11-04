package cn.jonhon.jump.module.system.enums.user;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.framework.subsystemapi.ApiEndpointSpecs;
import lombok.Getter;

/**
 * 业务叶子上的一次外部调用。一条绑定 = 这个页面动作使用哪个接口系统的哪条接口。
 */
@Getter
public enum LeafApiSceneEnum {

    ACCOUNT_SYNC("account_sync", "同步域账号/刷卡卡号", ApiEndpointSpecs.UPDATE, "修改人员"),
    USER_REGISTER("user_register", "用户注册（新增人员）", ApiEndpointSpecs.CREATE, "新增人员"),
    EMPLOYEE_QUERY("employee_query", "查询外部人员", ApiEndpointSpecs.QUERY, "查询人员"),
    ROLE_ASSIGN("role_assign", "分配角色同步", ApiEndpointSpecs.UPDATE, "修改人员"),
    ROLE_CREATE("role_create", "角色注册（新增角色）", ApiEndpointSpecs.ROLE_CREATE, "新增角色"),
    ROLE_QUERY("role_query", "查询外部系统角色", ApiEndpointSpecs.ROLE_QUERY, "查询角色");

    private final String scene;
    private final String label;
    private final String purpose;
    private final String purposeLabel;

    LeafApiSceneEnum(String scene, String label, String purpose, String purposeLabel) {
        this.scene = scene;
        this.label = label;
        this.purpose = purpose;
        this.purposeLabel = purposeLabel;
    }

    public static LeafApiSceneEnum ofScene(String scene) {
        if (StrUtil.isBlank(scene)) {
            return null;
        }
        for (LeafApiSceneEnum item : values()) {
            if (item.scene.equals(scene.trim())) {
                return item;
            }
        }
        return null;
    }

}
