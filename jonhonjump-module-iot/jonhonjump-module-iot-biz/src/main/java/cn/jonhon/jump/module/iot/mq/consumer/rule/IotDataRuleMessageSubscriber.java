package cn.jonhon.jump.module.iot.mq.consumer.rule;

import cn.jonhon.jump.framework.tenant.core.util.TenantUtils;
import cn.jonhon.jump.module.iot.core.messagebus.core.IotMessageBus;
import cn.jonhon.jump.module.iot.core.messagebus.core.IotMessageSubscriber;
import cn.jonhon.jump.module.iot.core.mq.message.IotDeviceMessage;
import cn.jonhon.jump.module.iot.service.rule.data.IotDataRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 针对 {@link IotDeviceMessage} 的消费者，处理数据流转
 *
 * @author 中航光电
 */
@Component
@Slf4j
public class IotDataRuleMessageSubscriber implements IotMessageSubscriber<IotDeviceMessage> {

    @Resource
    private IotDataRuleService dataRuleService;

    @Resource
    private IotMessageBus messageBus;

    @PostConstruct
    public void init() {
        messageBus.register(this);
    }

    @Override
    public String getTopic() {
        return IotDeviceMessage.MESSAGE_BUS_DEVICE_MESSAGE_TOPIC;
    }

    @Override
    public String getGroup() {
        return "iot_data_rule_consumer";
    }

    @Override
    public void onMessage(IotDeviceMessage message) {
        TenantUtils.execute(message.getTenantId(), () -> dataRuleService.executeDataRule(message));
    }

}
