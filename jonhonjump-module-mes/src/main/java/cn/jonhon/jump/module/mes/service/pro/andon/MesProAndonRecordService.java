package cn.jonhon.jump.module.mes.service.pro.andon;

import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.module.mes.controller.admin.pro.andon.vo.record.MesProAndonRecordCreateReqVO;
import cn.jonhon.jump.module.mes.controller.admin.pro.andon.vo.record.MesProAndonRecordPageReqVO;
import cn.jonhon.jump.module.mes.controller.admin.pro.andon.vo.record.MesProAndonRecordUpdateReqVO;
import cn.jonhon.jump.module.mes.dal.dataobject.pro.andon.MesProAndonRecordDO;

import javax.validation.Valid;

/**
 * MES 安灯呼叫记录 Service 接口
 *
 * @author 中航光电
 */
public interface MesProAndonRecordService {

    /**
     * 创建安灯呼叫记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAndonRecord(@Valid MesProAndonRecordCreateReqVO createReqVO);

    /**
     * 删除安灯呼叫记录
     *
     * @param id 编号
     */
    void deleteAndonRecord(Long id);

    /**
     * 更新安灯呼叫记录（保存处置信息 / 标记已处置）
     *
     * @param updateReqVO 更新信息
     */
    void updateAndonRecord(@Valid MesProAndonRecordUpdateReqVO updateReqVO);

    /**
     * 获得安灯呼叫记录
     *
     * @param id 编号
     * @return 安灯呼叫记录
     */
    MesProAndonRecordDO getAndonRecord(Long id);

    /**
     * 获得安灯呼叫记录分页
     *
     * @param pageReqVO 分页查询
     * @return 安灯呼叫记录分页
     */
    PageResult<MesProAndonRecordDO> getAndonRecordPage(MesProAndonRecordPageReqVO pageReqVO);

}
