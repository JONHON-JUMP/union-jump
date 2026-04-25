package cn.jonhon.jump.module.bpm.api.task;

import cn.jonhon.jump.module.bpm.service.task.BpmTaskService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 流程任务 Api 实现类
 *
 * @author jason
 */
@Service
@Validated
public class BpmProcessTaskApiImpl implements BpmProcessTaskApi {

    @Resource
    private BpmTaskService bpmTaskService;

    @Override
    public void triggerTask(String processInstanceId, String taskDefineKey) {
        bpmTaskService.triggerTask(processInstanceId, taskDefineKey);
    }

}
