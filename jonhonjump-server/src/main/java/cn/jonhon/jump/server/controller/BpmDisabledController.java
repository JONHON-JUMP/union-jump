package cn.jonhon.jump.server.controller;

import cn.jonhon.jump.framework.common.pojo.CommonResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.jonhon.jump.framework.common.exception.enums.GlobalErrorCodeConstants.NOT_IMPLEMENTED;
import static cn.jonhon.jump.framework.common.pojo.CommonResult.error;

/**
 * BPM 模块未引入时的兜底提示。
 * 若 {@code jonhonjump-module-bpm} 已启用，则不会注册该 Controller。
 */
@RestController
@ConditionalOnMissingClass("cn.jonhon.jump.module.bpm.framework.flowable.config.BpmFlowableConfiguration")
public class BpmDisabledController {

    @RequestMapping("/admin-api/bpm/**")
    public CommonResult<Boolean> bpm404() {
        return error(NOT_IMPLEMENTED.getCode(),
                "[工作流模块 jonhonjump-module-bpm - 已禁用][参考 https://doc.iocoder.cn/bpm/ 开启]");
    }

}
