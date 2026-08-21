package cn.jonhon.jump.module.mes.process.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class ConfigConstant {

    /**
     * ERP基础URL
     */
    @Value("${sysconfig.erpBaseUrl}")
    public String erpBaseUrl;

}
