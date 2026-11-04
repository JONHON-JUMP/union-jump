package cn.jonhon.jump.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingItemReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingListRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingSaveReqVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiBindingSceneRespVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiEndpointOptionVO;
import cn.jonhon.jump.module.system.controller.admin.user.vo.subsystem.LeafApiOptionRespVO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiBindingDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiConfigDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemApiEndpointDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemDO;
import cn.jonhon.jump.module.system.dal.dataobject.user.SubSystemWorkshopDO;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiBindingMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiConfigMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemApiEndpointMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemMapper;
import cn.jonhon.jump.module.system.dal.mysql.user.SubSystemWorkshopMapper;
import cn.jonhon.jump.module.system.enums.user.LeafApiSceneEnum;
import cn.jonhon.jump.module.system.framework.subsystemapi.ApiEndpointSpecs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.jonhon.jump.framework.common.util.collection.CollectionUtils.convertMap;

@Service
@Validated
public class LeafApiBindingServiceImpl implements LeafApiBindingService {

    @Resource
    private SubSystemApiBindingMapper bindingMapper;
    @Resource
    private SubSystemApiConfigMapper apiConfigMapper;
    @Resource
    private SubSystemApiEndpointMapper endpointMapper;
    @Resource
    private SubSystemMapper subSystemMapper;
    @Resource
    private SubSystemWorkshopMapper workshopMapper;
    @Resource
    private SubSystemAccessService subSystemAccessService;

    @Override
    public LeafApiBindingListRespVO getLeafBindings(Long bizSubSystemId, String workshopCode) {
        String code = normalizeCode(workshopCode);
        List<SubSystemApiBindingDO> rows = bizSubSystemId == null
                ? Collections.emptyList()
                : bindingMapper.selectListByLeaf(bizSubSystemId, code);
        Map<Long, SubSystemDO> systems = loadSystems(rows.stream()
                .map(SubSystemApiBindingDO::getApiSubSystemId).collect(Collectors.toList()));
        Map<Long, SubSystemApiConfigDO> configs = loadConfigs(rows.stream()
                .map(SubSystemApiBindingDO::getApiSubSystemId).collect(Collectors.toList()));
        List<LeafApiBindingRespVO> bindings = new ArrayList<>();
        for (SubSystemApiBindingDO row : rows) {
            LeafApiBindingRespVO vo = new LeafApiBindingRespVO();
            vo.setScene(row.getScene());
            vo.setApiSubSystemId(row.getApiSubSystemId());
            vo.setEndpointId(row.getEndpointId());
            SubSystemDO system = systems.get(row.getApiSubSystemId());
            if (system != null) {
                vo.setApiSystemName(system.getSystemName());
            }
            SubSystemApiConfigDO config = configs.get(row.getApiSubSystemId());
            if (config != null) {
                vo.setApiType(config.getApiType());
            }
            if (row.getEndpointId() != null) {
                SubSystemApiEndpointDO endpoint = endpointMapper.selectById(row.getEndpointId());
                if (endpoint != null) {
                    vo.setEndpointName(endpoint.getName());
                    vo.setEndpointUrl(endpoint.getUrl());
                    vo.setPurpose(endpoint.getPurpose());
                }
            }
            bindings.add(vo);
        }
        LeafApiBindingListRespVO resp = new LeafApiBindingListRespVO();
        resp.setScenes(sceneCatalog());
        resp.setBindings(bindings);
        return resp;
    }

