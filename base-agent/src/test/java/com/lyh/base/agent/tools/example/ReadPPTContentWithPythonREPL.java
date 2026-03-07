package com.lyh.base.agent.tools.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * 使用 markitdown 工具读取 PowerPoint 文件内容的示例（使用 Java ProcessBuilder）
 */
public class ReadPPTContentWithPythonREPL {

    public static void main(String[] args) {
        System.out.println("使用 markitdown 工具读取 PPT 文件内容");
        System.out.println("===============================================");
        // 获取 PPT 文件的绝对路径
        String pptFilePath = Paths.get("").toAbsolutePath()
                .resolve("data/test/Hello-Agents-初识智能体.pptx").toString();
        try {
            // 使用 ProcessBuilder 调用 markitdown 命令
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "chcp 65001 >nul & python -m markitdown \"" + pptFilePath + "\"");
            pb.redirectErrorStream(true);

            // 设置环境变量确保 UTF-8 编码
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            System.out.println("正在执行命令: python -m markitdown " + pptFilePath);
            Process process = pb.start();

            // 读取命令输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();

            System.out.println("命令执行完成，退出码: " + exitCode);
            System.out.println("输出内容:\n" + output.toString());

        } catch (IOException | InterruptedException e) {
            System.err.println("处理 PPT 文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n处理完成！");
    }
}