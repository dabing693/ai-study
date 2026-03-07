package com.lyh.base.agent.tools;

import com.lyh.base.agent.annotation.Tool;
import com.lyh.base.agent.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class BashTool {

    @Tool(name = "bash", description = "Run a shell command on the local machine. Use this to execute bash or terminal commands, such as 'dir', 'type', etc. Input is the exact bash/cmd command string. Note that the OS is Windows. DO NOT use this to run 'python' or 'python -m' commands. For any Python execution, write equivalent Python code and use the 'python_repl' tool instead.")
    public String run(@ToolParam(description = "The terminal command to run") String command) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command).start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), Charset.forName("GBK")));
            StringBuilder errSb = new StringBuilder();
            while ((line = errReader.readLine()) != null) {
                errSb.append(line).append("\n");
            }

            if (!finished) {
                process.destroy();
                return "Execution timed out. Output so far: \n" + sb.toString() + "\nErrors:\n" + errSb.toString();
            }

            if (errSb.length() > 0 && sb.length() == 0) {
                return errSb.toString();
            }
            return sb.toString() + (errSb.length() > 0 ? "\nErrors:\n" + errSb.toString() : "");
        } catch (Exception e) {
            return "Execution failed: " + e.getMessage();
        }
    }
}
