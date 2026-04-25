package cn.jonhon.jump.module.iot.gateway.protocol.http.handler.downstream;

import cn.jonhon.jump.module.iot.core.messagebus.core.IotMessageBus;
import cn.jonhon.jump.module.iot.core.mq.message.IotDeviceMessage;
import cn.jonhon.jump.module.iot.gateway.protocol.IotProtocol;
import cn.jonhon.jump.module.iot.gateway.protocol.AbstractIotProtocolDownstreamSubscriber;
import lombok.extern.slf4j.Slf4j;

/**
 * IoT 网关 HTTP 订阅者：接收下行给设备的消息
 *
 * @author 中航光电
 */

@Slf4j
public class IotHttpDownstreamSubscriber extends AbstractIotProtocolDownstreamSubscriber {

    public IotHttpDownstreamSubscriber(IotProtocol protocol, IotMessageBus messageBus) {
        super(protocol, messageBus);
    }

    @Override
    protected void handleMessage(IotDeviceMessage message) {
        log.info("[handleMessage][IoT 网关 HTTP 协议不支持下行消息，忽略消息：{}]", message);
    }

}
