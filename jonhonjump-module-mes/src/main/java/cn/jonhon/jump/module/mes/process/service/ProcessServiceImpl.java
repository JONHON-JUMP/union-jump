package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ErrorCode;
import cn.jonhon.jump.module.mes.process.constant.CommonConstant;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessFileUrlRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.FormalProcessReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.TemporaryProcessReqVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import cn.jonhon.jump.module.mes.process.enums.YesOrNo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
@Component
public class ProcessServiceImpl implements ProcessService{
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private CaoeTableMapper caoeTableMapper;
    @Resource
    private TemporaryProcessTreeAssembler temporaryProcessTreeAssembler;
    @Resource
    private FormalProcessTreeAssembler formalProcessTreeAssembler;

    /**
     * 临时工艺信息查询接口地址
     */
    @Value("${jonhonjump.mes.process.temporary-process-url}")
    private String temporaryProcessUrl;

    /**
     * 临时工艺信息查询接口请求头 Authorization 的值
     */
    @Value("${jonhonjump.mes.process.temporary-process-token:}")
    private String temporaryProcessToken;

    /**
     * MPM 正式工艺版本查询接口地址
     */
    @Value("${jonhonjump.mes.process.formal-process-url}")
    private String formalProcessUrl;

    /**
     * MPM 正式工艺版本查询接口的 X-Access-Token
     */
    @Value("${jonhonjump.mes.process.mpm-access-token}")
    private String mpmAccessToken;

    /**
     * MPM 工艺文件地址查询接口地址
     */
    @Value("${jonhonjump.mes.process.process-file-url}")
    private String processFileUrl;

