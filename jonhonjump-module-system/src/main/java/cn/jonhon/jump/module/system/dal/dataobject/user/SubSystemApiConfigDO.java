package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子系统人员接口配置 DO
 *
 * 每个外部系统一行；通过 apiType 选择适配器：
 * - camstar：Camstar 专用适配器（Cookie 会话登录 SSOLoginIn）
 * - http：通用 HTTP 适配器（按 param_mapping / response_mapping 纯配置驱动）
 *
 * 各接口 JSON 格式：{"path":"/BasicData/Employee/getEmployeeInfo","method":"POST"}
 */
@TableName("sub_system_api_config")
@KeySequence("sub_system_api_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemApiConfigDO extends BaseDO {

    @TableId
    private Long id;
    /** 外部系统 ID（sub_system.id），唯一 */
    private Long subSystemId;
    /** 适配器类型：camstar / http */
    private String apiType;
    /** 接口基地址，如 http://127.0.0.1:8090 */
    private String baseUrl;
    /** 鉴权方式：none / cookie_sso */
    private String authType;
    /** 鉴权配置 JSON：{"loginPath":"...","token":"...","cookieName":"..."} */
    private String authConfig;
    /** 查询接口 */
    private String apiQuery;
    /** 新增接口（人员 upsert） */
    private String apiCreate;
    /** 修改接口（人员 upsert，可同新增） */
    private String apiUpdate;
    /** 删除接口 */
    private String apiDelete;
    /** 班组下拉接口 */
    private String apiTeamCombo;
    /** 角色查询接口 */
    private String apiRoleQuery;
    /** 角色新增接口 */
    private String apiRoleCreate;
    /** 角色删除接口 */
    private String apiRoleDelete;
    /**
     * 接口目录树 JSON（菜单式：目录 + 叶子）。
     * 叶子含 purpose：auth/query/create/update/delete/role_query/role_create/role_delete；
     * 用户同步业务系统时调 purpose=create 的叶子；role_* 仅在线测试调用（角色裸增删查，不挂页面）。
     */
    private String apiCatalog;
    /** 参数映射 JSON：JUMP标准参数名→对方参数名 */
    private String paramMapping;
    /** 响应映射 JSON（http 适配器用）：successField/successValue/listPath/totalPath */
    private String responseMapping;
    /** 删除二次确认提示语 */
    private String deleteTip;
    /** 连接超时（毫秒） */
    private Long connectTimeoutMs;
    /** 读取超时（毫秒） */
    private Long readTimeoutMs;
    /** 状态：0启用 1停用 */
    private Integer status;

}
