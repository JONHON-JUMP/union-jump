package cn.jonhon.jump.module.bpm.service.definition;

import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleRespVO;
import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleSaveReqVO;

import java.util.List;

/**
 * 流程任务分配规则 Service 接口
 *
 * 兼容 Vue2 管理端：规则数据存储在 BPMN XML 的候选人策略中
 */
public interface BpmTaskAssignRuleService {

    /**
     * 获得流程任务分配规则列表
     *
     * @param modelId 流程模型编号
     * @param processDefinitionId 流程定义编号
     * @return 任务分配规则列表
     */
    List<BpmTaskAssignRuleRespVO> getTaskAssignRuleList(String modelId, String processDefinitionId);

    /**
     * 创建流程任务分配规则
     *
     * @param createReqVO 创建信息
     * @return 规则编号
     */
    Long createTaskAssignRule(BpmTaskAssignRuleSaveReqVO createReqVO);

    /**
     * 更新流程任务分配规则
     *
     * @param updateReqVO 更新信息
     */
    void updateTaskAssignRule(BpmTaskAssignRuleSaveReqVO updateReqVO);

}
