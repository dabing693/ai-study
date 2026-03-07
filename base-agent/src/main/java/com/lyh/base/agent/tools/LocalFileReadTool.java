package com.lyh.base.agent.tools;

import com.lyh.base.agent.annotation.Tool;
import com.lyh.base.agent.annotation.ToolParam;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class LocalFileReadTool {

    @Tool(name = "read_file", description = "Read the contents of a local text file. ONLY USE THIS FOR PLAIN TEXT FILES (e.g. .txt, .md, .json, .py, .java). DO NOT use this tool to read binary files like .pptx, .pdf, .docx, images, etc. For binary or presentation files, use the specific bash commands mentioned in documentation (like 'python -m markitdown').")
    public String read(@ToolParam(description = "Absolute or relative path to the file") String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Failed to read file: " + e.getMessage();
        }
    }
}
