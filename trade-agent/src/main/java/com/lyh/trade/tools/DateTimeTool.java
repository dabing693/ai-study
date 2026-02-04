package com.lyh.trade.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Component
public class DateTimeTool {
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss SSS";

    @Tool(description = "获取当前的日期及时间，格式：" + DATE_FORMAT_PATTERN)
    public String currentDateTime() {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return format.format(new Date());
    }
}
