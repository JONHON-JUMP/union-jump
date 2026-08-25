package cn.jonhon.jump.module.rm.recipechange.mq.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JUMP 发送给 MES 的工艺变更消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeChangeMessage {

    /**
     * MPM 通知唯一标识，用于 MES 侧幂等处理及回调关联
     */
    private String notifyId;

    /**
     * 目标车间编码
     */
    private String workshopCode;

    /**
     * 工艺变更的原始 JSON 内容
     */
    private JsonNode changeContent;

}
