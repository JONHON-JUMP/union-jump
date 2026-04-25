package cn.jonhon.jump.module.infra.api.logger;

import cn.jonhon.jump.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.jonhon.jump.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.jonhon.jump.module.infra.service.logger.ApiErrorLogService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * API 访问日志的 API 接口
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ApiErrorLogApiImpl implements ApiErrorLogCommonApi {

    @Resource
    private ApiErrorLogService apiErrorLogService;

    @Override
    public void createApiErrorLog(ApiErrorLogCreateReqDTO createDTO) {
        apiErrorLogService.createApiErrorLog(createDTO);
    }

}
