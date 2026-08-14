package cn.jonhon.jump.module.system.dal.dataobject.faq;

import cn.jonhon.jump.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常见 QA 表
 */
@TableName("system_faq")
@KeySequence("system_faq_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class FaqDO extends BaseDO {

    /**
     * 编号
     */
    private Long id;
    /**
     * 分类，对应 system_faq_category 字典
     */
    private Integer category;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 显示顺序，越大越靠前
     */
    private Integer sort;
    /**
     * 状态
     *
     * 枚举 {@link cn.jonhon.jump.module.system.enums.faq.FaqStatusEnum}
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

}
