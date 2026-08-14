package cn.jonhon.jump.module.bpm.framework.flowable.core.util;

/**
 * BPM 模块布尔字段工具类。
 * <p>
 * 数据库（PostgreSQL/Kingbase 等）中布尔语义字段通常使用 smallint/tinyint 存储，
 * Java DO 使用 Integer，API VO 仍使用 Boolean。
 */
public final class BpmBooleanUtils {

    private BpmBooleanUtils() {
    }

    public static Boolean toBoolean(Integer value) {
        return value == null ? null : value != 0;
    }

    public static Integer toInteger(Boolean value) {
        if (value == null) {
            return null;
        }
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    public static boolean isTrue(Integer value) {
        return value != null && value != 0;
    }

    public static boolean isFalse(Integer value) {
        return value != null && value == 0;
    }

}
