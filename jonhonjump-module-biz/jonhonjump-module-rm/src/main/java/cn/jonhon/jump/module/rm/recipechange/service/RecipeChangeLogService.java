package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.pojo.PageParam;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeContentRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticePageReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticePageRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeExportRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeOperationLogPageRespVO;

/**
 * 工艺变更通知日志管理服务
 */
public interface RecipeChangeLogService {

    /**
     * 分页查询工艺变更通知管理页面的数据
     *
     * @param pageReqVO 页面查询条件
     * @return 通知分页数据
     */
    PageResult<RecipeChangeNoticePageRespVO> getRecipeChangeNoticePage(RecipeChangeNoticePageReqVO pageReqVO);

    /**
     * 查询导出工艺变更通知 Excel 所需的全部记录
     *
     * @param pageReqVO 页面筛选条件，不使用分页参数
     * @return 符合筛选条件的全部导出行数据
     */
    java.util.List<RecipeChangeNoticeExportRespVO> getRecipeChangeNoticeExportList(RecipeChangeNoticePageReqVO pageReqVO);

    /**
     * 查询指定通知的工艺变更内容
     *
     * @param noticeId 通知主键
     * @return 通知内容，不存在时返回 {@code null}
     */
    RecipeChangeNoticeContentRespVO getRecipeChangeNoticeContent(Long noticeId);

    /**
     * 分页查询指定通知的操作日志
     *
     * @param noticeId 通知主键
     * @param pageReqVO 分页条件
     * @return 操作日志分页数据
     */
    PageResult<RecipeChangeOperationLogPageRespVO> getRecipeChangeOperationLogPage(Long noticeId,
                                                                                     PageParam pageParam);

}
