package cn.jonhon.jump.module.system.enums.faq;

import cn.jonhon.jump.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 常见 QA 业务状态（与通知一致：草稿 / 已发布 / 已删除）
 */
@Getter
@AllArgsConstructor
public enum FaqStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    DELETED(2, "已删除");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(FaqStatusEnum::getStatus)
            .toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isDraft(Integer status) {
        return DRAFT.status.equals(status);
    }

    public static boolean isPublished(Integer status) {
        return PUBLISHED.status.equals(status);
    }

    public static boolean isDeleted(Integer status) {
        return DELETED.status.equals(status);
    }

}
