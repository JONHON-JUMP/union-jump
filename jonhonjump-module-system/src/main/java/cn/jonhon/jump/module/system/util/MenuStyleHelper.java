package cn.jonhon.jump.module.system.util;

import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuColorDO;
import cn.jonhon.jump.module.system.dal.dataobject.permission.MenuDO;

import java.util.Map;

/**
 * 菜单颜色继承：仅一级菜单（parentId = 0）可配置颜色，子菜单继承一级菜单颜色。
 */
public final class MenuStyleHelper {

    public static final Long DEFAULT_STYLE_ID = 10L;
    public static final String DEFAULT_COLOR = "#3A71A8";
    public static final String DEFAULT_SHAPE = "rounded";

    private MenuStyleHelper() {
    }

    public static MenuColorDO defaultStyle() {
        MenuColorDO style = new MenuColorDO();
        style.setId(DEFAULT_STYLE_ID);
        style.setName("中性蓝·通用默认");
        style.setColor(DEFAULT_COLOR);
        style.setShape(DEFAULT_SHAPE);
        return style;
    }

    public static boolean isFirstLevelMenu(Long parentId) {
        return MenuDO.ID_ROOT.equals(parentId);
    }

    public static MenuColorDO resolveFirstLevelStyle(Long styleId, Map<Long, MenuColorDO> colorMap) {
        if (styleId != null && colorMap != null && !colorMap.isEmpty()) {
            MenuColorDO style = colorMap.get(styleId);
            if (style != null) {
                return style;
            }
        }
        return defaultStyle();
    }

    /**
     * @param parentId        当前菜单 parentId
     * @param styleId         当前菜单 styleId（仅一级菜单有效）
     * @param firstLevelStyle 一级菜单已解析的颜色
     */
    public static MenuColorDO resolveEffectiveStyle(Long parentId, Long styleId,
                                                    MenuColorDO firstLevelStyle,
                                                    Map<Long, MenuColorDO> colorMap) {
        if (isFirstLevelMenu(parentId)) {
            return resolveFirstLevelStyle(styleId, colorMap);
        }
        return firstLevelStyle != null ? firstLevelStyle : defaultStyle();
    }

    public static void applyStyle(MenuColorDO style, StyleTarget target) {
        if (target == null) {
            return;
        }
        MenuColorDO effective = style != null ? style : defaultStyle();
        target.setStyleId(effective.getId());
        target.setColor(effective.getColor());
        target.setShape(effective.getShape() != null ? effective.getShape() : DEFAULT_SHAPE);
    }

    public interface StyleTarget {
        void setStyleId(Long styleId);

        void setColor(String color);

        void setShape(String shape);
    }

}
