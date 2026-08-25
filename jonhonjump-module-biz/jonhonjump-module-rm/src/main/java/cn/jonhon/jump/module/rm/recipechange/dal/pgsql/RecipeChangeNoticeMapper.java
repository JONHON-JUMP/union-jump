package cn.jonhon.jump.module.rm.recipechange.dal.pgsql;

import cn.jonhon.jump.framework.mybatis.core.mapper.BaseMapperX;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticePageReqVO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 工艺变更通知主表的数据访问接口
 */
@Mapper
public interface RecipeChangeNoticeMapper extends BaseMapperX<RecipeChangeNoticeDO> {

    /**
     * 根据 MPM 通知唯一标识查询通知主记录
     *
     * @param notifyId MPM 通知唯一标识
     * @return 对应通知记录；不存在时返回 {@code null}
     */
    default RecipeChangeNoticeDO selectByNotifyId(String notifyId) {
        return selectOne(RecipeChangeNoticeDO::getNotifyId, notifyId);
    }

    /**
     * 新增通知主记录；当 {@code notifyId} 已存在时忽略本次插入
     * 具体 PostgreSQL 幂等语句定义在同名 MyBatis XML 中
     *
     * @param notice 待新增的通知主记录
     * @return 成功插入的记录数；发生重复时返回 0
     */
    int insertIgnoreDuplicate(RecipeChangeNoticeDO notice);

    /**
     * 更新 RabbitMQ 发送成功后的通知状态
     *
     * @param id         通知主键
     * @param fromStatus 发送前状态
     * @param toStatus   发送后状态
     * @param updater    执行本次状态更新的人员或系统标识
     * @return 成功更新的记录数
     */
    int updateSendSuccess(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus,
                          @Param("updater") String updater);

    /**
     * 更新 RabbitMQ 发送失败后的通知状态和错误信息
     *
     * @param id         通知主键
     * @param fromStatus 发送前状态
     * @param toStatus   发送后状态
     * @param errorMsg   发送失败原因
     * @param updater 执行本次状态更新的人员或系统标识
     * @return 成功更新的记录数
     */
    int updateSendFailure(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus,
                          @Param("errorMsg") String errorMsg, @Param("increaseRetryCount") boolean increaseRetryCount,
                          @Param("updater") String updater);

    /** 查询所有等待自动重试的发送失败通知 */
    java.util.List<RecipeChangeNoticeDO> selectSendFailedNotices();

    /** 将自动重试次数已耗尽的通知标记为待人工处理 */
    int updatePendingManual(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    /**
     * 将指定原状态的通知标记为已完成
     *
     * @param id 通知主键
     * @param fromStatus 标记前状态
     * @param toStatus 标记后状态
     * @param updater 执行本次状态更新的管理员
     * @return 成功更新的记录数
     */
    int updateMarkedComplete(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus, @Param("updater") String updater);

    /**
     * 原子领取 MES 处理权；仅可领取待消费、MES 处理失败或处理租约已到期的通知。
     * 更新行数为 1 表示当前消费者获得唯一处理权，为 0 表示消息已终态或仍由其他消费者处理。
     *
     * @param id 通知主键
     * @param sentStatus 已发送 MQ 状态
     * @param mesProcessFailedStatus MES 处理失败状态，允许延迟回流后再次领取
     * @param processingStatus MES 处理中状态
     * @param processingToken 当前消费者生成的唯一令牌
     * @param updater 执行领取动作的系统标识
     * @return 成功领取时为 1，否则为 0
     */
    int tryAcquireProcessing(@Param("id") Long id, @Param("sentStatus") Integer sentStatus,
                             @Param("mesProcessFailedStatus") Integer mesProcessFailedStatus,
                             @Param("processingStatus") Integer processingStatus,
                             @Param("processingToken") String processingToken, @Param("updater") String updater);

    /**
     * 更新 MES 回调后的通知状态
     *
     * @param id 通知主键
     * @param fromStatus 当前状态
     * @param toStatus MES 回调后的目标状态
     * @param updater 本次状态更新人
     * @param errorMsg MES 处理失败原因，成功时为空
     * @param processingToken 当前消费者的处理令牌，必须与数据库中的有效令牌一致
     * @return 成功更新的记录数
     */
    int updateMesProcessStatus(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
                               @Param("toStatus") Integer toStatus, @Param("updater") String updater,
                               @Param("errorMsg") String errorMsg, @Param("processingToken") String processingToken);

    /**
     * 分页查询变更通知管理页面需要的通知记录
     *
     * @param page      分页对象
     * @param pageReqVO 页面筛选条件
     * @return 分页后的通知记录
     */
    Page<RecipeChangeNoticeDO> selectNoticePage(Page<RecipeChangeNoticeDO> page, @Param("pageReqVO") RecipeChangeNoticePageReqVO pageReqVO);

    /**
     * 查询导出工艺变更通知 Excel 所需的全部记录
     *
     * @param pageReqVO 页面筛选条件，不使用分页参数
     * @return 符合筛选条件的全部通知记录
     */
    java.util.List<RecipeChangeNoticeDO> selectNoticeList(@Param("pageReqVO") RecipeChangeNoticePageReqVO pageReqVO);

    /**
     * 查询“内容查询”操作需要的通知基础信息和工艺变更内容
     *
     * @param noticeId 通知主键
     * @return 通知内容记录，不存在时返回 {@code null}
     */
    RecipeChangeNoticeDO selectNoticeContentById(@Param("noticeId") Long noticeId);

}
