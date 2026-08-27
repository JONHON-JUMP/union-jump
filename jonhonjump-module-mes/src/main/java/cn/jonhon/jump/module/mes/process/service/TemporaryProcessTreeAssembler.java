package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.module.mes.process.constant.CommonConstant;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将临时工艺接口的扁平明细转换为前端可直接展示的工序树。
 */
@Component
public class TemporaryProcessTreeAssembler {


    public List<ProcessCardDetailsRespVO> assemble(JSONArray details, String documentOid) {
        List<Row> rows = new ArrayList<>();
        if (details != null) {
            for (int index = 0; index < details.size(); index++) {
                rows.add(toRow(details.getJSONObject(index), index, documentOid));
            }
        }
        rows.sort(this::compareRows);
        return buildTreeAndAssignIndexes(rows);
    }

    private Row toRow(JSONObject json, int sourceIndex, String documentOid) {
        String no = StringUtils.trimToEmpty(json.getString("Seqno"));
        ProcessCardDetailsRespVO node = ProcessCardDetailsRespVO.builder()
                .name(json.getString("Seqdesc"))
                .code(null)
                .no(no)
                .url(CommonConstant.VIEW_URL_PREFIX + documentOid)
                .children(new ArrayList<>())
                .build();
        return new Row(no, sourceIndex, parseParts(no), node);
    }

    private List<BigInteger> parseParts(String no) {
        if (StringUtils.isBlank(no)) {
            return null;
        }
        String[] values = no.split("\\.", -1);
        List<BigInteger> parts = new ArrayList<>(values.length);
        for (String value : values) {
            if (!value.matches("\\d+")) {
                return null;
            }
            parts.add(new BigInteger(value));
        }
        return parts;
    }

    private int compareRows(Row left, Row right) {
        if (left.parts == null || right.parts == null) {
            if (left.parts == null && right.parts == null) {
                return Integer.compare(left.sourceIndex, right.sourceIndex);
            }
            return left.parts == null ? 1 : -1;
        }
        int commonSize = Math.min(left.parts.size(), right.parts.size());
        for (int index = 0; index < commonSize; index++) {
            int compared = left.parts.get(index).compareTo(right.parts.get(index));
            if (compared != 0) {
                return compared;
            }
        }
        int sizeCompared = Integer.compare(left.parts.size(), right.parts.size());
        return sizeCompared != 0 ? sizeCompared : Integer.compare(left.sourceIndex, right.sourceIndex);
    }

    private List<ProcessCardDetailsRespVO> buildTreeAndAssignIndexes(List<Row> rows) {
        Map<String, Row> firstRowsByNo = new HashMap<>();
        for (Row row : rows) {
            if (row.parts != null) {
                firstRowsByNo.putIfAbsent(row.no, row);
            }
        }

        List<ProcessCardDetailsRespVO> roots = new ArrayList<>();
        for (Row row : rows) {
            boolean duplicated = row.parts != null && firstRowsByNo.get(row.no) != row;
            int separatorIndex = row.no.lastIndexOf('.');
            Row parent = separatorIndex > 0 ? firstRowsByNo.get(row.no.substring(0, separatorIndex)) : null;
            if (row.parts == null || duplicated || parent == null) {
                roots.add(row.node);
            } else {
                parent.node.getChildren().add(row.node);
            }
        }

        long[] nextIndex = { 1L };
        assignIndexes(roots, nextIndex);
        return roots;
    }

    private void assignIndexes(List<ProcessCardDetailsRespVO> nodes, long[] nextIndex) {
        for (ProcessCardDetailsRespVO node : nodes) {
            node.setIdx(nextIndex[0]++);
            assignIndexes(node.getChildren(), nextIndex);
        }
    }

    private static final class Row {
        private final String no;
        private final int sourceIndex;
        private final List<BigInteger> parts;
        private final ProcessCardDetailsRespVO node;

        private Row(String no, int sourceIndex, List<BigInteger> parts, ProcessCardDetailsRespVO node) {
            this.no = no;
            this.sourceIndex = sourceIndex;
            this.parts = parts;
            this.node = node;
        }
    }
}
