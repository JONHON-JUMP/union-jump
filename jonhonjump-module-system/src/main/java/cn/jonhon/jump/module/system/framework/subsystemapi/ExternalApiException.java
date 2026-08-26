package cn.jonhon.jump.module.system.framework.subsystemapi;

/**
 * 子系统人员接口调用异常
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
