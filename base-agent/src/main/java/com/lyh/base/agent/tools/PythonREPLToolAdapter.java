package com.lyh.base.agent.tools;

import com.lyh.base.agent.annotation.Tool;

/**
 * Python REPL Tool that implements the Tool interface for use in AI agent frameworks
 * Similar to LangChain's PythonREPLTool but in Java
 */
public class PythonREPLToolAdapter {

    private final AdvancedPythonREPLTool replTool;

    public PythonREPLToolAdapter() {
        this.replTool = new AdvancedPythonREPLTool();
    }

    public PythonREPLToolAdapter(long timeoutSeconds) {
        this.replTool = new AdvancedPythonREPLTool(timeoutSeconds);
    }

    @Tool(name = "python_repl",
            description = "A Python shell. Use this to execute python commands. Input should be a valid python code snippet. If you expect output it should be printed out. DO NOT try to run bash commands like 'python -m markitdown', Instead, use this tool.")
    public String run(String input) {
        try {
            return replTool.run(input);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute Python code with timeout
     */
    public String runWithTimeout(String input, long timeoutSeconds) {
        AdvancedPythonREPLTool tempTool = new AdvancedPythonREPLTool(timeoutSeconds);
        try {
            return tempTool.run(input);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            tempTool.cleanup();
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        replTool.cleanup();
    }
}