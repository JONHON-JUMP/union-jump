package cn.jonhon.jump.module.bpm.service.definition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.util.string.StrUtils;
import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleRespVO;
import cn.jonhon.jump.module.bpm.controller.admin.definition.vo.task.BpmTaskAssignRuleSaveReqVO;
import cn.jonhon.jump.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.jonhon.jump.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.bpm.enums.ErrorCodeConstants.MODEL_NOT_EXISTS;

/**
 * 流程任务分配规则 Service 实现类
 */
@Service
@Validated
public class BpmTaskAssignRuleServiceImpl implements BpmTaskAssignRuleService {

    @Resource
    private BpmModelService modelService;
    @Resource
    private BpmTaskCandidateInvoker taskCandidateInvoker;

    @Override
    public List<BpmTaskAssignRuleRespVO> getTaskAssignRuleList(String modelId, String processDefinitionId) {
        BpmnModel bpmnModel = getBpmnModel(modelId, processDefinitionId);
        if (bpmnModel == null) {
            return Collections.emptyList();
        }
        List<BpmTaskAssignRuleRespVO> result = new ArrayList<>();
        for (UserTask userTask : BpmnModelUtils.getBpmnModelElements(bpmnModel, UserTask.class)) {
            BpmTaskAssignRuleRespVO respVO = buildTaskAssignRule(userTask, modelId, processDefinitionId);
            result.add(respVO);
        }
        return result;
    }

    @Override
    public Long createTaskAssignRule(BpmTaskAssignRuleSaveReqVO createReqVO) {
        if (StrUtil.isBlank(createReqVO.getModelId()) || StrUtil.isBlank(createReqVO.getTaskDefinitionKey())) {
            throw exception(MODEL_NOT_EXISTS);
        }
        return saveTaskAssignRule(createReqVO.getModelId(), createReqVO.getTaskDefinitionKey(),
                createReqVO.getType(), createReqVO.getOptions());
    }

    @Override
    public void updateTaskAssignRule(BpmTaskAssignRuleSaveReqVO updateReqVO) {
        if (StrUtil.isBlank(updateReqVO.getModelId())) {
            throw exception(MODEL_NOT_EXISTS);
        }
        String taskDefinitionKey = updateReqVO.getTaskDefinitionKey();
        if (StrUtil.isBlank(taskDefinitionKey) && updateReqVO.getId() != null) {
            BpmnModel bpmnModel = getBpmnModel(updateReqVO.getModelId(), null);
            if (bpmnModel == null) {
                throw exception(MODEL_NOT_EXISTS);
            }
            UserTask userTask = findUserTaskByRuleId(bpmnModel, updateReqVO.getId());
            taskDefinitionKey = userTask != null ? userTask.getId() : null;
        }
        if (StrUtil.isBlank(taskDefinitionKey)) {
            throw exception(MODEL_NOT_EXISTS);
        }
        saveTaskAssignRule(updateReqVO.getModelId(), taskDefinitionKey,
                updateReqVO.getType(), updateReqVO.getOptions());
    }

    private Long saveTaskAssignRule(String modelId, String taskDefinitionKey, Integer type, List<Long> options) {
        byte[] bpmnBytes = modelService.getModelBpmnXML(modelId);
        BpmnModel bpmnModel = BpmnModelUtils.getBpmnModel(bpmnBytes);
        if (bpmnModel == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        UserTask userTask = CollUtil.findOne(BpmnModelUtils.getBpmnModelElements(bpmnModel, UserTask.class),
                task -> Objects.equals(task.getId(), taskDefinitionKey));
        if (userTask == null) {
            throw exception(MODEL_NOT_EXISTS);
        }

        String candidateParam = buildCandidateParam(options);
        BpmnModelUtils.addCandidateElements(type, candidateParam, userTask);

        // 仅校验当前任务，避免保存单个节点时因其它节点未配置而无法保存
        taskCandidateInvoker.validateUserTaskCandidate(userTask);
        String bpmnXml = BpmnModelUtils.getBpmnXml(bpmnModel);
        modelService.updateModelBpmnXml(modelId, bpmnXml);
        return buildTaskAssignRuleId(taskDefinitionKey);
    }

    private BpmnModel getBpmnModel(String modelId, String processDefinitionId) {
        if (StrUtil.isNotBlank(modelId)) {
            byte[] bpmnBytes = modelService.getModelBpmnXML(modelId);
            return BpmnModelUtils.getBpmnModel(bpmnBytes);
        }
        if (StrUtil.isNotBlank(processDefinitionId)) {
            return modelService.getBpmnModelByDefinitionId(processDefinitionId);
        }
        return null;
    }

    private BpmTaskAssignRuleRespVO buildTaskAssignRule(UserTask userTask, String modelId, String processDefinitionId) {
        BpmTaskAssignRuleRespVO respVO = new BpmTaskAssignRuleRespVO();
        respVO.setModelId(modelId);
        respVO.setProcessDefinitionId(processDefinitionId);
        respVO.setTaskDefinitionKey(userTask.getId());
        respVO.setTaskDefinitionName(userTask.getName());

        Integer strategy = BpmnModelUtils.parseCandidateStrategy(userTask);
        if (strategy != null) {
            respVO.setId(buildTaskAssignRuleId(userTask.getId()));
            respVO.setType(strategy);
            respVO.setOptions(parseOptions(BpmnModelUtils.parseCandidateParam(userTask)));
        }
        return respVO;
    }

    private List<Long> parseOptions(String candidateParam) {
        if (StrUtil.isBlank(candidateParam)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(StrUtils.splitToLongSet(candidateParam));
    }

    private String buildCandidateParam(List<Long> options) {
        if (CollUtil.isEmpty(options)) {
            return null;
        }
        return CollUtil.join(options, StrPool.COMMA);
    }

    private UserTask findUserTaskByRuleId(BpmnModel bpmnModel, Long id) {
        for (UserTask userTask : BpmnModelUtils.getBpmnModelElements(bpmnModel, UserTask.class)) {
            if (Objects.equals(buildTaskAssignRuleId(userTask.getId()), id)) {
                return userTask;
            }
        }
        return null;
    }

    private static Long buildTaskAssignRuleId(String taskDefinitionKey) {
        return (long) taskDefinitionKey.hashCode() & 0x7FFFFFFFL;
    }

}
