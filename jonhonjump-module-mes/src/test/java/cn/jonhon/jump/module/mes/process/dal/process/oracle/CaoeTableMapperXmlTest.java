package cn.jonhon.jump.module.mes.process.dal.process.oracle;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CaoeTableMapperXmlTest {

    @Test
    void formalProcessSqlUsesExactTablesParametersAndOrdering() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/mapper/process/CaoePpMapper.xml")), StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ");

        assertTrue(xml.contains("id=\"queryProcessState\""));
        assertTrue(xml.matches("(?is).*FROM CAOE_PP.*PP_NUMBER = #\\{accno}.*PP_VERSION = #\\{version}.*"));
        assertTrue(xml.matches("(?is).*id=\"queryChildOperations\".*FROM CAOE_PPOPLINK.*"
                + "FNUMBER = #\\{number}.*FVERSION = #\\{version}.*ORDER BY label ASC.*"));
        assertTrue(xml.matches("(?is).*id=\"queryPdmOperationLink\".*SELECT op_link.*FROM CAOE_OP.*"));
        assertTrue(xml.matches("(?is).*id=\"queryMpmOperationOid\".*SELECT oid.*FROM CAOE_OP.*"));
    }
}
