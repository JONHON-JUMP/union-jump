package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ErrorCode;
import cn.jonhon.jump.module.mes.process.constant.CommonConstant;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.ProcessOperationDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.jonhon.jump.framework.common.exception.util.ServiceExceptionUtil.exception;

@Component
public class FormalProcessTreeAssembler {

    @Resource
    private CaoeTableMapper mapper;

    public List<ProcessCardDetailsRespVO> assemble(String accno, String version, boolean mpm) {
        List<ProcessCardDetailsRespVO> roots = buildChildren(
                accno, version, mpm, new HashSet<>());
        if (roots.isEmpty()) {
            throw exception(new ErrorCode(500, "未查询到正式工艺工序"));
        }
        assignIndexes(roots, new long[]{1L});
        return roots;
    }

    private List<ProcessCardDetailsRespVO> buildChildren(
            String number, String version, boolean mpm, Set<String> path) {
        List<ProcessOperationDTO> queried = mapper.queryChildOperations(number, version);
        List<ProcessOperationDTO> operations = queried == null ? Collections.emptyList() : queried;
        List<ProcessCardDetailsRespVO> nodes = new ArrayList<>(operations.size());
        for (ProcessOperationDTO operation : operations) {
            validateOperation(operation);
            String key = operation.getCnumber() + "@" + operation.getCversion();
            if (!path.add(key)) {
                throw exception(new ErrorCode(500, "正式工艺层级存在循环"));
            }
            ProcessCardDetailsRespVO node = createNode(operation, mpm);
            node.setChildren(buildChildren(
                    operation.getCnumber(), operation.getCversion(), mpm, path));
            path.remove(key);
            nodes.add(node);
        }
        return nodes;
    }

    private void validateOperation(ProcessOperationDTO operation) {
        if (operation == null || StringUtils.isAnyBlank(
                operation.getCname(), operation.getCnumber(),
                operation.getLabel(), operation.getCversion())) {
            throw exception(new ErrorCode(500, "正式工艺工序信息不完整"));
        }
    }

    private ProcessCardDetailsRespVO createNode(ProcessOperationDTO operation, boolean mpm) {
        String url = null;
        String oid = null;
        if (mpm) {
            oid = mapper.queryMpmOperationOid(operation.getCnumber(), operation.getCversion());
            if (StringUtils.isBlank(oid)) {
                throw exception(new ErrorCode(500, "正式工艺文件信息不完整"));
            }
        } else {
            String operationLink = mapper.queryPdmOperationLink(
                    operation.getCnumber(), operation.getCversion());
            int queryIndex = StringUtils.defaultString(operationLink).indexOf('?');
            String query = queryIndex >= 0 ? operationLink.substring(queryIndex + 1) : null;
            if (StringUtils.isBlank(query)) {
                throw exception(new ErrorCode(500, "正式工艺文件信息不完整"));
            }
            url = CommonConstant.PDM_VIEW_URL_PREFIX + query;
        }
        return ProcessCardDetailsRespVO.builder()
                .name(operation.getCname())
                .code(operation.getCnumber())
                .no(operation.getLabel())
                .url(url)
                .oid(oid)
                .children(new ArrayList<>())
                .build();
    }

    private void assignIndexes(List<ProcessCardDetailsRespVO> nodes, long[] nextIndex) {
        for (ProcessCardDetailsRespVO node : nodes) {
            node.setIdx(nextIndex[0]++);
            assignIndexes(node.getChildren(), nextIndex);
        }
    }
}
