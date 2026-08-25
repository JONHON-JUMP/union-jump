package cn.jonhon.jump.module.rm.recipechange.service;

import cn.jonhon.jump.framework.common.pojo.PageParam;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.mybatis.core.util.MyBatisUtils;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeContentRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticePageReqVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticePageRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeNoticeExportRespVO;
import cn.jonhon.jump.module.rm.recipechange.controller.admin.vo.RecipeChangeOperationLogPageRespVO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeNoticeDO;
import cn.jonhon.jump.module.rm.recipechange.dal.dataobject.RecipeChangeOperationLogDO;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeNoticeMapper;
import cn.jonhon.jump.module.rm.recipechange.dal.pgsql.RecipeChangeOperationLogMapper;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeNoticeStatusEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationResultEnum;
import cn.jonhon.jump.module.rm.recipechange.enums.RecipeChangeOperationTypeEnum;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 工艺变更通知日志管理服务实现
 */
@Service
public class RecipeChangeLogServiceImpl implements RecipeChangeLogService {

    /**
     * 工艺变更通知主表 Mapper
     */
    @Resource
    private RecipeChangeNoticeMapper recipeChangeNoticeMapper;

    /**
     * 工艺变更操作日志 Mapper
     */
    @Resource
    private RecipeChangeOperationLogMapper recipeChangeOperationLogMapper;

    /**
     * 分页查询变更通知管理页面的数据，并补充状态中文名称
     *
     * @param pageReqVO 页面查询条件
     * @return 通知分页数据
     */
    @Override
    public PageResult<RecipeChangeNoticePageRespVO> getRecipeChangeNoticePage(RecipeChangeNoticePageReqVO pageReqVO) {
        // 根据页面参数构建 MyBatis-Plus 分页对象，并由 XML 中的查询语句完成筛选和排序
        Page<RecipeChangeNoticeDO> page = recipeChangeNoticeMapper.selectNoticePage(MyBatisUtils.buildPage(pageReqVO), pageReqVO);
        PageResult<RecipeChangeNoticeDO> pageResult = new PageResult<>(page.getRecords(), page.getTotal());
        // 列表只返回原型展示字段，不返回体积较大的 changeContent
        return BeanUtils.toBean(pageResult, RecipeChangeNoticePageRespVO.class,
                recipeChangeNoticePageRespVO -> recipeChangeNoticePageRespVO.setStatusName(
                        getNoticeStatusName(recipeChangeNoticePageRespVO.getStatus())));
    }

    /**
     * 查询 Excel 导出的全部通知，并补充与管理页面一致的状态中文名称
     *
     * @param pageReqVO 页面筛选条件，不使用分页参数
     * @return 可直接写入 Excel 的全部通知行数据
     */
    @Override
    public List<RecipeChangeNoticeExportRespVO> getRecipeChangeNoticeExportList(RecipeChangeNoticePageReqVO pageReqVO) {
        // 导出查询复用管理页面的所有筛选条件，但不设置分页限制以获取全部匹配记录
        List<RecipeChangeNoticeDO> recipeChangeNotices = recipeChangeNoticeMapper.selectNoticeList(pageReqVO);
        // 将状态编码转换为中文名称，保证 Excel 内容与页面展示一致
        return BeanUtils.toBean(recipeChangeNotices, RecipeChangeNoticeExportRespVO.class,
                recipeChangeNoticeExportRespVO -> recipeChangeNoticeExportRespVO.setStatusName(
                        getNoticeStatusName(recipeChangeNoticeExportRespVO.getStatus())));
    }

    /**
     * 查询指定通知的原始工艺变更 JSON 内容
     *
     * @param noticeId 通知主键
     * @return 通知内容，不存在时返回 {@code null}
     */
    @Override
    public RecipeChangeNoticeContentRespVO getRecipeChangeNoticeContent(Long noticeId) {
        // 单独查询内容，避免管理列表查询时重复传输 changeContent
        RecipeChangeNoticeDO recipeChangeNotice = recipeChangeNoticeMapper.selectNoticeContentById(noticeId);
        return BeanUtils.toBean(recipeChangeNotice, RecipeChangeNoticeContentRespVO.class);
    }

    /**
     * 分页查询指定通知的操作日志，并补充操作类型和结果中文名称
     *
     * @param noticeId 通知主键
     * @return 操作日志分页数据
     */
    @Override
    public PageResult<RecipeChangeOperationLogPageRespVO> getRecipeChangeOperationLogPage(Long noticeId, PageParam pageParam) {
        // 操作日志按实际发生时间正序返回，以便页面按处理链路阅读
        Page<RecipeChangeOperationLogDO> page = recipeChangeOperationLogMapper.selectOperationLogPage(MyBatisUtils.buildPage(pageParam), noticeId);
        PageResult<RecipeChangeOperationLogDO> pageResult = new PageResult<>(page.getRecords(), page.getTotal());
        return BeanUtils.toBean(pageResult, RecipeChangeOperationLogPageRespVO.class,
                recipeChangeOperationLogPageRespVO -> {
                    recipeChangeOperationLogPageRespVO.setOperationTypeName(
                            getOperationTypeName(recipeChangeOperationLogPageRespVO.getOperationType()));
                    recipeChangeOperationLogPageRespVO.setOperationResultName(
                            getOperationResultName(recipeChangeOperationLogPageRespVO.getOperationResult()));
                });
    }

    /**
     * 将状态编码转换为页面展示的中文名称
     *
     * @param status 状态编码
     * @return 状态中文名称，未定义编码时返回 {@code null}
     */
    private String getNoticeStatusName(Integer status) {
        return Arrays.stream(RecipeChangeNoticeStatusEnum.values())
                .filter(recipeChangeNoticeStatusEnum -> recipeChangeNoticeStatusEnum.getStatus().equals(status))
                .map(RecipeChangeNoticeStatusEnum::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * 将操作类型编码转换为页面展示的中文名称
     *
     * @param operationType 操作类型编码
     * @return 操作类型中文名称，未定义编码时返回 {@code null}
     */
    private String getOperationTypeName(Integer operationType) {
        return Arrays.stream(RecipeChangeOperationTypeEnum.values())
                .filter(recipeChangeOperationTypeEnum -> recipeChangeOperationTypeEnum.getType().equals(operationType))
                .map(RecipeChangeOperationTypeEnum::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * 将操作结果编码转换为页面展示的中文名称
     *
     * @param operationResult 操作结果编码
     * @return 操作结果中文名称，未定义编码时返回 {@code null}
     */
    private String getOperationResultName(Integer operationResult) {
        return Arrays.stream(RecipeChangeOperationResultEnum.values())
                .filter(recipeChangeOperationResultEnum -> recipeChangeOperationResultEnum.getResult().equals(operationResult))
                .map(RecipeChangeOperationResultEnum::getName)
                .findFirst()
                .orElse(null);
    }

}