    @Override
    public List<LeafApiOptionRespVO> getApiOptions() {
        List<SubSystemApiConfigDO> configs = apiConfigMapper.selectList();
        if (CollUtil.isEmpty(configs)) {
            return Collections.emptyList();
        }
        Map<Long, SubSystemDO> systems = loadSystems(configs.stream()
                .map(SubSystemApiConfigDO::getSubSystemId).collect(Collectors.toList()));
        List<LeafApiOptionRespVO> options = new ArrayList<>();
        for (SubSystemApiConfigDO config : configs) {
            if (config.getSubSystemId() == null || !systems.containsKey(config.getSubSystemId())) {
                continue;
            }
            LeafApiOptionRespVO option = new LeafApiOptionRespVO();
            option.setSubSystemId(config.getSubSystemId());
            option.setSystemName(systems.get(config.getSubSystemId()).getSystemName());
            option.setApiType(config.getApiType());
            List<LeafApiEndpointOptionVO> endpoints = new ArrayList<>();
            for (SubSystemApiEndpointDO endpoint : endpointMapper.selectListBySubSystemId(config.getSubSystemId())) {
                if (endpoint == null || StrUtil.isBlank(endpoint.getPurpose())) {
                    continue;
                }
                LeafApiEndpointOptionVO item = new LeafApiEndpointOptionVO();
                item.setId(endpoint.getId());
                item.setName(StrUtil.blankToDefault(endpoint.getName(), purposeLabel(endpoint.getPurpose())));
                item.setPurpose(endpoint.getPurpose());
                item.setPurposeLabel(purposeLabel(endpoint.getPurpose()));
                item.setUrl(endpoint.getUrl());
                item.setMethod(endpoint.getMethod());
                item.setEnabled(endpoint.getEnabled() == null || endpoint.getEnabled() == 1);
                endpoints.add(item);
            }
            option.setEndpoints(endpoints);
            options.add(option);
        }
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLeafBindings(LeafApiBindingSaveReqVO reqVO) {
        if (reqVO.getBizSubSystemId() == null) {
            throw exception0(BAD_REQUEST.getCode(), "业务系统不能为空");
        }
        subSystemAccessService.checkAccessible(reqVO.getBizSubSystemId());
        String code = normalizeCode(reqVO.getWorkshopCode());
        assertLeafExists(reqVO.getBizSubSystemId(), code);
        Map<String, SubSystemApiBindingDO> existing = new LinkedHashMap<>();
        for (SubSystemApiBindingDO row : bindingMapper.selectListByLeaf(reqVO.getBizSubSystemId(), code)) {
            existing.put(row.getScene(), row);
        }
        for (LeafApiBindingItemReqVO item : CollUtil.emptyIfNull(reqVO.getItems())) {
            LeafApiSceneEnum scene = LeafApiSceneEnum.ofScene(item.getScene());
            if (scene == null) {
                throw exception0(BAD_REQUEST.getCode(), "无法识别的页面调用");
            }
            boolean clear = item.getApiSubSystemId() == null && item.getEndpointId() == null;
            SubSystemApiBindingDO current = existing.get(scene.getScene());
            if (clear) {
                if (current != null) {
                    bindingMapper.deleteById(current.getId());
                }
                continue;
            }
            if (item.getApiSubSystemId() == null || item.getEndpointId() == null) {
                throw exception0(BAD_REQUEST.getCode(),
                        "「" + scene.getLabel() + "」需要同时选择接口系统和具体接口");
            }
            SubSystemApiEndpointDO endpoint = endpointMapper.selectById(item.getEndpointId());
            if (endpoint == null || !item.getApiSubSystemId().equals(endpoint.getSubSystemId())) {
                throw exception0(BAD_REQUEST.getCode(),
                        "「" + scene.getLabel() + "」选择的接口不属于该接口系统");
            }
            if (endpoint.getEnabled() != null && endpoint.getEnabled() != 1) {
                throw exception0(BAD_REQUEST.getCode(),
                        "「" + scene.getLabel() + "」选择的接口已停用");
            }
            if (!scene.getPurpose().equals(StrUtil.trim(endpoint.getPurpose()))) {
                throw exception0(BAD_REQUEST.getCode(),
                        "「" + scene.getLabel() + "」只能绑定用途为「" + scene.getPurposeLabel() + "」的接口");
            }
            if (current == null) {
                SubSystemApiBindingDO row = new SubSystemApiBindingDO();
                row.setBizSubSystemId(reqVO.getBizSubSystemId());
                row.setWorkshopCode(code);
                row.setScene(scene.getScene());
                row.setApiSubSystemId(item.getApiSubSystemId());
                row.setEndpointId(item.getEndpointId());
                bindingMapper.insert(row);
            } else {
                current.setApiSubSystemId(item.getApiSubSystemId());
                current.setEndpointId(item.getEndpointId());
                bindingMapper.updateById(current);
            }
        }
    }

    @Override
    public Long findApiSubSystemId(Long bizSubSystemId, String workshopCode, LeafApiSceneEnum scene) {
        if (bizSubSystemId == null || scene == null) {
            return null;
        }
        SubSystemApiBindingDO row = bindingMapper.selectByLeafAndScene(
                bizSubSystemId, normalizeCode(workshopCode), scene.getScene());
        return row == null ? null : row.getApiSubSystemId();
    }

    @Override
    public Long requireApiSubSystemId(Long bizSubSystemId, String workshopCode, LeafApiSceneEnum scene) {
        Long apiSubSystemId = findApiSubSystemId(bizSubSystemId, workshopCode, scene);
        if (apiSubSystemId == null) {
            String leaf = StrUtil.isBlank(workshopCode) ? "该业务系统" : ("叶子「" + workshopCode.trim() + "」");
            throw exception0(BAD_REQUEST.getCode(),
                    leaf + "的「" + scene.getLabel() + "」尚未绑定外部接口。请到【业务系统叶子】的「接口绑定」中，先选择接口系统，再选择具体接口。");
        }
        return apiSubSystemId;
    }

    private void assertLeafExists(Long bizSubSystemId, String workshopCode) {
        if (StrUtil.isBlank(workshopCode)) {
            return;
        }
        SubSystemWorkshopDO leaf = workshopMapper.selectByWorkshopCode(workshopCode);
        if (leaf == null || !bizSubSystemId.equals(leaf.getSubSystemId())) {
            throw exception0(BAD_REQUEST.getCode(), "叶子不存在或不属于当前业务系统");
        }
    }

    private List<LeafApiBindingSceneRespVO> sceneCatalog() {
        return Arrays.stream(LeafApiSceneEnum.values()).map(scene -> {
            LeafApiBindingSceneRespVO vo = new LeafApiBindingSceneRespVO();
            vo.setScene(scene.getScene());
            vo.setLabel(scene.getLabel());
            vo.setPurpose(scene.getPurpose());
            vo.setPurposeLabel(scene.getPurposeLabel());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, SubSystemDO> loadSystems(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(id -> id != null).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(distinct)) {
            return Collections.emptyMap();
        }
        return convertMap(subSystemMapper.selectListByIds(distinct), SubSystemDO::getId);
    }

    private Map<Long, SubSystemApiConfigDO> loadConfigs(List<Long> subSystemIds) {
        Map<Long, SubSystemApiConfigDO> map = new LinkedHashMap<>();
        for (Long id : subSystemIds.stream().filter(item -> item != null).distinct().collect(Collectors.toList())) {
            SubSystemApiConfigDO config = apiConfigMapper.selectBySubSystemId(id);
            if (config != null) {
                map.put(id, config);
            }
        }
        return map;
    }

    private static String purposeLabel(String purpose) {
        LeafApiSceneEnum scene = Arrays.stream(LeafApiSceneEnum.values())
                .filter(item -> item.getPurpose().equals(purpose))
                .findFirst().orElse(null);
        if (scene != null) {
            return scene.getPurposeLabel();
        }
        if (ApiEndpointSpecs.UPDATE.equals(purpose)) {
            return "修改人员";
        }
        if (ApiEndpointSpecs.DELETE.equals(purpose)) {
            return "删除人员";
        }
        if (ApiEndpointSpecs.ROLE_DELETE.equals(purpose)) {
            return "删除角色";
        }
        if (ApiEndpointSpecs.TEAM_COMBO.equals(purpose)) {
            return "班组下拉";
        }
        return StrUtil.blankToDefault(purpose, "接口");
    }

    private static String normalizeCode(String workshopCode) {
        return StrUtil.blankToDefault(StrUtil.trim(workshopCode), "");
    }

}
