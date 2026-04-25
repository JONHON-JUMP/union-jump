package cn.jonhon.jump.framework.websocket.core.sender.local;

import cn.jonhon.jump.framework.websocket.core.sender.AbstractWebSocketMessageSender;
import cn.jonhon.jump.framework.websocket.core.sender.WebSocketMessageSender;
import cn.jonhon.jump.framework.websocket.core.session.WebSocketSessionManager;

/**
 * 本地的 {@link WebSocketMessageSender} 实现类
 *
 * 注意：仅仅适合单机场景！！！
 *
 * @author 中航光电
 */
public class LocalWebSocketMessageSender extends AbstractWebSocketMessageSender {

    public LocalWebSocketMessageSender(WebSocketSessionManager sessionManager) {
        super(sessionManager);
    }

}
