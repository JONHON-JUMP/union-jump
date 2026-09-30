package cn.jonhon.jump.module.system.dal.dataobject.user;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import cn.jonhon.jump.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子系统接口配置 DO（一行 = 一个接口）
 *
 * 与适配器类型无关（camstar/http 通用）：主表 sub_system_api_config 存系统级配置
 * （base_url/鉴权/映射/超时），本表按 purpose 存各接口端点；新增接口类型 = 加一行，不改表。
 */
@TableName("sub_system_api_endpoint")
@KeySequence("sub_system_api_endpoint_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class SubSystemApiEndpointDO extends BaseDO {

    @TableId
    private Long id;
    /** 外部系统 ID（sub_system.id） */
    private Long subSystemId;
    /**
     * 用途：query/create/update/delete/team_combo/role_query/role_create/role_delete；
     * 空串 = 未指定用途的自定义接口（不参与同用途唯一约束）
     */
    private String purpose;
    /** 前端分组：person=人员接口 / role=角色接口 / other=其他 */
    private String groupCode;
    /** 接口名称 */
    private String name;
    /** 完整地址或相对路径（相对时拼主表 base_url） */
    private String url;
    /** HTTP 方法：GET/POST/... */
    private String method;
    /** 是否启用：1启用 0停用 */
    private Integer enabled;
    /** 是否携带系统会话 Cookie：1携带 0不带 */
    private Integer withSession;
    /** 组内排序 */
    private Integer sort;

}
