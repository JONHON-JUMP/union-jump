package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子系统人员接口配置 DO（系统级配置，一行 = 一个接入系统）
 *
 * 通过 apiType 选择适配器：
 * - camstar：Camstar 专用适配器（Cookie 会话登录 SSOLoginIn）
 * - http：通用 HTTP 适配器（按 param_mapping / response_mapping 纯配置驱动）
 *
 * 各接口端点存 {@link SubSystemApiEndpointDO}（一行一接口，按 purpose 区分用途）。
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
