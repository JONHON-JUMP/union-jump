package cn.jonhon.jump.framework.excel.core.convert;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.DataFormatData;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.write.metadata.style.WriteCellStyle;

import java.math.BigDecimal;

/**
 * 按文本读写。Excel 把 0123 存成数字后前导 0 已经丢失，这里不再把文本解析成数字。
 */
public class ExcelTextStringConverter implements Converter<String> {

    @Override
    public Class<?> supportJavaTypeKey() {
        return String.class;
    }

    @Override
    public WriteCellData<String> convertToExcelData(String value, ExcelContentProperty contentProperty,
                                                    GlobalConfiguration globalConfiguration) {
        WriteCellData<String> cellData = new WriteCellData<>(value == null ? "" : value);
        WriteCellStyle style = new WriteCellStyle();
        DataFormatData format = new DataFormatData();
        format.setIndex((short) 49);
        style.setDataFormatData(format);
        style.setQuotePrefix(Boolean.TRUE);
        cellData.setWriteCellStyle(style);
        return cellData;
    }

    @Override
    public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                    GlobalConfiguration globalConfiguration) {
        if (cellData == null || cellData.getType() == CellDataTypeEnum.EMPTY) {
            return null;
        }
        // 单元格本身是文本时，原样保留前导 0
        String text = StrUtil.trim(cellData.getStringValue());
        if (StrUtil.isNotEmpty(text)) {
            return text;
        }
        BigDecimal number = cellData.getNumberValue();
        if (number != null) {
            return number.stripTrailingZeros().toPlainString();
        }
        return null;
    }

}