    /**
     * 查看工艺卡片
     * @param reqVO
     * @return
     */
    @Override
    public List<ProcessCardRespVO> queryCard(ProcessCardReqVO reqVO) {

        // 正式工艺
        if (reqVO.getAccno().startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX)) {
            return queryFormalCard(reqVO.getAccno());
        }
        // 临时工艺
        else {

            if (StringUtils.isEmpty(reqVO.getPrtno())) {
                throw exception(new ErrorCode(500, "临时工艺必须输入物料号"));
            }

            int isFix = reqVO.getAccno().length() > 4
                    ? YesOrNo.YES.getType() : YesOrNo.NO.getType();
            TemporaryProcessReqVO temporaryProcessReqVO = TemporaryProcessReqVO.builder()
                    .prtno(reqVO.getPrtno())
                    .accno(reqVO.getAccno())
                    .plndept(reqVO.getAccno().substring(0, Math.min(4, reqVO.getAccno().length())))
                    .fxtype(String.valueOf(isFix))
                    .build();
            String reqParam = JSON.toJSONString(temporaryProcessReqVO);

            JSONObject responseBodyJsonObject = queryTemporaryProcessInfo(reqParam);
            log.info("临时工艺响应信息:{}", responseBodyJsonObject);

            if (responseBodyJsonObject == null) {
                throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
            }
            String oid = responseBodyJsonObject.getString(CommonConstant.OID);
            if (StringUtils.isEmpty(oid)) {
                throw exception(new ErrorCode(500, "临时工艺信息缺少oid"));
            }

            Object detailsPayload = responseBodyJsonObject.get(CommonConstant.DETAILS);
            if (!(detailsPayload instanceof JSONArray) || ((JSONArray) detailsPayload).isEmpty()) {
                throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
            }

            JSONArray jsonArray = (JSONArray) detailsPayload;
            for (Object detail : jsonArray) {
                if (!(detail instanceof JSONObject)) {
                    throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
                }
            }

            // 检查工艺版本是否发行
            String docNumber;
            if (reqVO.getAccno().length() > 4) {
                docNumber = responseBodyJsonObject.getString(CommonConstant.ROUTNUMBER);
            }
            else {
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                docNumber = jsonObject.getString(CommonConstant.ROUTREMARK);
            }

            if (StringUtils.isEmpty(docNumber)) {
                throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
            }

            log.info("docNumber:{}", docNumber);
            CaoeDocInfoDTO caoeDocInfoDTO = caoeTableMapper.queryDocInfo(docNumber);
            if (caoeDocInfoDTO == null) {
                throw exception(new ErrorCode(500, "临时工艺文档信息不存在"));
            }
            log.info("caoeDocInfoDTO:{}", caoeDocInfoDTO);
            if (!CommonConstant.PUBLISHED.equals(caoeDocInfoDTO.getDocState())) {
                throw exception(new ErrorCode(500, "工艺未发行，无法查看"));
            }
            if (StringUtils.isBlank(caoeDocInfoDTO.getOid())) {
                throw exception(new ErrorCode(500, "临时工艺查看地址缺失"));
            }

            List<ProcessCardDetailsRespVO> details = temporaryProcessTreeAssembler
                    .assemble(jsonArray, caoeDocInfoDTO.getOid());
            ProcessCardRespVO card = ProcessCardRespVO.builder()
                    .accno(reqVO.getAccno())
                    .version(null)
                    .isFormal(YesOrNo.NO.getType())
                    .isFix(isFix)
                    .details(details)
                    .build();
            return Collections.singletonList(card);
        }
    }

    /**
     * 直连 ERP 数据平台接口查询临时工艺信息
     */
    private JSONObject queryTemporaryProcessInfo(String reqParam) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.set("Authorization", temporaryProcessToken);
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(temporaryProcessUrl,
                    new HttpEntity<>(reqParam, headers), String.class);
        } catch (RestClientException requestException) {
            log.error("调用临时工艺查询接口失败, url: {}", temporaryProcessUrl, requestException);
            throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
        }
        return parseTemporaryProcessResponse(response.getBody());
    }

    private JSONObject parseTemporaryProcessResponse(String body) {
        if (StringUtils.isEmpty(body)) {
            throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
        }

        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(body);
        } catch (RuntimeException parseException) {
            throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
        }
        if (jsonObject == null
                || !jsonObject.containsKey(CommonConstant.RETCODE)
                || !String.valueOf(HttpStatus.SC_OK).equals(jsonObject.getString(CommonConstant.RETCODE))
                || !jsonObject.containsKey(CommonConstant.RESPONSEBODY)
                || !(jsonObject.get(CommonConstant.RESPONSEBODY) instanceof JSONObject)
        ) {
            throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
        }
        return jsonObject.getJSONObject(CommonConstant.RESPONSEBODY);
    }

    private List<ProcessCardRespVO> queryFormalCard(String accno) {
        FormalProcessReqVO request = FormalProcessReqVO.builder()
                .objType(CommonConstant.OBJTYPE)
                .objNumbers(Collections.singletonList(accno))
                .isLatest(CommonConstant.TRUE)
                .build();

        String version = queryFormalVersion(request);

        String state = caoeTableMapper.queryProcessState(accno, version);
        if (!CommonConstant.PUBLISHED.equals(state)) {
            throw exception(new ErrorCode(500, "工艺未发行，无法查看"));
        }

        boolean mpm = accno.startsWith(CommonConstant.MPM_FORMAL_ACCNO_PREFIX);
        List<ProcessCardDetailsRespVO> details = formalProcessTreeAssembler.assemble(accno, version, mpm);
        ProcessCardRespVO card = ProcessCardRespVO.builder()
                .accno(accno)
                .version(version)
                .isFormal(YesOrNo.YES.getType())
                .isFix(YesOrNo.NO.getType())
                .details(details)
                .build();
        return Collections.singletonList(card);
    }

    /**
     * 直连 MPM 平台接口查询正式工艺版本
     */
    private String queryFormalVersion(FormalProcessReqVO request) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(formalProcessUrl,
                    new HttpEntity<>(JSON.toJSONString(request), buildMpmHeaders()), String.class);
        } catch (RestClientException requestException) {
            log.error("调用正式工艺版本查询接口失败, url: {}", formalProcessUrl, requestException);
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        return parseFormalVersionResponse(response.getBody());
    }

    /**
     * MPM 平台接口公共请求头
     */
    private HttpHeaders buildMpmHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.set("X-Access-Token", mpmAccessToken);
        headers.set("Accept-User", CommonConstant.MPM_VIEW_USER);
        return headers;
    }

    private String parseFormalVersionResponse(String body) {
        if (StringUtils.isBlank(body)) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        JSONObject envelope;
        try {
            envelope = JSONObject.parseObject(body);
        } catch (RuntimeException parseException) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        Object code = envelope == null ? null : envelope.get("code");
        if (envelope == null
                || !Boolean.TRUE.equals(envelope.get("success"))
                || !(code instanceof Number)
                || ((Number) code).intValue() != HttpStatus.SC_OK
                || !(envelope.get("result") instanceof JSONArray)) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        JSONArray result = envelope.getJSONArray("result");
        if (result.isEmpty() || !(result.get(0) instanceof JSONObject)) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        Object versionValue = result.getJSONObject(0).get("version");
        if (!(versionValue instanceof String)) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        String fullVersion = ((String) versionValue).trim();
        int separatorIndex = fullVersion.indexOf('.');
        String version = fullVersion.substring(0,
                separatorIndex >= 0 ? separatorIndex : fullVersion.length()).trim();
        if (StringUtils.isBlank(version)) {
            throw exception(new ErrorCode(500, "工艺版本信息查询失败"));
        }
        return version;
    }

    @Override
    public ProcessFileUrlRespVO queryFileUrl(ProcessFileUrlReqVO reqVO) {
        if (reqVO == null || StringUtils.isBlank(reqVO.getOid())) {
            throw exception(new ErrorCode(500, "工序oid不能为空"));
        }
        JSONObject request = new JSONObject();
        request.put("oid", "OperationEntity:" + reqVO.getOid());
        return queryProcessFileUrl(request.toJSONString());
    }

    /**
     * 直连 MPM 平台接口查询工艺文件地址
     */
    private ProcessFileUrlRespVO queryProcessFileUrl(String reqParam) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(processFileUrl,
                    new HttpEntity<>(reqParam, buildMpmHeaders()), String.class);
        } catch (RestClientException requestException) {
            log.error("调用工艺文件地址接口失败, url: {}", processFileUrl, requestException);
            throw exception(new ErrorCode(500, "工艺文件地址获取失败"));
        }
        return parseFileUrlResponse(response.getBody());
    }

    private ProcessFileUrlRespVO parseFileUrlResponse(String body) {
        if (StringUtils.isBlank(body)) {
            throw exception(new ErrorCode(500, "工艺文件地址获取失败"));
        }
        JSONObject envelope;
        try {
            envelope = JSONObject.parseObject(body);
        } catch (RuntimeException parseException) {
            throw exception(new ErrorCode(500, "工艺文件地址获取失败"));
        }
        Object code = envelope == null ? null : envelope.get("code");
        if (envelope == null
                || !Boolean.TRUE.equals(envelope.get("success"))
                || !(code instanceof Number)
                || ((Number) code).intValue() != HttpStatus.SC_OK
                || !(envelope.get("result") instanceof JSONObject)) {
            throw exception(new ErrorCode(500, "工艺文件地址获取失败"));
        }
        Object urlValue = envelope.getJSONObject("result").get("url");
        if (!(urlValue instanceof String) || StringUtils.isBlank((String) urlValue)) {
            throw exception(new ErrorCode(500, "工艺文件地址获取失败"));
        }
        return ProcessFileUrlRespVO.builder().url((String) urlValue).build();
    }
}
