package cn.jonhon.jump.framework.dict.config;

import cn.jonhon.jump.framework.common.biz.system.dict.DictDataCommonApi;
import cn.jonhon.jump.framework.dict.core.DictFrameworkUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class JonhonjumpDictAutoConfiguration {

    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public DictFrameworkUtils dictUtils(DictDataCommonApi dictDataApi) {
        DictFrameworkUtils.init(dictDataApi);
        return new DictFrameworkUtils();
    }

}
