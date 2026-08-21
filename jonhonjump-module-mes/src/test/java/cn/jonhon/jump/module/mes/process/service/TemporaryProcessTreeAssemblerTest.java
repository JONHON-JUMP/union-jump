package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemporaryProcessTreeAssemblerTest {

    private final TemporaryProcessTreeAssembler assembler = new TemporaryProcessTreeAssembler();

    @Test
    void assemble_sortsNumericallyAndBuildsTree() {
        JSONArray details = JSON.parseArray("["
                + "{\"Seqno\":\"10.2\",\"Seqdesc\":\"子工序2\"},"
                + "{\"Seqno\":\"2\",\"Seqdesc\":\"工序2\"},"
                + "{\"Seqno\":\"10.1.2\",\"Seqdesc\":\"孙工序\"},"
                + "{\"Seqno\":\"10\",\"Seqdesc\":\"工序10\"},"
                + "{\"Seqno\":\"10.1\",\"Seqdesc\":\"子工序1\"}]");

        List<ProcessCardDetailsRespVO> result = assembler.assemble(details, "OR:wt.doc.WTDocument:1");

        assertEquals(Arrays.asList("2", "10"), result.stream()
                .map(ProcessCardDetailsRespVO::getNo).collect(Collectors.toList()));
        assertEquals(Arrays.asList("10.1", "10.2"), result.get(1).getChildren().stream()
                .map(ProcessCardDetailsRespVO::getNo).collect(Collectors.toList()));
        assertEquals("10.1.2", result.get(1).getChildren().get(0).getChildren().get(0).getNo());
    }

    @Test
    void assemble_keepsOrphansInvalidAndDuplicateRows() {
        JSONArray details = JSON.parseArray("["
                + "{\"Seqno\":\"20.1\",\"Seqdesc\":\"缺父\"},"
                + "{\"Seqno\":\"bad\",\"Seqdesc\":\"非法\"},"
                + "{\"Seqno\":\"10\",\"Seqdesc\":\"第一条\"},"
                + "{\"Seqno\":\"10\",\"Seqdesc\":\"重复条\"}]");

        List<ProcessCardDetailsRespVO> result = assembler.assemble(details, "OID-1");

        assertEquals(4, result.size());
        assertEquals("第一条", result.get(0).getName());
        assertNull(result.get(0).getCode());
        assertEquals("http://MESloginUser:MESloginUseradmin@pdm.caoe.com/"
                + "Windchill/netmarkets/jsp/ext/caoe/mes/export.jsp?oid=OID-1", result.get(0).getUrl());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L), flatten(result).stream()
                .map(ProcessCardDetailsRespVO::getIdx).collect(Collectors.toList()));
    }

    private List<ProcessCardDetailsRespVO> flatten(List<ProcessCardDetailsRespVO> nodes) {
        List<ProcessCardDetailsRespVO> result = new ArrayList<>();
        for (ProcessCardDetailsRespVO node : nodes) {
            result.add(node);
            if (node.getChildren() != null) {
                result.addAll(flatten(node.getChildren()));
            }
        }
        return result;
    }
}
