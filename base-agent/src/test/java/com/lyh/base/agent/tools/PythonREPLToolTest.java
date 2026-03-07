package com.lyh.base.agent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class PythonREPLToolTest {

    private AdvancedPythonREPLTool replTool;

    @BeforeEach
    public void setUp() {
        replTool = new AdvancedPythonREPLTool(10); // 10 second timeout for tests
    }

    @AfterEach
    public void tearDown() {
        replTool.cleanup();
    }

    @Test
    public void testVariableAssignment() {
        String result = replTool.run("x = 10\nprint(x * 2)");
        assertTrue("20".equals(result) || "20\n".equals(result),
                "Expected '20' or '20\\n', but got: '" + result + "'");
    }

    @Test
    public void testMathOperations() {
        String result = replTool.run("import math\nprint(math.sqrt(16))");
        assertTrue("4.0".equals(result) || "4.0\n".equals(result),
                "Expected '4.0' or '4.0\\n', but got: '" + result + "'");
    }

    @Test
    public void testWithVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("x", 5);
        variables.put("y", 10);

        String result = replTool.run("print(x + y)", variables);
        assertTrue("15".equals(result) || "15\n".equals(result),
                "Expected '15' or '15\\n', but got: '" + result + "'");
    }

    @Test
    public void testListWithVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("numbers", new Integer[]{1, 2, 3, 4, 5});

        String result = replTool.run("print(sum(numbers))", variables);
        assertTrue("15".equals(result) || "15\n".equals(result),
                "Expected '15' or '15\\n', but got: '" + result + "'");
    }

    @Test
    public void testUnsafeCodeDetection() {
        // Test dangerous import
        assertThrows(SecurityException.class, () -> {
            replTool.run("import os\nprint('test')");
        });

        // Test dangerous function call
        assertThrows(SecurityException.class, () -> {
            replTool.run("exec('print(\"test\")')");
        });

        // Test eval function
        assertThrows(SecurityException.class, () -> {
            replTool.run("eval('2 + 2')");
        });
    }

    @Test
    public void testToolInterface() {
        PythonREPLToolAdapter tool = new PythonREPLToolAdapter();

        String result = tool.run("print('Hello from Python')");
        assertTrue("Hello from Python".equals(result) || "Hello from Python\n".equals(result),
                "Expected 'Hello from Python' or 'Hello from Python\\n', but got: '" + result + "'");

        tool.cleanup();
    }

    @Test
    public void testTimeout() {
        // Create a new instance with a short timeout
        AdvancedPythonREPLTool shortTimeoutTool = new AdvancedPythonREPLTool(1); // 1 second timeout

        try {
            // Run an infinite loop to test timeout
            assertThrows(RuntimeException.class, () -> {
                shortTimeoutTool.run("while True:\n    pass");
            });
        } finally {
            shortTimeoutTool.cleanup();
        }
    }

    @Test
    public void testLastResult() {
        replTool.run("x = 42");
        String result = replTool.run("print(x)");
        assertEquals("42", result);
        assertEquals("42", replTool.getLastResult());
    }

    @Test
    public void testClear() {
        replTool.run("x = 100");
        assertEquals("100", replTool.getLastResult());

        replTool.clear();
        assertEquals("", replTool.getLastResult());
    }
}