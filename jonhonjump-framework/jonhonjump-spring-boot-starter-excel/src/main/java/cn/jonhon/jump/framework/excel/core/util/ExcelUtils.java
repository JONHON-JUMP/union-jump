package cn.jonhon.jump.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.handler.WriteHandler;
import cn.jonhon.jump.framework.common.util.http.HttpUtils;
import cn.jonhon.jump.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.jonhon.jump.framework.excel.core.handler.SelectSheetWriteHandler;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Excel 工具类
 *
 * @author 中航光电
 */
public class ExcelUtils {

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        doWrite(response, filename, sheetName, head, data, false);
    }

    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data, WriteHandler... extraHandlers) throws IOException {
        doWrite(response, filename, sheetName, head, data, true, extraHandlers);
    }

    private static <T> void doWrite(HttpServletResponse response, String filename, String sheetName,
                                    Class<T> head, List<T> data, boolean inMemory,
                                    WriteHandler... extraHandlers) throws IOException {
        // 输出 Excel
        ExcelWriterBuilder builder = FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .inMemory(inMemory) // 文本列格式必须写进 xlsx 本体，流式写出时 Excel 不认
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()); // 避免 Long 类型丢失精度
        if (extraHandlers != null) {
            for (WriteHandler handler : extraHandlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        }
        builder.sheet(sheetName).doWrite(data);
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType(inMemory
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/vnd.ms-excel;charset=UTF-8");
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        // 参考 https://t.zsxq.com/zM77F 帖子，增加 try 处理，兼容 windows 场景
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcelFactory.read(inputStream, head, null)
                    .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                    .doReadAllSync();
        }
    }

}
