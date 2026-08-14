package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.dal.dataobject.user.AdminUserDO;
import cn.jonhon.jump.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.USER_UID_GENERATE_FAILED;

/**
 * 主系统跨系统用户唯一标识生成器。
 * <p>
 * 格式：{@code U + yyyyMMddHHmmss + 三位流水}，例 {@code U20260720170405001}。<br>
 * 创建、导入新建用户必须经由此类分配，禁止外部指定；库表 {@code uk_user_uid} 兜底唯一。
 */
@Component
public class UserUidGenerator {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int SEQ_MAX = 999;
    private static final int ALLOCATE_MAX_ATTEMPTS = 20;
    private static final Object LOCK = new Object();

    @Resource
    private AdminUserMapper userMapper;

    /**
     * 分配全局唯一的 user_uid（线程安全；同秒查库递增并跳过已占用号）。
     */
    public String allocate() {
        synchronized (LOCK) {
            for (int attempt = 0; attempt < ALLOCATE_MAX_ATTEMPTS; attempt++) {
                String timePart = LocalDateTime.now().format(TIME_FMT);
                String prefix = "U" + timePart;
                int nextSeq = resolveNextSeq(prefix);
                while (nextSeq <= SEQ_MAX) {
                    String candidate = prefix + String.format("%03d", nextSeq);
                    if (userMapper.selectByUserUid(candidate) == null) {
                        return candidate;
                    }
                    nextSeq++;
                }
                sleepToNextSecond();
            }
            throw exception(USER_UID_GENERATE_FAILED);
        }
    }

    /**
     * 插入用户前写入唯一 user_uid；若唯一索引冲突则重新分配并重试。
     */
    public void insertWithUniqueUid(AdminUserDO user, Runnable insertAction) {
        RuntimeException last = null;
        for (int i = 0; i < 8; i++) {
            user.setUserUid(allocate());
            try {
                insertAction.run();
                return;
            } catch (DataIntegrityViolationException ex) {
                last = ex;
                if (!isUserUidConflict(ex)) {
                    throw ex;
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw exception(USER_UID_GENERATE_FAILED);
    }

    private int resolveNextSeq(String prefix) {
        int nextSeq = 1;
        String maxUid = userMapper.selectMaxUserUidByPrefix(prefix);
        if (StrUtil.isNotBlank(maxUid) && maxUid.length() >= prefix.length() + 3) {
            String seqPart = maxUid.substring(prefix.length(), prefix.length() + 3);
            if (StrUtil.isNumeric(seqPart)) {
                nextSeq = Integer.parseInt(seqPart) + 1;
            }
        }
        return nextSeq;
    }

    private static void sleepToNextSecond() {
        try {
            Thread.sleep(1000L - (System.currentTimeMillis() % 1000L) + 5L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw exception(USER_UID_GENERATE_FAILED);
        }
    }

    private static boolean isUserUidConflict(Throwable ex) {
        Throwable cur = ex;
        while (cur != null) {
            String msg = StrUtil.blankToDefault(cur.getMessage(), "").toLowerCase();
            if (msg.contains("uk_user_uid")
                    || msg.contains("uk_system_users_user_uid")
                    || (msg.contains("user_uid") && (msg.contains("unique") || msg.contains("duplicate")))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

}
