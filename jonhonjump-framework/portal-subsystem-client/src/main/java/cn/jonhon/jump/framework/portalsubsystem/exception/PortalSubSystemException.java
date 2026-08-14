package cn.jonhon.jump.framework.portalsubsystem.exception;

/**
 * 门户子系统客户端异常。
 */
public class PortalSubSystemException extends RuntimeException {

    public PortalSubSystemException(String message) {
        super(message);
    }

    public PortalSubSystemException(String message, Throwable cause) {
        super(message, cause);
    }

}
