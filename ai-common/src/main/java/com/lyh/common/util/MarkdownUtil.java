package com.lyh.common.util;

/**
 * Markdown工具类
 *
 * @author lengYinHui
 * @date 2026/1/23
 */
public class MarkdownUtil {
    /**
     * 将二维数组转换为markdown表格
     *
     * @param data 二维数组，第一行为表头，后续行为数据
     * @return markdown表格字符串
     */
    public static String arrayToMarkdownTable(Object[][] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 添加表头
        for (int i = 0; i < data[0].length; i++) {
            sb.append("| " + data[0][i] + " ");
        }
        sb.append("|\n");

        // 添加分隔线
        for (int i = 0; i < data[0].length; i++) {
            sb.append("| :--- ");
        }
        sb.append("|\n");

        // 添加数据行
        for (int i = 1; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                // 将\n替换为<br>标签，以在Markdown表格中实现换行
                Object cellContent = data[i][j];
                if (cellContent instanceof String) {
                    cellContent = ((String) cellContent).replace("\n", "<br>")
                    .replace("|","&#124;");
                }
                sb.append("| " + cellContent + " ");
            }
            sb.append("|\n");
        }

        return sb.toString();
    }
}