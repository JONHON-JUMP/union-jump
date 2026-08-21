package cn.jonhon.jump.module.mes.process.dal.process.oracle;

import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.CaoeDocInfoDTO;
import cn.jonhon.jump.module.mes.process.dal.process.oracle.dto.ProcessOperationDTO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CAOE_PP Mapper。
 *
 * 表结构明确后再补充字段映射和查询方法。
 */
@Mapper
@DS("oracle")
public interface CaoeTableMapper {


    /**
     * 查询版本发行状态与工艺链接
     * @param docNumber
     * @return
     */
    CaoeDocInfoDTO queryDocInfo(@Param("docNumber") String docNumber);

    String queryProcessState(@Param("accno") String accno, @Param("version") String version);

    List<ProcessOperationDTO> queryChildOperations(@Param("number") String number,
                                                   @Param("version") String version);

    String queryPdmOperationLink(@Param("number") String number, @Param("version") String version);

    String queryMpmOperationOid(@Param("number") String number, @Param("version") String version);
}
