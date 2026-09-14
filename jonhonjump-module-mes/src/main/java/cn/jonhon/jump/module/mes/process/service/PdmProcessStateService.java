package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ErrorCode;
import cn.jonhon.jump.module.mes.process.constant.CommonConstant;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
@Service
public class PdmProcessStateService {
    @Resource
    private RestTemplate restTemplate;

    @Value("${jonhonjump.mes.process.pdm-version-url:http://192.168.240.127:4306/api/apiTransOperation/getPdmVersion}")
    private String queryUrl;

    public void requirePublished(String number) {
        String state;
        try {
            JSONObject request = new JSONObject();
            request.put("keyword", number);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.postForEntity(queryUrl,
                    new HttpEntity<>(request.toJSONString(), headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Invalid HTTP status");
            }
            state = parseState(response.getBody(), number);
        } catch (RuntimeException failure) {
            log.warn("工艺发行状态查询失败, number: {}, exception: {}", number, failure.getClass().getSimpleName());
            throw exception(new ErrorCode(500, "工艺发行状态查询失败"));
        }
        // 业务状态单独处理，避免被接口异常转换后丢失原始提示。
        if (!CommonConstant.PUBLISHED.equals(state)) {
            throw exception(new ErrorCode(500, state));
        }
    }

    private String parseState(String body, String number) {
        JSONObject outer = JSONObject.parseObject(body);
        if (outer == null || !"200".equals(outer.getString("retCode"))
                || !(outer.get("data") instanceof String)) {
            throw new IllegalArgumentException("Invalid outer response");
        }
        // 第一层data是JSON字符串，需要再次解析；第二层data才是对象数组。
        JSONObject inner = JSONObject.parseObject(outer.getString("data"));
        if (inner == null || !"200".equals(inner.getString("code"))
                || !Boolean.TRUE.equals(inner.get("success")) || !(inner.get("data") instanceof JSONArray)) {
            throw new IllegalArgumentException("Invalid inner response");
        }
        String state = null;
        for (Object item : inner.getJSONArray("data")) {
            if (!(item instanceof JSONObject)) {
                throw new IllegalArgumentException("Invalid record");
            }
            JSONObject record = (JSONObject) item;
            if (number.equals(record.getString("number"))) {
                if (state != null || !(record.get("stateDisplay") instanceof String)
                        || StringUtils.isBlank(record.getString("stateDisplay"))) {
                    throw new IllegalArgumentException("Missing or ambiguous state");
                }
                state = record.getString("stateDisplay");
            }
        }
        if (state == null) {
            throw new IllegalArgumentException("No matching process");
        }
        return state;
    }
}
