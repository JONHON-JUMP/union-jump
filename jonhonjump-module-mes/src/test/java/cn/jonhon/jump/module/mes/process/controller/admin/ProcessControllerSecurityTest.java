package cn.jonhon.jump.module.mes.process.controller.admin;

import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
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
}
