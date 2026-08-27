package cn.jonhon.jump.module.mes.process.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "MES管理 - 工艺卡片查看 响应参数")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessCardRespVO {

    /**
     * 工艺号
     */
    private String accno;

    /**
     * 工艺版本
     */
    private String version;

    /**
     * 是否正式工艺
     */
    private Integer isFormal;

    /**
     * 是否返修
     */
    private Integer isFix;

    /**
     * 工艺信息详情
      */
    List<ProcessCardDetailsRespVO> details;
}
