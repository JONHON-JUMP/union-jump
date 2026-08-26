package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigSaveReqVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;

import java.util.List;

/**
 * 子系统人员接口配置 Service
 */
public interface SubSystemApiConfigService {

    List<SubSystemApiConfigRespVO> getApiConfigList();

    SubSystemApiConfigRespVO getApiConfig(Long id);

    Long createApiConfig(SubSystemApiConfigSaveReqVO createReqVO);

    void updateApiConfig(SubSystemApiConfigSaveReqVO updateReqVO);

    void deleteApiConfig(Long id);

    /**
     * 按外部系统取启用配置（人员接口调用方使用）；未配置/已停用返回 null
     */
    SubSystemApiConfigDO getEnabledConfigBySubSystemId(Long subSystemId);

    /**
     * 已启用配置的系统 ID 列表
     */
    List<Long> getEnabledSubSystemIds();

    /**
     * 测试连接：按配置调一次查询接口（page=1,rows=1），返回耗时与结果摘要
     */
    String testConnection(Long id);

}
