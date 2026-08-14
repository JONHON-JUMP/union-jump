package cn.jonhon.jump.module.system.dal.redis.dict;

import cn.jonhon.jump.framework.common.util.json.JsonUtils;
import cn.jonhon.jump.module.system.dal.dataobject.dict.DictDataDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.DICT_DATA_ALL;
import static cn.jonhon.jump.module.system.dal.redis.RedisKeyConstants.DICT_DATA_TYPE;

/**
 * 字典数据 Redis DAO
 */
@Repository
public class DictDataRedisDAO {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public List<DictDataDO> getListByType(String dictType) {
        String json = stringRedisTemplate.opsForValue().get(formatTypeKey(dictType));
        if (json == null) {
            return null;
        }
        return JsonUtils.parseArray(json, DictDataDO.class);
    }

    public void setListByType(String dictType, List<DictDataDO> list) {
        stringRedisTemplate.opsForValue().set(formatTypeKey(dictType),
                JsonUtils.toJsonString(list), CACHE_TTL);
    }

    public void deleteByType(String dictType) {
        stringRedisTemplate.delete(formatTypeKey(dictType));
    }

    public List<DictDataDO> getAll() {
        String json = stringRedisTemplate.opsForValue().get(DICT_DATA_ALL);
        if (json == null) {
            return null;
        }
        return JsonUtils.parseArray(json, DictDataDO.class);
    }

    public void setAll(List<DictDataDO> list) {
        stringRedisTemplate.opsForValue().set(DICT_DATA_ALL,
                JsonUtils.toJsonString(list), CACHE_TTL);
    }

    public void deleteAll() {
        stringRedisTemplate.delete(DICT_DATA_ALL);
    }

    private static String formatTypeKey(String dictType) {
        return String.format(DICT_DATA_TYPE, dictType);
    }

}
