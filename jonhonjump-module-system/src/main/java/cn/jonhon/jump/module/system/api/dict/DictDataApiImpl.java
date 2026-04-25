package cn.jonhon.jump.module.system.api.dict;

import cn.jonhon.jump.framework.common.util.object.BeanUtils;
import cn.jonhon.jump.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.jonhon.jump.module.system.dal.dataobject.dict.DictDataDO;
import cn.jonhon.jump.module.system.service.dict.DictDataService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 字典数据 API 实现类
 *
 * @author 中航光电
 */
@Service
public class DictDataApiImpl implements DictDataApi {

    @Resource
    private DictDataService dictDataService;

    @Override
    public void validateDictDataList(String dictType, Collection<String> values) {
        dictDataService.validateDictDataList(dictType, values);
    }

    @Override
    public List<DictDataRespDTO> getDictDataList(String dictType) {
        List<DictDataDO> list = dictDataService.getDictDataListByDictType(dictType);
        return BeanUtils.toBean(list, DictDataRespDTO.class);
    }

}
