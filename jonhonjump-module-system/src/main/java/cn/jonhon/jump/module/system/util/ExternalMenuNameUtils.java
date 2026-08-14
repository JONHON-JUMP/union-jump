package cn.jonhon.jump.module.system.util;

import cn.hutool.core.util.StrUtil;

/**
 * 主系统「外部系统」一级目录下菜单名称工具
 */
public final class ExternalMenuNameUtils {

    private static final String EXTERNAL_PREFIX = "外部";
    /** 主系统一级目录名称（parentId = 0） */
    public static final String EXTERNAL_SYSTEM_DIR = "外部系统";
    public static final String EXTERNAL_SYSTEM_COMPONENT_PREFIX = "system/subSystem/";

    private ExternalMenuNameUtils() {
    }

    /**
     * 规范化菜单名称：统一加「外部」前缀（已含前缀则跳过）
     */
    public static String normalizeMenuName(String menuName) {
        if (StrUtil.isBlank(menuName)) {
            return menuName;
        }
        String trimmed = menuName.trim();
        if (trimmed.startsWith(EXTERNAL_PREFIX)) {
            return trimmed;
        }
        return EXTERNAL_PREFIX + trimmed;
    }

}
