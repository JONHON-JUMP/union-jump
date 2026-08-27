package cn.jonhon.jump.module.mes.process.controller.admin;

import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlReqVO;
import org.junit.jupiter.api.Test;

import javax.annotation.security.PermitAll;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessControllerSecurityTest {

    @Test
    void queryCardRequiresAuthenticatedAccess() throws NoSuchMethodException {
        assertFalse(ProcessController.class
                .getMethod("queryCard", ProcessCardReqVO.class)
                .isAnnotationPresent(PermitAll.class));
    }

    @Test
    void queryFileUrlRequiresAuthenticatedAccess() throws NoSuchMethodException {
        assertFalse(ProcessController.class
                .getMethod("queryFileUrl", ProcessFileUrlReqVO.class)
                .isAnnotationPresent(PermitAll.class));
    }
}
