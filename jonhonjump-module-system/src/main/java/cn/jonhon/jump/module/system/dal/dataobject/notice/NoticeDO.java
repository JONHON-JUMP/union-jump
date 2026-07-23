package cn.jonhon.jump.module.system.dal.dataobject.notice;

import cn.jonhon.jump.framework.tenant.core.db.TenantBaseDO;
import cn.jonhon.jump.module.system.controller.admin.notify.vo.message.NotifyMessageAttachmentVO;
import cn.jonhon.jump.module.system.enums.notify.NotifyTemplateTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 通知公告表
 *
 * @author ruoyi
 */
@TableName(value = "system_notice", autoResultMap = true)
@KeySequence("system_notice_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeDO extends TenantBaseDO {

    /**
     * 公告ID
     */
    private Long id;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 通知类型，对应 system_notify_template_type 字典
     *
     * 枚举 {@link NotifyTemplateTypeEnum}
     */
    private Integer type;
    /**
     * 公告内容
     */
    private String content;
    /**
     * 公告状态
     *
     * 枚举 {@link cn.jonhon.jump.module.system.enums.notice.NoticeStatusEnum}
     */
    private Integer status;

    /**
     * 发布人
     */
    private String publisherName;
    /**
     * 发布部门
     */
    private String deptName;
    /**
     * 附件列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<NotifyMessageAttachmentVO> attachments;

}
