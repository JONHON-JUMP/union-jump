package cn.jonhon.jump.module.system.framework.subsystemapi;

import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeePageRespDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemEmployeeQueryDTO;
import cn.jonhon.jump.module.system.framework.subsystemapi.dto.SubSystemTeamComboDTO;

import java.util.List;

/**
 * 子系统人员接口统一契约（SPI）
 *
 * 每个外部系统一个适配器实现；工厂按接口配置表（sub_system_api_config.apiType）分发。
 * 新增系统：优先用 {@link cn.jonhon.jump.module.system.framework.subsystemapi.GenericHttpEmployeeApiAdapter}
 * 纯配置接入；特殊行为（如 Camstar 的 Cookie 会话）才写新适配器。
 */
public interface SubSystemEmployeeApi {

    /** 分页查询人员 */
    SubSystemEmployeePageRespDTO page(SubSystemEmployeeQueryDTO query);

    /** 新增人员（工号/姓名/车间必填） */
    void create(SubSystemEmployeeDTO employee);

    /** 修改人员（按工号 upsert） */
    void update(SubSystemEmployeeDTO employee);

    /** 删除人员（按工号；注意目标系统可能有连带副作用，如删域账号） */
    void delete(String userCode);

    /** 按车间拉班组下拉 */
    List<SubSystemTeamComboDTO> teamCombo(String workshopCode);

    /** 连通性测试：返回耗时与结果摘要 */
    String ping();

}
