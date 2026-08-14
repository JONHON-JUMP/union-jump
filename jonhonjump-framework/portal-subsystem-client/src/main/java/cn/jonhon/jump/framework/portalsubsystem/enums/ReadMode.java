package cn.jonhon.jump.framework.portalsubsystem.enums;

/**
 * 读模式。
 */
public enum ReadMode {

    REDIS,
    HTTP;

    public static ReadMode from(String value) {
        if (value == null) {
            return REDIS;
        }
        return "http".equalsIgnoreCase(value) ? HTTP : REDIS;
    }

}
