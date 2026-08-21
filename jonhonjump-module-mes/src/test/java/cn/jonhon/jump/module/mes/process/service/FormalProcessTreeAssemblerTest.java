package cn.jonhon.jump.module.mes.process.service;

import cn.jonhon.jump.framework.common.exception.ServiceException;
import cn.jonhon.jump.module.mes.process.controller.admin.vo.ProcessCardDetailsRespVO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.CaoeTableMapper;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.ProcessOperationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormalProcessTreeAssemblerTest {

    @InjectMocks
    private FormalProcessTreeAssembler assembler;

    @Mock
    private CaoeTableMapper mapper;

    @Test
    void assemble_buildsPdmTreeAndAssignsDepthFirstIndexes() {
        ProcessOperationDTO first = operation("一级工序1", "D1", "0010", "A");
        ProcessOperationDTO second = operation("一级工序2", "D4", "0040", "B");
        ProcessOperationDTO child = operation("二级工序", "D2", "0020", "A");
        when(mapper.queryChildOperations("C100", "A")).thenReturn(Arrays.asList(first, second));
        when(mapper.queryChildOperations("D1", "A")).thenReturn(Collections.singletonList(child));
        when(mapper.queryChildOperations("D2", "A")).thenReturn(Collections.emptyList());
        when(mapper.queryChildOperations("D4", "B")).thenReturn(Collections.emptyList());
        when(mapper.queryPdmOperationLink("D1", "A")).thenReturn("http://legacy/a?oid=one");
        when(mapper.queryPdmOperationLink("D2", "A")).thenReturn("http://legacy/a?oid=child");
        when(mapper.queryPdmOperationLink("D4", "B")).thenReturn("http://legacy/a?oid=two");

        List<ProcessCardDetailsRespVO> tree = assembler.assemble("C100", "A", false);

        assertEquals(Arrays.asList("0010", "0040"), tree.stream()
                .map(ProcessCardDetailsRespVO::getNo).collect(Collectors.toList()));
        assertEquals("0020", tree.get(0).getChildren().get(0).getNo());
        assertEquals("D2", tree.get(0).getChildren().get(0).getCode());
        assertEquals("http://pdm.caoe.com/Windchill/netmarkets/jsp/ext/caoe/mpml/"
                        + "routCard.jsp?oid=child",
                tree.get(0).getChildren().get(0).getUrl());
        assertEquals(Arrays.asList(1L, 2L, 3L), Arrays.asList(
                tree.get(0).getIdx(), tree.get(0).getChildren().get(0).getIdx(), tree.get(1).getIdx()));
    }

    @Test
    void assemble_keepsMpmOidForClickTimeRelease() {
        ProcessOperationDTO operation = operation("MPM工序", "M1", "0010", "B");
        when(mapper.queryChildOperations("CX100", "B")).thenReturn(Collections.singletonList(operation));
        when(mapper.queryChildOperations("M1", "B")).thenReturn(Collections.emptyList());
        when(mapper.queryMpmOperationOid("M1", "B")).thenReturn("308233850219293315");

        ProcessCardDetailsRespVO node = assembler.assemble("CX100", "B", true).get(0);

        assertEquals("308233850219293315", node.getOid());
        assertNull(node.getUrl());
    }

    @Test
    void assemble_rejectsIncompleteOperation() {
        when(mapper.queryChildOperations("C100", "A")).thenReturn(Collections.singletonList(
                operation("缺编码", null, "0010", "A")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> assembler.assemble("C100", "A", false));

        assertEquals("正式工艺工序信息不完整", exception.getMessage());
    }

    @Test
    void assemble_rejectsMissingFileInfo() {
        ProcessOperationDTO operation = operation("工序", "D1", "0010", "A");
        when(mapper.queryChildOperations("C100", "A")).thenReturn(Collections.singletonList(operation));
        when(mapper.queryPdmOperationLink("D1", "A")).thenReturn("http://legacy/no-query");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> assembler.assemble("C100", "A", false));

        assertEquals("正式工艺文件信息不完整", exception.getMessage());
    }

    @Test
    void assemble_rejectsCycleOnCurrentPath() {
        ProcessOperationDTO operation = operation("循环工序", "D1", "0010", "A");
        when(mapper.queryChildOperations("C100", "A")).thenReturn(Collections.singletonList(operation));
        when(mapper.queryChildOperations("D1", "A")).thenReturn(Collections.singletonList(operation));
        when(mapper.queryPdmOperationLink("D1", "A")).thenReturn("http://legacy/a?oid=one");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> assembler.assemble("C100", "A", false));

        assertEquals("正式工艺层级存在循环", exception.getMessage());
    }

    @Test
    void assemble_rejectsMissingRootOperations() {
        when(mapper.queryChildOperations("C100", "A")).thenReturn(Collections.emptyList());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> assembler.assemble("C100", "A", false));

        assertEquals("未查询到正式工艺工序", exception.getMessage());
    }

    private ProcessOperationDTO operation(String name, String number, String label, String version) {
        ProcessOperationDTO operation = new ProcessOperationDTO();
        operation.setCname(name);
        operation.setCnumber(number);
        operation.setLabel(label);
        operation.setCversion(version);
        return operation;
    }
}
