package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ErrorCode;
import cn.jonhon.jump.module.mes.process.constant.CommonConstant;
import cn.jonhon.jump.module.mes.process.constant.InvokeIdConstant;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardRespVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.FormalProcessReqVO;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.TemporaryProcessReqVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import cn.jonhon.jump.module.mes.process.enums.YesOrNo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jonhon.route.ThirdPartyRouteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

@Slf4j
@Component
public class ProcessServiceImpl implements ProcessService{

    @Resource
    private ThirdPartyRouteService thirdPartyRouteService;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private CaoeTableMapper caoeTableMapper;

    /**
     * 查看工艺卡片
     * @param reqVO
     * @return
     */
    @Override
    public List<ProcessCardRespVO> queryCard(ProcessCardReqVO reqVO) {

        // 正式工艺
        if (reqVO.getAccno().startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX) || reqVO.getAccno().startsWith(CommonConstant.MPM_FORMAL_ACCNO_PREFIX)) {

            // 调用接口获取工艺信息


            // MPM正式工艺
            if (reqVO.getAccno().startsWith(CommonConstant.MPM_FORMAL_ACCNO_PREFIX)) {

            }

            // PDM正式工艺
            if (reqVO.getAccno().startsWith(CommonConstant.PDM_FORMAL_ACCNO_PREFIX)) {

            }

            FormalProcessReqVO queryRouteStructInfoReqVO = FormalProcessReqVO.builder().fnumber(reqVO.getAccno()).build();
            String reqParam = JSON.toJSONString(queryRouteStructInfoReqVO);

            JSONArray ppopLinkJsonArray = thirdPartyRouteService.invoke(
                    InvokeIdConstant.GetPrtRoutInfo,
                    CommonConstant.JUMP,
                    CommonConstant.WXD,
                    reqParam,
            null,
            response -> {
                    if (StringUtils.isEmpty(response.getBody())) {
                        throw exception(new ErrorCode(500, "工艺信息查询失败"));
                    }

                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (!jsonObject.containsKey(CommonConstant.DATA)) {
                        throw exception(new ErrorCode(500, "工艺信息查询失败"));
                    }
                    else {
                        if (!jsonObject.containsKey(CommonConstant.CAOE_PPOP_LINK)) {
                            throw exception(new ErrorCode(500, "工艺信息查询失败"));
                        }
                        else {
                            return jsonObject.getJSONArray(CommonConstant.CAOE_PPOP_LINK);
                        }
                    }
                }
            );

        }
        // 临时工艺
        else {

            if (StringUtils.isEmpty(reqVO.getPrtno())) {
                throw exception(new ErrorCode(500, "临时工艺必须输入物料号"));
            }

            TemporaryProcessReqVO temporaryProcessReqVO = TemporaryProcessReqVO.builder().prtno(reqVO.getPrtno()).build();

            if (reqVO.getAccno().length() > 4) {
                temporaryProcessReqVO.setFxtype(YesOrNo.YES.getType().toString());
            }
            else {
                temporaryProcessReqVO.setFxtype(YesOrNo.NO.getType().toString());
            }
            String reqParam = JSON.toJSONString(temporaryProcessReqVO);

            JSONObject responseBodyJsonObject = thirdPartyRouteService.invoke(
                InvokeIdConstant.GetPrtRoutInfo,
                CommonConstant.JUMP,
                CommonConstant.WXD,
                reqParam,
                null,
                response -> {
                    if (StringUtils.isEmpty(response.getBody())) {
                        throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
                    }

                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    if (!jsonObject.containsKey(CommonConstant.RETCODE)
                            || !jsonObject.getString(CommonConstant.RETCODE).equals(String.valueOf(HttpStatus.SC_OK))
                            || !jsonObject.containsKey(CommonConstant.RESPONSEBODY)
                            || StringUtils.isEmpty(jsonObject.getString(CommonConstant.RESPONSEBODY))
                    ) {
                        throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
                    }
                    return jsonObject.getJSONObject(CommonConstant.RESPONSEBODY);
                }
            );

            String oid = responseBodyJsonObject.getString(CommonConstant.OID);
            if (StringUtils.isEmpty(oid)) {
                throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
            }

            if (!responseBodyJsonObject.containsKey(CommonConstant.DETAILS)
                    || StringUtils.isEmpty(responseBodyJsonObject.get(CommonConstant.DETAILS).toString())
            ){
                throw exception(new ErrorCode(500, "临时工艺信息查询失败"));
            }

            JSONArray jsonArray = responseBodyJsonObject.getJSONArray(CommonConstant.DETAILS);

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

            CaoeDocInfoDTO caoeDocInfoDTO = caoeTableMapper.queryDocInfo(docNumber);
            if (!caoeDocInfoDTO.getDocSate().equals(CommonConstant.PUBLISHED)) {
                throw exception(new ErrorCode(500, "工艺未发行，无法查看"));
            }



        }

        return null;
    }
}
