package cn.jonhon.jump.module.mes.controller.admin.wm.productproduce;

import cn.hutool.core.collection.CollUtil;
import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.collection.MapUtils;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.mes.controller.admin.wm.productproduce.vo.MesWmProductProduceLinePageReqVO;
import cn.jonhon.jump.module.mes.controller.admin.wm.productproduce.vo.MesWmProductProduceLineRespVO;
import cn.jonhon.jump.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.jonhon.jump.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceLineDO;
import cn.jonhon.jump.module.mes.service.md.item.MesMdItemService;
import cn.jonhon.jump.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.jonhon.jump.module.mes.service.wm.productproduce.MesWmProductProduceLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - MES 生产入库单行")
@RestController
@RequestMapping("/mes/wm/product-produce-line")
@Validated
public class MesWmProductProduceLineController {

    @Resource
    private MesWmProductProduceLineService productProduceLineService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;

    @GetMapping("/page")
    @Operation(summary = "获得生产入库单行分页")
    @PreAuthorize("@ss.hasPermission('mes:wm-product-produce:query')")
    public CommonResult<PageResult<MesWmProductProduceLineRespVO>> getProductProduceLinePage(
            @Valid MesWmProductProduceLinePageReqVO pageReqVO) {
        PageResult<MesWmProductProduceLineDO> pageResult = productProduceLineService.getProductProduceLinePage(
                pageReqVO);
        return success(new PageResult<>(buildRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    // ==================== 拼接 VO ====================

    private List<MesWmProductProduceLineRespVO> buildRespVOList(List<MesWmProductProduceLineDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 1. 获得关联数据
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(list, MesWmProductProduceLineDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMeasureMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        // 2. 构建结果
        return BeanUtils.toBean(list, MesWmProductProduceLineRespVO.class, vo -> {
            MapUtils.findAndThen(itemMap, vo.getItemId(), item -> {
                vo.setItemCode(item.getCode()).setItemName(item.getName()).setSpecification(item.getSpecification());
                MapUtils.findAndThen(unitMeasureMap, item.getUnitMeasureId(),
                        unitMeasure -> vo.setUnitMeasureName(unitMeasure.getName()));
            });
        });
    }

}
