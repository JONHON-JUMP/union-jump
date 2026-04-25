package cn.jonhon.jump.module.infra.api.logger;

import cn.jonhon.jump.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.jonhon.jump.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.jonhon.jump.module.infra.service.logger.ApiAccessLogService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * API 访问日志的 API 实现类
 *
 * @author 中航光电
 */
@Service
@Validated
public class ApiAccessLogApiImpl implements ApiAccessLogCommonApi {

    @Resource
    private ApiAccessLogService apiAccessLogService;

    @Override
    public void createApiAccessLog(ApiAccessLogCreateReqDTO createDTO) {
        apiAccessLogService.createApiAccessLog(createDTO);
    }

}
