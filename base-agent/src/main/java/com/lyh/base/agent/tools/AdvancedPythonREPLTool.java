package com.lyh.base.agent.tools;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Advanced Java implementation of LangChain's PythonREPLTool
 * Allows executing Python code from Java in a secure manner with timeout and variable persistence
 */
public class AdvancedPythonREPLTool {
    
    private static final Pattern DANGEROUS_IMPORTS = Pattern.compile(
        "(?i)(import|from)\\s+(sys|subprocess|shutil|glob|pickle|dill|marshal|socket|urllib|urllib2|urllib3|requests|ftplib|telnetlib|smtplib|imaplib|poplib|nntplib|cgi|cgitb|wsgiref|xml|xmlrpclib|xmlrpc|ssl|asyncio|aiohttp|websockets|socketserver|paramiko|fabric|ssh|ftp|ftputil|smbprotocol|pysmb|pysftp|invoke|multiprocessing)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DANGEROUS_CALLS = Pattern.compile(
        "(?i)\\b(exec|eval|compile|open|file|input|raw_input|execfile|__import__|execfile|breakpoint|quit|exit)\\s*\\(", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
        "(?i)\\b(exec|eval|breakpoint|quit|exit)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private final Map<String, Object> globals;
    private final Map<String, Object> locals;
    private String lastResult;
    private final String pythonExecutable;
    private final long timeoutSeconds;
    private final File workingDir;
    
    public AdvancedPythonREPLTool() {
        this.globals = new HashMap<>();
        this.locals = new HashMap<>();
        this.lastResult = "";
        this.pythonExecutable = findPythonExecutable();
        this.timeoutSeconds = 30; // 30 seconds timeout
        this.workingDir = createSandboxDirectory();
    }
    
    public AdvancedPythonREPLTool(long timeoutSeconds) {
        this.globals = new HashMap<>();
        this.locals = new HashMap<>();
        this.lastResult = "";
        this.pythonExecutable = findPythonExecutable();
        this.timeoutSeconds = timeoutSeconds;
        this.workingDir = createSandboxDirectory();
    }
    
    /**
     * Find the Python executable in the system
     */
    private String findPythonExecutable() {
        String[] possiblePaths = {"python", "python3", "python.exe", "python3.exe"};
        
        for (String path : possiblePaths) {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            pb.directory(new File(System.getProperty("user.dir")));
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    return path;
                }
            } catch (IOException | InterruptedException e) {
                continue;
            }
        }
        
        throw new RuntimeException("Python executable not found in system PATH");
    }
    
    /**
     * Create a sandbox directory for Python execution
     */
    private File createSandboxDirectory() {
        try {
            File sandboxDir = new File(System.getProperty("java.io.tmpdir"), "python_repl_sandbox_" + UUID.randomUUID());
            if (sandboxDir.mkdirs()) {
                return sandboxDir;
            } else {
                throw new IOException("Could not create sandbox directory");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating sandbox directory: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute Python code and return the result
     */
    public String run(String code) {
        // Sanitize input first
        if (!isSafePythonCode(code)) {
            throw new SecurityException("Potentially unsafe Python code detected");
        }
        
        try {
            // Create a temporary file to execute the code
            File tempFile = createTempPythonFile(code);
            
            // Execute the Python file with timeout
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, tempFile.getAbsolutePath());
            pb.directory(workingDir); // Execute in sandbox directory
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Wait for the process to complete with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                // Process timed out, destroy it
                process.destroyForcibly();
                throw new RuntimeException("Python code execution timed out after " + timeoutSeconds + " seconds");
            }
            
            // Read the output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Clean up the temporary file
            tempFile.delete();
            
            // Store the result
            this.lastResult = output.toString().trim();
            
            // Return the output
            return this.lastResult;
            
        } catch (IOException e) {
            throw new RuntimeException("Error executing Python code: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Python code execution interrupted: " + e.getMessage(), e);
        }
    }
    
    /**
     * Execute Python code with variables and return the result
     */
    public String run(String code, Map<String, Object> variables) {
        StringBuilder codeBuilder = new StringBuilder();
        
        // Add variable definitions at the beginning
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Convert Java value to Python representation
            String pythonValue = convertToPythonValue(value);
            codeBuilder.append(key).append(" = ").append(pythonValue).append("\n");
        }
        
        // Add the original code
        codeBuilder.append(code);
        
        return run(codeBuilder.toString());
    }
    
    /**
     * Convert Java value to Python representation
     */
    private String convertToPythonValue(Object value) {
        if (value == null) {
            return "None";
        } else if (value instanceof String) {
            return "\"" + ((String) value).replace("\"", "\\\"") + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString().substring(0, 1).toUpperCase() + value.toString().substring(1);
        } else if (value instanceof Object[]) {
            StringBuilder sb = new StringBuilder("[");
            Object[] arr = (Object[]) value;
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(convertToPythonValue(arr[i]));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return "\"" + value.toString().replace("\"", "\\\"") + "\""; // Default to string
        }
    }
    
    /**
     * Create a temporary Python file with the given code
     */
    private File createTempPythonFile(String code) throws IOException {
        File tempFile = File.createTempFile("python_repl_", ".py", workingDir);
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println("# Automatically generated Python REPL code");
            writer.println();
            writer.println("import sys");
            writer.println("import io");
            writer.println();
            // Redirect stdout to capture output
            writer.println("old_stdout = sys.stdout");
            writer.println("sys.stdout = buffer = io.StringIO()");
            writer.println();
            // Execute the provided code
            writer.println(code);
            writer.println();
            // Print the captured output
            writer.println("sys.stdout = old_stdout");
            writer.println("captured_output = buffer.getvalue()");
            writer.println("if captured_output:");
            writer.println("    print(captured_output, end='')");
            writer.println("else:");
            writer.println("    # If nothing was printed, try to get the last expression value");
            writer.println("    import builtins");
            writer.println("    if '__builtins__' in dir() and hasattr(builtins, '_'):");
            writer.println("        print(repr(_))");
        }
        return tempFile;
    }
    
    /**
     * Check if the Python code is safe to execute
     */
    private boolean isSafePythonCode(String code) {
        // Check for dangerous imports
        if (DANGEROUS_IMPORTS.matcher(code).find()) {
            return false;
        }
        
        // Check for dangerous function calls
        if (DANGEROUS_CALLS.matcher(code).find()) {
            return false;
        }
        
        // Check for dangerous keywords
        if (DANGEROUS_KEYWORDS.matcher(code).find()) {
            return false;
        }
        
        // Additional security checks can be added here
        // For example, check for system commands, file operations, etc.
        
        return true;
    }
    
    /**
     * Get the last result from the REPL
     */
    public String getLastResult() {
        return this.lastResult;
    }
    
    /**
     * Clear the globals and locals
     */
    public void clear() {
        this.globals.clear();
        this.locals.clear();
        this.lastResult = "";
    }
    
    /**
     * Get the working directory used for execution
     */
    public File getWorkingDir() {
        return workingDir;
    }
    
    /**
     * Clean up the sandbox directory
     */
    public void cleanup() {
        if (workingDir.exists()) {
            deleteDirectory(workingDir);
        }
    }
    
    /**
     * Delete directory recursively
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
