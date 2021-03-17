package io.jenkins.plugins.SignPath.ApiIntegration;

import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.DefaultPowerShellExecutor;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellExecutionResult;
import io.jenkins.plugins.SignPath.ApiIntegration.PowerShell.PowerShellCommand;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static io.jenkins.plugins.SignPath.TestUtils.AssertionExtensions.assertContains;
import static org.junit.Assert.*;

public class DefaultPowerShellExecutorTest {
    private ByteArrayOutputStream outputStream;

    private DefaultPowerShellExecutor sut;

    @Before
    public void setup() {
        outputStream = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(outputStream);

        sut = new DefaultPowerShellExecutor("pwsh", logger);
    }

    @Test
    public void execute() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("echo 'some string'"), Integer.MAX_VALUE);

        // ASSERT
        assertFalse(executionResult.getHasError());

        assertNotNull(executionResult.getOutput());
        assertContains("some string", executionResult.getOutput());
        assertContains("some string", outputStream.toString());
    }

    @Test
    public void execute_withEnvironmentVariables() {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("myvariable", "content");

        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("echo $env:myvariable", environmentVariables), Integer.MAX_VALUE);

        // ASSERT
        assertFalse(executionResult.getHasError());

        assertNotNull(executionResult.getOutput());
        assertContains("content", executionResult.getOutput());
        assertContains("content", outputStream.toString());
    }

    @Test
    public void execute_withError() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("echo 'some string'; exit 1"), Integer.MAX_VALUE);

        // ASSERT
        assertTrue(executionResult.getHasError());
        assertEquals("Execution did not complete successfully (ExitCode: 1)", executionResult.getErrorDescription());
        assertNull(executionResult.getOutput());

        assertContains("some string", outputStream.toString());
    }

    @Test
    public void execute_withTimeout() {
        // ACT
        PowerShellExecutionResult executionResult = sut.execute(new PowerShellCommand("Start-Sleep -Seconds 5;"), 0);

        // ASSERT
        assertTrue(executionResult.getHasError());
        assertEquals("Execution did not complete within 0s", executionResult.getErrorDescription());
        assertNull(executionResult.getOutput());

        // cannot assert outputStream here since the timeout is immediately reached before any output is captured
    }
}
