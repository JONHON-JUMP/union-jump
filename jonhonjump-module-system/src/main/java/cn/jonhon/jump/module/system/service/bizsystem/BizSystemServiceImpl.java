package cn.jonhon.jump.module.system.service.bizsystem;

import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.framework.common.pojo.PageResult;
import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemPageReqVO;
import cn.jonhon.jump.module.system.controller.admin.bizsystem.vo.BizSystemSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.bizsystem.BizSystemDO;
import cn.jonhon.jump.module.system.dal.mysql.bizsystem.BizSystemMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.BIZ_SYSTEM_IP_PORT_DUPLICATE;
import static cn.jonhon.jump.module.system.enums.ErrorCodeConstants.BIZ_SYSTEM_NOT_EXISTS;

@Service
@Validated
public class BizSystemServiceImpl implements BizSystemService {

    @Resource
    private BizSystemMapper bizSystemMapper;

    @Override
    public Long createBizSystem(BizSystemSaveReqVO createReqVO) {
        validateIpPortUnique(null, createReqVO.getIp(), createReqVO.getPort());
        BizSystemDO row = BeanUtils.toBean(createReqVO, BizSystemDO.class);
        row.setIp(StrUtil.trim(row.getIp()));
        row.setSystemName(StrUtil.trim(row.getSystemName()));
        bizSystemMapper.insert(row);
        return row.getId();
    }

    @Override
    public void updateBizSystem(BizSystemSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        validateIpPortUnique(updateReqVO.getId(), updateReqVO.getIp(), updateReqVO.getPort());
        BizSystemDO row = BeanUtils.toBean(updateReqVO, BizSystemDO.class);
        row.setIp(StrUtil.trim(row.getIp()));
        row.setSystemName(StrUtil.trim(row.getSystemName()));
        bizSystemMapper.updateById(row);
    }

    @Override
    public void deleteBizSystem(Long id) {
        validateExists(id);
        bizSystemMapper.deleteById(id);
    }

    @Override
    public void deleteBizSystemList(List<Long> ids) {
        bizSystemMapper.deleteByIds(ids);
    }

    @Override
    public BizSystemDO getBizSystem(Long id) {
        return bizSystemMapper.selectById(id);
    }

    @Override
    public PageResult<BizSystemDO> getBizSystemPage(BizSystemPageReqVO pageReqVO) {
        return bizSystemMapper.selectPage(pageReqVO);
    }

    private void validateExists(Long id) {
        if (id == null || bizSystemMapper.selectById(id) == null) {
            throw exception(BIZ_SYSTEM_NOT_EXISTS);
        }
    }

    private void validateIpPortUnique(Long id, String ip, Integer port) {
        BizSystemDO exists = bizSystemMapper.selectByIpAndPort(StrUtil.trim(ip), port);
        if (exists == null) {
            return;
        }
        if (id == null || !exists.getId().equals(id)) {
            throw exception(BIZ_SYSTEM_IP_PORT_DUPLICATE, StrUtil.trim(ip), port);
        }
    }

}
