package cn.jonhon.jump.module.mes.controller.admin.wm.itemconsume;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.mes.controller.admin.wm.itemconsume.vo.MesWmItemConsumeLinePageReqVO;
import cn.jonhon.jump.module.mes.controller.admin.wm.itemconsume.vo.MesWmItemConsumeLineRespVO;
import cn.jonhon.jump.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.jonhon.jump.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.jonhon.jump.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeLineDO;
import cn.jonhon.jump.module.mes.service.md.item.MesMdItemService;
import cn.jonhon.jump.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.jonhon.jump.module.mes.service.wm.itemconsume.MesWmItemConsumeLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

import static cn.jonhon.jump.framework.common.pojo.CommonResult.success;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.jonhon.jump.framework.common.util.collection.MapUtils.findAndThen;

@Tag(name = "管理后台 - MES 物料消耗记录行")
@RestController
@RequestMapping("/mes/wm/item-consume-line")
@Validated
public class MesWmItemConsumeLineController {

    @Resource
    private MesWmItemConsumeLineService itemConsumeLineService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdUnitMeasureService unitMeasureService;

    @GetMapping("/page")
    @Operation(summary = "根据报工编号分页获取消耗行列表")
    @PreAuthorize("@ss.hasPermission('mes:pro-feedback:query')")
    public CommonResult<PageResult<MesWmItemConsumeLineRespVO>> getItemConsumeLinePage(
            @Valid MesWmItemConsumeLinePageReqVO pageReqVO) {
        // 1. 分页查询消耗行
        PageResult<MesWmItemConsumeLineDO> pageResult =
                itemConsumeLineService.getItemConsumeLinePage(pageReqVO);
        PageResult<MesWmItemConsumeLineRespVO> voPage = BeanUtils.toBean(pageResult, MesWmItemConsumeLineRespVO.class);
        // 2. 拼接物料信息
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(
                convertSet(pageResult.getList(), MesWmItemConsumeLineDO::getItemId));
        Map<Long, MesMdUnitMeasureDO> unitMap = unitMeasureService.getUnitMeasureMap(
                convertSet(itemMap.values(), MesMdItemDO::getUnitMeasureId));
        for (MesWmItemConsumeLineRespVO vo : voPage.getList()) {
            findAndThen(itemMap, vo.getItemId(), item -> {
                vo.setItemCode(item.getCode()).setItemName(item.getName()).setSpecification(item.getSpecification());
                findAndThen(unitMap, item.getUnitMeasureId(), unit -> vo.setUnitName(unit.getName()));
            });
        }
        return success(voPage);
    }

}
