package cn.jonhon.jump.module.system.service.user;

import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiConfigSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemApiTestRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemExternalRoleCreateReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.SubSystemRegisterableApiRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;

import java.util.List;

/**
 * 子系统人员接口配置 Service
 */
public interface SubSystemApiConfigService {

    List<SubSystemApiConfigRespVO> getApiConfigList();

    SubSystemApiConfigRespVO getApiConfig(Long id);

    Long createApiConfig(SubSystemApiConfigSaveReqVO createReqVO);

    /**
     * 重命名接口接入树根节点对应的业务系统显示名
     */
    void renameAccessSystem(Long subSystemId, String systemName);

    void updateApiConfig(SubSystemApiConfigSaveReqVO updateReqVO);

    void deleteApiConfig(Long id);

    /**
     * 按外部系统取启用配置（人员接口调用方使用）；未配置/已停用返回 null
     */
    SubSystemApiConfigDO getEnabledConfigBySubSystemId(Long subSystemId);

    /**
     * 测试连接：按配置调一次查询接口（page=1,rows=1），返回耗时与结果摘要
     */
    String testConnection(Long id);

    /**
     * 本页调试：按已保存配置用 HTTP 调用指定接口，返回完整地址与响应
     */
    SubSystemApiTestRespVO testInvoke(SubSystemApiTestReqVO reqVO);

    /**
     * 新增外部系统角色：调对方系统「角色新增」接口（role_create），
     * 角色名自动拼接为 车间编号_角色名称；返回最终角色名
     */
    String createExternalRole(SubSystemExternalRoleCreateReqVO reqVO);

    /**
     * 调对方「角色新增」接口推送裸角色（roleName 为最终全名，如 4200_操作员）
     */
    void pushExternalRoleCreate(Long subSystemId, String workshopCode, String externalRoleName);

    /**
     * 已启用「角色新增」接口的接入系统列表（与门户角色所属系统解耦，如 Camstar人员管理）
     */
    List<SubSystemRegisterableApiRespVO> listRoleCreateApis();

}
