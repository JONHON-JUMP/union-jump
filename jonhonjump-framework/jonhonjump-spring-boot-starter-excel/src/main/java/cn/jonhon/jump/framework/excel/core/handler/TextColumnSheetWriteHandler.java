package cn.jonhon.jump.framework.excel.core.handler;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.WorkbookWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 把导入列的格式设成文本，用户在表头下面新输入的格子也按文本保存。
 * 不预建空行。列样式在整本写完后再设一次，避免列宽调整把文本格式冲掉。
 */
public class TextColumnSheetWriteHandler implements SheetWriteHandler, CellWriteHandler, WorkbookWriteHandler {

    /** 内置文本格式 @。自定义格式码在 WPS 里经常不生效。 */
    private static final short TEXT_FORMAT = 49;

    /** 晚于 FastExcel 的样式填充，避免文本格式被盖掉。 */
    private static final int ORDER = 50001;

    private final int columnCount;
    private CellStyle textStyle;

    public TextColumnSheetWriteHandler(Class<?> head) {
        int count = 0;
        for (Field field : head.getDeclaredFields()) {
            if (field.getAnnotation(ExcelProperty.class) != null) {
                count++;
            }
        }
        this.columnCount = count;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        textStyle = createTextStyle(writeWorkbookHolder.getWorkbook());
        applyColumnTextFormat(writeSheetHolder.getSheet());
    }

    @Override
    public void afterWorkbookDispose(WriteWorkbookHolder writeWorkbookHolder) {
        if (textStyle == null) {
            textStyle = createTextStyle(writeWorkbookHolder.getWorkbook());
        }
        Workbook workbook = writeWorkbookHolder.getWorkbook();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            applyColumnTextFormat(workbook.getSheetAt(i));
        }
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                 Integer relativeRowIndex, Boolean isHead) {
        if (Boolean.TRUE.equals(isHead) || cell == null || cell.getColumnIndex() >= columnCount) {
            return;
        }
        if (textStyle == null) {
            textStyle = createTextStyle(cell.getSheet().getWorkbook());
        }
        cell.setCellStyle(textStyle);
    }

    private void applyColumnTextFormat(Sheet sheet) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setDefaultColumnStyle(i, textStyle);
        }
    }

    private static CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(TEXT_FORMAT);
        if (style instanceof XSSFCellStyle) {
            CTXf xf = ((XSSFCellStyle) style).getCoreXf();
            xf.setNumFmtId(TEXT_FORMAT);
            xf.setApplyNumberFormat(true);
            // 等价于格子前的单引号，输入 0123 时留下前面的 0
            xf.setQuotePrefix(true);
        }
        return style;
    }

}
